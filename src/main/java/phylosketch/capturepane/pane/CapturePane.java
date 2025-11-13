/*
 * CapturePane.java Copyright (C) 2025 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package phylosketch.capturepane.pane;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.ClipboardUtils;
import phylosketch.capturepane.capture.CaptureService;
import phylosketch.capturepane.capture.PhyloImageAnalyzer;
import phylosketch.utils.ScrollPaneUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;
import phylosketch.window.MainWindowController;
import phylosketch.window.WindowNotifications;

/**
 * maintain an image in the background for image capturepane
 * Daniel Huson, 1.2025
 */
public class CapturePane extends HBox {
	private final DrawView view;
	private final AnchorPane windowPane;

	private final Pane mainPane = new Pane();
	private final ImageView imageView = new ImageView();
	private final Group wordsGroup = new Group();
	private final Group pathsGroup = new Group();
	private final Group rootGroup = new Group();

	private final BooleanProperty hasImage = new SimpleBooleanProperty(this, "hasImage", false);
	private final BooleanProperty showCapture = new SimpleBooleanProperty(this, "showCapture", false);

	private final BooleanProperty hasRootLocation = new SimpleBooleanProperty(this, "hasRootLocationProperty", false);

	private final BooleanProperty canSelect = new SimpleBooleanProperty(this, "canSelect", true);
	private final BooleanProperty canEdit = new SimpleBooleanProperty(this, "canEdit", true);
	private final BooleanProperty canResize = new SimpleBooleanProperty(this, "canResize", true);
	private final BooleanProperty canCopy = new SimpleBooleanProperty(this, "canCopy", false);

	private final RootTool rootTool;

	private final CaptureService captureService;

	private final PropertySettingPane propertySettingPane = new PropertySettingPane();

	private final ObjectProperty<Rectangle> selectionRectangle = new SimpleObjectProperty<>(this, "selectionRectangle");

	public CapturePane(DrawView view, MainWindowController controller) {
		this.view = view;
		windowPane = controller.getRootPane();

		imageView.setOpacity(0.5);

		imageView.setStyle("-fx-background-color: white;");
		// imageView.setEffect(SelectionEffectRed.getInstance());
		imageView.imageProperty().addListener((v, o, n) -> {
			if (n != null) {
				imageView.setFitWidth(n.getWidth());
				imageView.setFitHeight(n.getHeight());
			}
		});

		hasImage.bind(imageView.imageProperty().isNotNull());

		mainPane.getChildren().addAll(imageView);
		showCapture.addListener((v, o, n) -> {
			if (n) {
				mainPane.getChildren().addAll(pathsGroup, wordsGroup);
				view.getOtherGroup().getChildren().add(rootGroup);
			} else {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(view, () -> {
					mainPane.getChildren().removeAll(pathsGroup, wordsGroup);
					view.getOtherGroup().getChildren().remove(rootGroup);
				});
			}
		});

		getStyleClass().add("viewer-background");
		setStyle("-fx-border-color: gray;");


		var closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(a -> {
			imageView.setImage(null);
			view.setMode(DrawView.Mode.Sketch);
		});

		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		SetupMouseInteraction.apply(view, this, resizeHandle, selectionRectangle);

		var rightBorderPane = new BorderPane();
		rightBorderPane.setTop(closeButton);
		rightBorderPane.setBottom(resizeHandle);
		getChildren().addAll(mainPane, rightBorderPane);
		canCopy.bind(showCaptureProperty());

		hasRootLocation.bind(rootGroup.visibleProperty());
		rootTool = new RootTool(view);
		getRootGroup().getChildren().add(rootTool);
		getRootGroup().setVisible(false);

		captureService = SetupCaptureService.apply(controller, view, this);

		if (false) {
			InvalidationListener updateScaling = e -> {
				if (imageView.getImage() != null) {
					var factorX = imageView.getBoundsInLocal().getWidth() / imageView.getImage().getWidth();
					var factorY = imageView.getBoundsInLocal().getHeight() / imageView.getImage().getHeight();
					wordsGroup.setScaleX(factorX);
					wordsGroup.setScaleY(factorY);
					pathsGroup.setScaleX(factorX);
					pathsGroup.setScaleY(factorY);
				}
			};
			imageView.imageProperty().addListener(updateScaling);
			imageView.fitWidthProperty().addListener(updateScaling);
			imageView.fitHeightProperty().addListener(updateScaling);
		}
	}

	public ImageView getImageView() {
		return imageView;
	}

	public ReadOnlyBooleanProperty canCopyProperty() {
		return canCopy;
	}

	public void copy() {
		if (imageView.getImage() != null) {
			ClipboardUtils.putImage(imageView.getImage());
		}
	}

	public DrawView getView() {
		return view;
	}

	public Group getRootGroup() {
		return rootGroup;
	}

	public Group getWordsGroup() {
		return wordsGroup;
	}

	public Group getPathsGroup() {
		return pathsGroup;
	}

	public BooleanProperty hasImageProperty() {
		return hasImage;
	}

	public ReadOnlyBooleanProperty runningProperty() {
		return captureService.runningProperty();
	}

	public boolean isShowCapture() {
		return showCapture.get();
	}

	public BooleanProperty showCaptureProperty() {
		return showCapture;
	}

	public void run(int goal) {
		if (!hasRootLocation.get())
			rootGroup.setVisible(true);
		else {
			if (captureService.getInputImage() != getImageView().getImage())
				captureService.setInputImage(getImageView().getImage());
			captureService.run(goal);
		}
	}

	public ReadOnlyIntegerProperty getPhaseProperty() {
		return captureService.phaseProperty();
	}


	public ReadOnlyBooleanProperty hasRootLocationProperty() {
		return hasRootLocation;
	}

	public Point2D getRootLocationOnScreen() {
		return rootTool.getRootLocationOnScreen();
	}

	public RootPosition.Side getRootSide() {
		return rootTool.getRootSide();
	}

	public ObjectProperty<RootPosition.Side> rootSideProperty() {
		return rootTool.rootSideProperty();
	}

	public void setRootSide(RootPosition.Side rootSide) {
		rootTool.setRootSide(rootSide);
	}

	public void reset() {
		wordsGroup.getChildren().clear();
		pathsGroup.getChildren().clear();
		captureService.clearData();
	}

	public Pane getMainPane() {
		return mainPane;
	}

	public boolean isCanSelect() {
		return canSelect.get();
	}

	public BooleanProperty canSelectProperty() {
		return canSelect;
	}

	public boolean isCanEdit() {
		return canEdit.get();
	}

	public BooleanProperty canEditProperty() {
		return canEdit;
	}

	public boolean isCanResize() {
		return canResize.get();
	}

	public BooleanProperty canResizeProperty() {
		return canResize;
	}

	public boolean isCanCopy() {
		return canCopy.get();
	}

	public CaptureService getCaptureService() {
		return captureService;
	}

	public PropertySettingPane getPropertySettingPane() {
		return propertySettingPane;
	}

	public void setImage(Image image) {

		PhyloImageAnalyzer.analyze(image, s -> {
			WindowNotifications.showWarning(windowPane, s);
		});
		getImageView().setImage(image);
	}
}

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

package phylosketch.capture;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.BasicFX;
import jloda.fx.util.ClipboardUtils;
import jloda.fx.util.SelectionEffectRed;
import net.sourceforge.tess4j.Word;
import phylocap.capture.Point;
import phylocap.capture.Segment;
import phylosketch.commands.EditImageCommand;
import phylosketch.view.DrawPane;
import phylosketch.window.MainWindowController;

import java.util.ArrayList;
import java.util.List;

/**
 * maintain an image in the background for image capture
 * Daniel Huson, 1.2025
 */
public class CapturePane extends HBox {
	private final DrawPane view;

	private final Rectangle selectionRectangle;

	private final Pane mainPane = new Pane();
	private final ImageView imageView = new ImageView();
	private final Group wordsGroup = new Group();
	private final Group pathsGroup = new Group();
	private final Group rootGroup = new Group();

	private final BooleanProperty hasImage = new SimpleBooleanProperty(this, "hasImage", false);
	private final BooleanProperty showCapture = new SimpleBooleanProperty(this, "showCapture", false);

	private final ObjectProperty<Point2D> rootLocation = new SimpleObjectProperty<>(this, "rootLocation", new Point2D(50, 50));
	private final BooleanProperty hasRootLocation = new SimpleBooleanProperty(this, "hasRootLocationProperty", false);

	private final BooleanProperty canSelect = new SimpleBooleanProperty(this, "canSelect", true);
	private final BooleanProperty canEdit = new SimpleBooleanProperty(this, "canEdit", true);
	private final BooleanProperty canResize = new SimpleBooleanProperty(this, "canResize", true);
	private final BooleanProperty canDelete = new SimpleBooleanProperty(this, "canDelete", false);
	private final BooleanProperty canCopy = new SimpleBooleanProperty(this, "canCopy", false);

	private final RunCapture runCapture;

	public CapturePane(DrawPane view, MainWindowController controller) {
		this.view = view;

		imageView.setOpacity(0.6);
		// imageView.setEffect(SelectionEffectRed.getInstance());
		imageView.imageProperty().addListener((v, o, n) -> {
			if (n != null) {
				imageView.setFitWidth(n.getWidth());
				imageView.setFitHeight(n.getHeight());
			}
		});

		hasImage.bind(imageView.imageProperty().isNotNull());

		selectionRectangle = new Rectangle();
		selectionRectangle.setFill(Color.WHITE.deriveColor(1, 1, 1, 0.1));
		selectionRectangle.setStroke(Color.DARKGREEN);
		selectionRectangle.getStrokeDashArray().setAll(3.0, 3.0);

		mainPane.getChildren().addAll(imageView);
		showCapture.addListener((v, o, n) -> {
			if (n) {
				mainPane.getChildren().addAll(wordsGroup, pathsGroup);
				view.getOtherGroup().getChildren().add(rootGroup);
			} else {
				mainPane.getChildren().removeAll(wordsGroup, pathsGroup);
				view.getOtherGroup().getChildren().remove(rootGroup);
			}
		});

		mainPane.getChildren().addListener((InvalidationListener) e -> canDelete.set(mainPane.getChildren().contains(selectionRectangle)));

		getStyleClass().add("viewer-background");
		setStyle("-fx-border-color: gray;-fx-padding: 5;");


		var closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(a -> {
			imageView.setImage(null);
			view.setMode(DrawPane.Mode.Edit);
		});

		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		SetupMouseInteraction.apply(view, this, resizeHandle, selectionRectangle);

		var rightBorderPane = new BorderPane();
		rightBorderPane.setTop(closeButton);
		rightBorderPane.setBottom(resizeHandle);
		getChildren().addAll(mainPane, rightBorderPane);
		canCopy.bind(showCaptureProperty());

		hasRootLocation.bind(rootGroup.visibleProperty());
		SetupRootTool.apply(getRootGroup(), rootLocation);
		getRootGroup().setVisible(false);

		runCapture = new RunCapture(controller, view, this);

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


	public ReadOnlyBooleanProperty canDeleteProperty() {
		return canDelete;
	}

	public ReadOnlyBooleanProperty canCopyProperty() {
		return canCopy;
	}

	public void copy() {
		if (mainPane.getChildren().contains(selectionRectangle) && selectionRectangle.getWidth() > 0 && selectionRectangle.getHeight() > 0) {
			var rect = EditImageCommand.screenToImage(selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal()), imageView);
			ClipboardUtils.putImage(EditImageCommand.getImageInRect(imageView.getImage(), rect));
		} else if (imageView.getImage() != null) {
			ClipboardUtils.putImage(imageView.getImage());
		}
	}

	public DrawPane getView() {
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
		return runCapture.getService().runningProperty();
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
			if (runCapture.getService().getInputImage() != getImageView().getImage())
				runCapture.getService().setInputImage(getImageView().getImage());
			runCapture.getService().run(goal);
		}
	}

	public ReadOnlyIntegerProperty getPhaseProperty() {
		return runCapture.getService().phaseProperty();
	}


	public ReadOnlyBooleanProperty hasRootLocationProperty() {
		return hasRootLocation;
	}

	public Point2D getRootLocationScreenCoordinates() {
		return view.getOtherGroup().localToScreen(rootLocation.get());
	}


	public void reset() {
		wordsGroup.getChildren().clear();
		pathsGroup.getChildren().clear();
		runCapture.getService().clearData();
		rootGroup.setVisible(false);
		getMainPane().getChildren().remove(selectionRectangle);
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

	public boolean isCanDelete() {
		return canDelete.get();
	}

	public boolean isCanCopy() {
		return canCopy.get();
	}

	public Point2D convertImageToScreen(double imageX, double imageY) {
		var screenX = imageX * imageView.getBoundsInLocal().getWidth() / imageView.getImage().getWidth() + imageView.localToScreen(0, 0).getX();
		var screenY = imageY * imageView.getBoundsInLocal().getHeight() / imageView.getImage().getHeight() + imageView.localToScreen(0, 0).getY();
		return new Point2D(screenX, screenY);
	}

	public List<Word> transformWords(ArrayList<Word> words) {
		var list = new ArrayList<Word>();
		for (var word : words) {
			var bbox = word.getBoundingBox();
			var localMin = wordsGroup.screenToLocal(convertImageToScreen(bbox.getX(), bbox.getY()));
			var x = (int) localMin.getX();
			var y = (int) localMin.getY();
			var localMax = wordsGroup.screenToLocal(convertImageToScreen(bbox.getX() + bbox.getWidth(), bbox.getY() + bbox.getHeight()));
			var width = (int) (localMax.getX() - localMin.getX());
			var height = (int) (localMax.getY() - localMin.getY());
			list.add(new Word(word.getText(), word.getConfidence(), new java.awt.Rectangle(x, y, width, height)));
		}
		return list;
	}

	public List<Segment> transformSegments(ArrayList<Segment> segments) {
		var list = new ArrayList<Segment>();
		for (var segment : segments) {
			var copy = new Segment();
			copy.points().addAll(segment.points().stream()
					.map(p -> pathsGroup.screenToLocal(convertImageToScreen(p.x(), p.y()))).
					map(p -> new Point((int) p.getX(), (int) p.getY())).toList());
			list.add(copy);
		}
		return list;
	}
}

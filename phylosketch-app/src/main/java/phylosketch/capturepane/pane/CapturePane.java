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

import javafx.application.Platform;
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
import jloda.fx.windownotifications.WindowNotifications;
import jloda.util.Basic;
import phylosketch.capturepane.capture.CaptureService;
import phylosketch.capturepane.capture.PhyloImageAnalyzer;
import phylosketch.capturepane.capture.RootFinder;
import phylosketch.utils.ScrollPaneUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;
import phylosketch.window.MainWindowController;
import phylosketch.window.MainWindowPresenter;

/**
 * maintain an image in the background for image capturepane
 * <p>
 * The root marker is shown by itself when a picture is there and nothing has been drawn or captured yet. It starts
 * where the root is likely to be, with the direction that the drawing is likely to grow in, and the view is
 * scrolled to it. Dropped near a line, it takes that line. Where the root is, is kept in the pixels of the picture,
 * so the marker stays on its place in the picture when the picture is resized.
 * Daniel Huson, 1.2025
 */
public class CapturePane extends HBox {
	// how far the root marker looks for a line when it is dropped, in pixels on the screen
	private static final double SNAP_RADIUS = 36;
	// the root marker can be seen if it is at least this far inside what is shown of the view, and this far below the
	// top of it, where the bar is, in pixels on the screen. If it is not, the view is scrolled
	private static final double VISIBLE_MARGIN = 40;
	private static final double VISIBLE_MARGIN_TOP = 110;

	private final DrawView view;
	private final MainWindowController controller;
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
	// where the root is, in the pixels of the picture; null as long as it has not been placed
	private final ObjectProperty<Point2D> rootLocation = new SimpleObjectProperty<>(this, "rootLocation");
	private RootFinder rootFinder; // what is known about the lines of the picture; null until it has been analysed
	private int analysis; // the number of the picture being analysed: the result for a picture that has been replaced is dropped
	private boolean analysed;
	private boolean rootPlacedByUser;
	private boolean rootHiddenByUser;

	private final CaptureService captureService;

	private final PropertySettingPane propertySettingPane = new PropertySettingPane();

	private final ObjectProperty<Rectangle> selectionRectangle = new SimpleObjectProperty<>(this, "selectionRectangle");

	public CapturePane(DrawView view, MainWindowController controller) {
		this.view = view;
		this.controller = controller;
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
				if (!mainPane.getChildren().contains(pathsGroup))
					mainPane.getChildren().remove(pathsGroup);
				if (!mainPane.getChildren().contains(wordsGroup))
					mainPane.getChildren().add(wordsGroup);
				view.getOtherGroup().getChildren().add(rootGroup);
			} else {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(controller.getScrollPane(), () -> {
					mainPane.getChildren().removeAll(pathsGroup, wordsGroup);
					view.getOtherGroup().getChildren().remove(rootGroup);
				});
			}
		});

		getStyleClass().add("viewer-background");
		setStyle("-fx-border-color: gray;");


		var closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(a -> controller.getClearCaptureImageItem().fire());

		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		SetupMouseInteraction.apply(view, this, resizeHandle, selectionRectangle);

		var rightBorderPane = new BorderPane();
		rightBorderPane.setTop(closeButton);
		rightBorderPane.setBottom(resizeHandle);
		getChildren().addAll(mainPane, rightBorderPane);
		canCopy.bind(showCaptureProperty());

		hasRootLocation.bind(rootGroup.visibleProperty());
		rootTool = new RootTool();
		getRootGroup().getChildren().add(rootTool);
		getRootGroup().setVisible(false);

		// the marker keeps its size on the screen
		rootTool.setZoom(controller.getScrollPane().getZoomX());
		controller.getScrollPane().zoomXProperty().addListener((v, o, n) -> rootTool.setZoom(n.doubleValue()));

		// ... and its place in the picture, also when the picture is resized or laid out anew
		rootLocation.addListener(e -> {
			placeRootTool();
			updateRootOnLine();
		});
		// (listeners for the changes, not for the invalidations: nothing else reads these, so they would be invalidated once and never again)
		imageView.localToSceneTransformProperty().addListener((v, o, n) -> placeRootTool());
		imageView.boundsInLocalProperty().addListener((v, o, n) -> placeRootTool());
		rootGroup.localToSceneTransformProperty().addListener((v, o, n) -> placeRootTool());

		rootTool.setOnDropped(() -> {
			var location = getRootToolLocation();
			if (location != null) {
				rootPlacedByUser = true;
				setRootLocationNearLine(location);
			}
		});

		// as long as the user has not placed the marker, it goes to where the root is likely to be for the side chosen
		rootTool.rootSideProperty().addListener((v, o, n) -> {
			if (!rootPlacedByUser && rootLocation.get() != null) {
				rootLocation.set(guessRootLocation(n));
				if (rootGroup.isVisible())
					Platform.runLater(this::scrollToRoot);
			}
		});

		imageView.imageProperty().addListener((v, o, n) -> analyse(n));
		view.modeProperty().addListener(e -> showRootToolForNewImage());

		captureService = SetupCaptureService.apply(controller, view, this);

		var captureBar = new CaptureBar(rootTool.rootSideProperty(), rootTool.onLineProperty(), captureService.runningProperty(), () -> run(CaptureService.PHYLOGENY));
		captureBar.visibleProperty().bind(view.modeProperty().isEqualTo(DrawView.Mode.Capture).and(hasImage).and(rootGroup.visibleProperty()));
		captureBar.managedProperty().bind(captureBar.visibleProperty());
		AnchorPane.setTopAnchor(captureBar, 8.0);
		AnchorPane.setLeftAnchor(captureBar, 0.0);
		AnchorPane.setRightAnchor(captureBar, 0.0);
		controller.getCenterAnchorPane().getChildren().add(captureBar);

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
			setShowRootTool(true);
		else {
			if (captureService.getInputImage() != getImageView().getImage())
				captureService.setInputImage(getImageView().getImage());
			captureService.setRootSide(getRootSide());
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

	/**
	 * where the root is
	 *
	 * @return the place in the pixels of the picture, or null if the root has not been placed
	 */
	public Point2D getRootLocation() {
		return rootLocation.get();
	}

	/**
	 * puts the root marker on a pixel of the picture, as it is, without looking for a line
	 *
	 * @param location the place in the pixels of the picture
	 */
	public void setRootLocation(Point2D location) {
		rootPlacedByUser = true;
		rootLocation.set(location);
		placeRootTool();
		updateRootOnLine();
	}

	/**
	 * is the root marker on a line of the picture? Also true as long as the lines of the picture are not known
	 */
	public ReadOnlyBooleanProperty rootOnLineProperty() {
		return rootTool.onLineProperty();
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

	/**
	 * shows or hides the root marker. When it is shown and has no place yet, it is put where the root is likely to be,
	 * and the view is scrolled to it
	 */
	public void setShowRootTool(boolean show) {
		if (show) {
			if (rootLocation.get() == null) {
				if (rootFinder != null && rootFinder.getSide() != null)
					setRootSide(rootFinder.getSide());
				rootLocation.set(guessRootLocation(getRootSide()));
			}
			rootHiddenByUser = false;
			rootGroup.setVisible(true);
			Platform.runLater(this::scrollToRoot);
		} else {
			rootHiddenByUser = true;
			rootGroup.setVisible(false);
		}
	}

	/**
	 * finds what there is to know about the lines of a new picture, in the background, and then shows the root marker
	 */
	private void analyse(Image image) {
		var number = ++analysis;
		rootFinder = null;
		analysed = false;
		rootPlacedByUser = false;
		rootHiddenByUser = false;
		rootGroup.setVisible(false);
		rootLocation.set(null);
		if (image == null || !MainWindowPresenter.SUPPORTS_CAPTURE)
			return;
		if (image.getPixelReader() == null || image.getWidth() < 1 || image.getHeight() < 1)
			return; // a file that could not be read as a picture: there is nothing to capture, so no marker
		setRootSide(RootPosition.Side.Left);
		var thread = new Thread(() -> {
			RootFinder finder = null;
			try {
				finder = new RootFinder(image);
			} catch (Throwable ex) { // the marker is then placed by hand
				Basic.caught(ex);
			}
			var result = finder;
			Platform.runLater(() -> {
				if (number == analysis) {
					rootFinder = result;
					analysed = true;
					if (rootLocation.get() != null && !rootPlacedByUser) { // shown before the lines were known: now put it where they say
						if (result != null && result.getSide() != null)
							setRootSide(result.getSide());
						rootLocation.set(guessRootLocation(getRootSide()));
						if (rootGroup.isVisible())
							Platform.runLater(this::scrollToRoot);
					}
					updateRootOnLine();
					showRootToolForNewImage();
				}
			});
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * shows the root marker by itself if there is a picture to capture and nothing has been drawn or captured yet
	 */
	private void showRootToolForNewImage() {
		if (analysed && !rootGroup.isVisible() && !rootHiddenByUser && hasImage.get() && view.getMode() == DrawView.Mode.Capture && view.getGraph().getNumberOfNodes() == 0)
			setShowRootTool(true);
	}

	/**
	 * where the root is likely to be for a drawing that grows from the given side
	 */
	private Point2D guessRootLocation(RootPosition.Side side) {
		var guess = (rootFinder != null ? rootFinder.getRoot(side) : null);
		if (guess == null) { // nothing is known: the middle of that side of the picture
			var image = imageView.getImage();
			if (image == null)
				return null;
			guess = switch (side) {
				case Left -> new Point2D(0.05 * image.getWidth(), 0.5 * image.getHeight());
				case Right -> new Point2D(0.95 * image.getWidth(), 0.5 * image.getHeight());
				case Top -> new Point2D(0.5 * image.getWidth(), 0.05 * image.getHeight());
				case Bottom -> new Point2D(0.5 * image.getWidth(), 0.95 * image.getHeight());
				case Center -> new Point2D(0.5 * image.getWidth(), 0.5 * image.getHeight());
			};
		}
		return guess;
	}

	/**
	 * puts the root at the middle of the line nearest to the given place, or at that place if there is no line near it
	 *
	 * @param location the place in the pixels of the picture
	 */
	private void setRootLocationNearLine(Point2D location) {
		var image = imageView.getImage();
		if (rootFinder != null && image != null) {
			// as far as the marker looks on the screen, in the pixels of the picture
			var pixelsOnScreen = controller.getScrollPane().getZoomX() * imageView.getBoundsInLocal().getWidth() / image.getWidth();
			var onLine = rootFinder.nearestLine(location.getX(), location.getY(), SNAP_RADIUS / Math.max(0.01, pixelsOnScreen));
			if (onLine != null)
				location = onLine;
		}
		rootLocation.set(location);
		placeRootTool(); // the marker has been dragged, so it is to be put back even if the place has not changed
		updateRootOnLine();
	}

	private void updateRootOnLine() {
		var location = rootLocation.get();
		rootTool.setOnLine(rootFinder == null || location != null && rootFinder.isOnLine(location.getX(), location.getY()));
	}

	/**
	 * moves the root marker to where the root is in the picture
	 */
	private void placeRootTool() {
		var location = rootLocation.get();
		var image = imageView.getImage();
		if (location == null || image == null || rootTool.isDragging() || imageView.getScene() == null || rootGroup.getScene() == null)
			return;
		var bounds = imageView.getBoundsInLocal();
		var inScene = imageView.localToScene(location.getX() / image.getWidth() * bounds.getWidth(), location.getY() / image.getHeight() * bounds.getHeight());
		var inGroup = rootGroup.sceneToLocal(inScene);
		if (inGroup != null && (Math.abs(inGroup.getX() - rootTool.getTranslateX()) > 1e-6 || Math.abs(inGroup.getY() - rootTool.getTranslateY()) > 1e-6)) {
			rootTool.setTranslateX(inGroup.getX());
			rootTool.setTranslateY(inGroup.getY());
		}
	}

	/**
	 * where the root marker is, in the pixels of the picture
	 */
	private Point2D getRootToolLocation() {
		var image = imageView.getImage();
		if (image == null || imageView.getScene() == null || rootTool.getScene() == null)
			return null;
		var bounds = imageView.getBoundsInLocal();
		var local = imageView.sceneToLocal(rootTool.localToScene(0, 0));
		return new Point2D(local.getX() / bounds.getWidth() * image.getWidth(), local.getY() / bounds.getHeight() * image.getHeight());
	}

	/**
	 * scrolls the view, if necessary, so that the root marker can be seen: if the marker is out of view, at the very
	 * border of what is shown, or below the bar, it is brought to the middle, as far as the view can be scrolled
	 */
	private void scrollToRoot() {
		var scrollPane = controller.getScrollPane();
		if (!rootGroup.isVisible() || rootTool.getScene() == null || scrollPane.getScene() == null)
			return;
		scrollPane.applyCss();
		scrollPane.layout();
		placeRootTool();

		var outer = scrollPane.getOuterNode();
		var viewport = scrollPane.getViewportBounds();
		var maxX = outer.getLayoutBounds().getWidth() - viewport.getWidth();
		var maxY = outer.getLayoutBounds().getHeight() - viewport.getHeight();
		var left = (maxX > 0 ? scrollPane.getHvalue() * maxX : 0);
		var top = (maxY > 0 ? scrollPane.getVvalue() * maxY : 0);
		var root = outer.sceneToLocal(rootTool.localToScene(0, 0));
		if (root == null)
			return;
		var margin = Math.min(VISIBLE_MARGIN, viewport.getWidth() / 4);
		var marginTop = Math.min(VISIBLE_MARGIN_TOP, viewport.getHeight() / 4);
		if (maxX > 0 && (root.getX() - margin < left || root.getX() + margin > left + viewport.getWidth()))
			scrollPane.setHvalue(Math.max(0, Math.min(1, (root.getX() - 0.5 * viewport.getWidth()) / maxX)));
		if (maxY > 0 && (root.getY() - marginTop < top || root.getY() + margin > top + viewport.getHeight()))
			scrollPane.setVvalue(Math.max(0, Math.min(1, (root.getY() - 0.5 * viewport.getHeight()) / maxY)));
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
		if (phylosketch.window.MainWindowPresenter.SUPPORTS_CAPTURE) {
			PhyloImageAnalyzer.analyze(image, s -> {
				WindowNotifications.showWarning(windowPane, s);
			});
		}
		getImageView().setImage(image);
	}
}

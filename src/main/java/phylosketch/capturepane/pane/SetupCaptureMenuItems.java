/*
 * SetupCaptureMenuItems.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.scene.layout.AnchorPane;
import jloda.fx.dialog.ExportImageDialog;
import jloda.fx.undo.UndoableRedoableCommand;
import phylosketch.capturepane.capture.CaptureService;
import phylosketch.main.PhyloSketch;
import phylosketch.utils.ScrollPaneUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;
import phylosketch.window.MainWindow;
import phylosketch.window.MainWindowController;

/**
 * setup capture pane and associated menu items
 * Daniel Huson, 1.2025
 */
public class SetupCaptureMenuItems {
	/**
	 * setup the capture pane
	 *
	 * @param window        the main window
	 * @param controller  the main controller
	 * @param capturePane the capture pane
	 */
	public static void apply(MainWindow window, MainWindowController controller, CapturePane capturePane) {
		var view = window.getDrawView();
		var canSetup = new SimpleBooleanProperty(capturePane, "canSetRoot", false);
		canSetup.bind(view.modeProperty().isEqualTo(DrawView.Mode.Capture).and(capturePane.hasImageProperty()));
		var canCapture = new SimpleBooleanProperty(capturePane, "canCapture", false);
		canCapture.bind(canSetup.and(capturePane.hasRootLocationProperty()));

		controller.getCaptureMenuButton().visibleProperty().bind(view.modeProperty().isEqualTo(DrawView.Mode.Capture));

		controller.getCaptureMenuButton().getGraphic().setOnMouseClicked(e -> {
			if (!capturePane.getRootGroup().isVisible())
				controller.getShowCaptureRootLocationItem().fire();
			else
				controller.getCapturePhylogenyItem().fire();
			e.consume();
		});
		controller.getCaptureMenuButton().getGraphic().setOnMousePressed(Event::consume);
		controller.getCaptureMenuButton().getGraphic().setOnMouseReleased(Event::consume);
		controller.getCaptureMenuButton().getGraphic().disableProperty().bind(canSetup.not());

		controller.getClearCaptureImageItem().setOnAction(e -> {
			var image = capturePane.getImageView().getImage();
			var removeCommand = UndoableRedoableCommand.create("remove image", () -> {
				view.setMode(DrawView.Mode.Capture);
				capturePane.getImageView().setImage(image);
			}, () -> {
				capturePane.getImageView().setImage(null);
				capturePane.reset();
				view.setMode(DrawView.Mode.Sketch);
			});
			view.getUndoManager().doAndAdd(removeCommand);
		});
		controller.getClearCaptureImageItem().disableProperty().bind(capturePane.hasImageProperty().not());

		controller.getSaveCaptureImageItem().setOnAction(e -> ExportImageDialog.show(window.getFileName(), window.getStage(), capturePane.getImageView()));
		controller.getSaveCaptureImageItem().disableProperty().bind(capturePane.hasImageProperty().not());

		var settingPane = capturePane.getPropertySettingPane();

		controller.getShowCaptureParametersItem().selectedProperty().addListener((v, o, n) -> {
			if (n && !controller.getCenterAnchorPane().getChildren().contains(settingPane.getPane())) {
				settingPane.getPane().setStyle("-fx-background-color: -fx-control-inner-background;-fx-border-color: grey;-fx-border-width: 1;");
				AnchorPane.setLeftAnchor(settingPane.getPane(), 50.0);
				AnchorPane.setTopAnchor(settingPane.getPane(), 5.0);
				controller.getCenterAnchorPane().getChildren().add(settingPane.getPane());
			} else {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(view, () -> controller.getCenterAnchorPane().getChildren().remove(settingPane.getPane()));
			}
		});
		controller.getShowCaptureParametersItem().disableProperty().bind(canSetup.not());

		view.modeProperty().addListener((v, o, n) -> {
			if (n != DrawView.Mode.Capture) {
				controller.getShowCaptureParametersItem().setSelected(false);
			}
		});

		if (PhyloSketch.isDesktop()) {
			controller.getLoadCaptureImageItem().setOnAction(e -> controller.getOpenImageFileItem().fire());
			controller.getLoadCaptureImageItem().disableProperty().bind(controller.getOpenImageFileItem().disableProperty());
		} else {
			controller.getCaptureMenuButton().getItems().remove(controller.getLoadCaptureImageItem());
		}

		controller.getShowCaptureRootLocationItem().setOnAction(e -> {
			var showing = capturePane.getRootGroup().isVisible();
			view.getUndoManager().doAndAdd("show root location", () -> capturePane.getRootGroup().setVisible(showing), () -> capturePane.getRootGroup().setVisible(!showing));
		});
		controller.getShowCaptureRootLocationItem().disableProperty().bind(canSetup.not());
		capturePane.hasRootLocationProperty().addListener((v, o, n) -> controller.getShowCaptureRootLocationItem().setSelected(n));

		capturePane.rootSideProperty().addListener((v, o, n) -> {
			if (!view.getUndoManager().isPerformingUndoOrRedo())
				view.getUndoManager().add("root side", () -> capturePane.setRootSide(o), () -> capturePane.setRootSide(n));
		});

		controller.getLeftRootSideMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				capturePane.setRootSide(RootPosition.Side.Left);
		});

		controller.getRightRootSideMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				capturePane.setRootSide(RootPosition.Side.Right);
		});

		controller.getTopRootSideMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				capturePane.setRootSide(RootPosition.Side.Top);
		});

		controller.getBottomRootSideMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				capturePane.setRootSide(RootPosition.Side.Bottom);
		});

		controller.getCenterRootSideMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				capturePane.setRootSide(RootPosition.Side.Center);
		});

		InvalidationListener listener = e -> {
			switch (capturePane.getRootSide()) {
				case Left -> controller.getLeftRootSideMenuItem().setSelected(true);
				case Right -> controller.getRightRootSideMenuItem().setSelected(true);
				case Top -> controller.getTopRootSideMenuItem().setSelected(true);
				case Bottom -> controller.getBottomRootSideMenuItem().setSelected(true);
				case Center -> controller.getCenterRootSideMenuItem().setSelected(true);
			}
		};
		capturePane.rootSideProperty().addListener(listener);
		listener.invalidated(null);

		controller.getRootSideMenu().disableProperty().bind(canSetup.not());

		controller.getCaptureLabelsItem().setOnAction(e ->
				view.getUndoManager().doAndAdd("capture", capturePane::reset, () -> capturePane.run(CaptureService.WORDS)));
		controller.getCaptureLabelsItem().disableProperty().bind(canCapture.not());

		controller.getCaptureLinesItem().setOnAction(e -> view.getUndoManager().doAndAdd("capture", capturePane::reset, () -> capturePane.run(CaptureService.DUSTED)));
		controller.getCaptureLinesItem().disableProperty().bind(capturePane.hasRootLocationProperty().not().or(canCapture.not()));

		controller.getCapturePhylogenyItem().setOnAction(e -> view.getUndoManager().doAndAdd("capture", capturePane::reset, () -> capturePane.run(CaptureService.PHYLOGENY)));
		controller.getCapturePhylogenyItem().disableProperty().bind(capturePane.hasRootLocationProperty().not().or(canCapture.not()));

		// controller.getCaptureMenuButton().visibleProperty().bind(view.modeProperty().isEqualTo(DrawView.Mode.Capture));

		capturePane.getPhaseProperty().addListener((v, o, n) -> {
			var phase = n.intValue();
			controller.getCaptureLabelsItem().setSelected(phase >= CaptureService.WORDS);
			controller.getCaptureLinesItem().setSelected(phase >= CaptureService.DUSTED);
			controller.getCapturePhylogenyItem().setSelected(phase >= CaptureService.PHYLOGENY);
		});

		InvalidationListener showImageListener = e -> {
			if (view.getMode() == DrawView.Mode.Capture) {
				if (capturePane.hasImageProperty().get()) {
					if (!view.getBackgroundGroup().getChildren().contains(capturePane))
						view.getBackgroundGroup().getChildren().add(capturePane);
					capturePane.showCaptureProperty().set(true);
				}
			} else {
				capturePane.showCaptureProperty().set(false);
				if (!capturePane.hasImageProperty().get()) {
					ScrollPaneUtils.runRemoveAndKeepScrollPositions(view, () -> view.getBackgroundGroup().getChildren().remove(capturePane));
				}
			}
		};
		view.modeProperty().addListener(showImageListener);
		capturePane.hasImageProperty().addListener(showImageListener);

		capturePane.getRootGroup().visibleProperty().addListener((v, o, n) -> {
			if (n) {
				controller.getScrollPane().ensureVisible(capturePane.getRootGroup());
			}
		});
	}
}

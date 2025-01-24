/*
 * SetupCapturePane.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.event.Event;
import jloda.fx.undo.UndoableRedoableCommand;
import phylocap.capture.CaptureService;
import phylosketch.view.DrawPane;
import phylosketch.window.MainWindowController;

/**
 * setup capture pane and associated menu items
 * Daniel Huson, 1.2025
 */
public class SetupCapturePane {
	public static CapturePane apply(DrawPane view, MainWindowController controller) {
		var captureImagePane = new CapturePane(view, controller);
		controller.getClearImageMenuItem().setOnAction(e -> {
			var image = captureImagePane.getImageView().getImage();
			var removeCommand = UndoableRedoableCommand.create("remove image", () -> {
				view.setMode(DrawPane.Mode.Capture);
				captureImagePane.getImageView().setImage(image);
			}, () -> {
				captureImagePane.getImageView().setImage(null);
				captureImagePane.reset();
				view.setMode(DrawPane.Mode.Edit);
			});
			view.getUndoManager().doAndAdd(removeCommand);
		});
		controller.getClearImageMenuItem().disableProperty().bind(captureImagePane.hasImageProperty().not());

		controller.getCaptureResetMenuItem().setOnAction(e -> {
			captureImagePane.reset();
		});
		controller.getCaptureResetMenuItem().disableProperty().bind(captureImagePane.hasRootLocationProperty().not());

		controller.getCaptureRootLocationMenuItem().setOnAction(e -> {
			captureImagePane.getRootGroup().setVisible(!captureImagePane.getRootGroup().isVisible());
		});
		controller.getCaptureRootLocationMenuItem().disableProperty().bind(captureImagePane.hasImageProperty().not());
		captureImagePane.hasRootLocationProperty().addListener((v, o, n) -> controller.getCaptureRootLocationMenuItem().setSelected(n));

		controller.getCaptureLabelsMenuItem().setOnAction(e -> {
			captureImagePane.run(CaptureService.WORDS);
		});
		controller.getCaptureLabelsMenuItem().disableProperty().bind(captureImagePane.hasRootLocationProperty().not());

		controller.getCaptureLinesMenuItem().setOnAction(e -> {
			captureImagePane.run(CaptureService.DUSTED);
		});
		controller.getCaptureLinesMenuItem().disableProperty().bind(captureImagePane.hasRootLocationProperty().not());

		controller.getCapturePhylogenyMenuItem().setOnAction(e -> {
			captureImagePane.run(CaptureService.PHYLOGENY);
		});
		controller.getCapturePhylogenyMenuItem().disableProperty().bind(captureImagePane.hasRootLocationProperty().not());

		controller.getCaptureMenuButton().visibleProperty().bind(view.modeProperty().isEqualTo(DrawPane.Mode.Capture));
		controller.getCaptureMenuButton().getGraphic().setOnMouseClicked(e -> {
			captureImagePane.run(CaptureService.PHYLOGENY);
			e.consume();
		});
		controller.getCaptureMenuButton().getGraphic().setOnMousePressed(Event::consume);
		controller.getCaptureMenuButton().getGraphic().setOnMouseReleased(Event::consume);
		controller.getCaptureMenuButton().getGraphic().disableProperty().bind(captureImagePane.hasImageProperty().not());

		captureImagePane.getPhaseProperty().addListener((v, o, n) -> {
			var phase = n.intValue();
			controller.getCaptureLabelsMenuItem().setSelected(phase >= CaptureService.WORDS);
			controller.getCaptureLinesMenuItem().setSelected(phase >= CaptureService.DUSTED);
			controller.getCapturePhylogenyMenuItem().setSelected(phase >= CaptureService.PHYLOGENY);
		});

		InvalidationListener showImageListener = e -> {
			if (view.getMode() == DrawPane.Mode.Capture) {
				if (captureImagePane.hasImageProperty().get()) {
					if (!view.getBackgroundGroup().getChildren().contains(captureImagePane))
						view.getBackgroundGroup().getChildren().add(captureImagePane);
					captureImagePane.showCaptureProperty().set(true);
				}
			} else {
				captureImagePane.showCaptureProperty().set(false);
				if (!captureImagePane.hasImageProperty().get())
					view.getBackgroundGroup().getChildren().remove(captureImagePane);
			}
		};
		view.modeProperty().addListener(showImageListener);
		captureImagePane.hasImageProperty().addListener(showImageListener);


		return captureImagePane;
	}
}

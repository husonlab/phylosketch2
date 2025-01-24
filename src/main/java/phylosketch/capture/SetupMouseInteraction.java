/*
 * SetupMouseInteraction.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import phylosketch.commands.EditImageCommand;
import phylosketch.view.DrawPane;

import static phylosketch.commands.EditImageCommand.screenToImage;

/**
 * setup mouse interaction for the capture pane
 * Daniel Huson, 1.2025
 */
public class SetupMouseInteraction {
	private static double mouseX;
	private static double mouseY;
	private static double mouseDownX;
	private static double mouseDownY;

	private static Bounds sourceRect;

	public static void apply(DrawPane view, CapturePane capturePane, Node resizeHandle, Rectangle selectionRectangle) {

		selectionRectangle.setOnMousePressed(e -> {
			if (capturePane.isCanEdit()) {
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
				sourceRect = null;
			}
			e.consume();
		});

		selectionRectangle.setOnMouseDragged(e -> {
			if (capturePane.isCanEdit()) {
				if (sourceRect == null) {
					sourceRect = screenToImage(selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal()), capturePane.getImageView());
				}
				var dx = e.getScreenX() - mouseX;
				var dy = e.getScreenY() - mouseY;
				selectionRectangle.setX(selectionRectangle.getX() + dx);
				selectionRectangle.setY(selectionRectangle.getY() + dy);
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
			}
			e.consume();
		});

		selectionRectangle.setOnMouseReleased(e -> {
			if (capturePane.isCanEdit()) {
				if (sourceRect != null) {
					var targetRect = screenToImage(selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal()), capturePane.getImageView());
					view.getUndoManager().doAndAdd(new EditImageCommand("move", capturePane.getImageView(), sourceRect, targetRect));
				}
			}
		});

		capturePane.getMainPane().setOnMousePressed(e -> {
			if (capturePane.isCanSelect()) {
				var location = capturePane.screenToLocal(e.getScreenX(), e.getScreenY());
				selectionRectangle.setX(location.getX());
				selectionRectangle.setY(location.getY());
				selectionRectangle.setWidth(0.1);
				selectionRectangle.setHeight(0.1);
			}
			e.consume();
		});

		capturePane.getMainPane().setOnMouseDragged(e -> {
			if (capturePane.isCanSelect()) {
				if (!capturePane.getMainPane().getChildren().contains(selectionRectangle)) {
					capturePane.getMainPane().getChildren().add(selectionRectangle);
				}
				var location = capturePane.screenToLocal(e.getScreenX(), e.getScreenY());
				var dx = location.getX() - selectionRectangle.getX();
				if (dx > 0)
					selectionRectangle.setWidth(dx);
				else {
					selectionRectangle.setWidth(selectionRectangle.getWidth() - dx);
					selectionRectangle.setX(location.getX());
				}

				var dy = location.getY() - selectionRectangle.getY();
				if (dy > 0)
					selectionRectangle.setHeight(dy);
				else {
					selectionRectangle.setHeight(selectionRectangle.getHeight() - dy);
					selectionRectangle.setY(location.getY());
				}
			}
			e.consume();
		});

		capturePane.getMainPane().setOnMouseReleased(e -> {
			if (capturePane.isCanSelect()) {
				if (selectionRectangle.getWidth() < 1 && selectionRectangle.getHeight() < 1)
					capturePane.getMainPane().getChildren().remove(selectionRectangle);
			}
			e.consume();
		});

		capturePane.getMainPane().setOnMouseClicked(Event::consume);

		{
			var imageView = capturePane.getImageView();

			resizeHandle.setOnMousePressed(e -> {
				mouseX = mouseDownX = e.getScreenX();
				mouseY = mouseDownY = e.getScreenY();
				e.consume();
			});
			resizeHandle.setOnMouseDragged(e -> {
				var dx = e.getScreenX() - mouseX;
				var dy = e.getScreenY() - mouseY;
				if (imageView.getFitWidth() + dx >= 100) {
					imageView.setFitWidth(imageView.getFitWidth() + dx);
				}
				if (imageView.getFitHeight() + dx >= 100) {
					imageView.setFitHeight(imageView.getFitHeight() + dy);
				}
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
				e.consume();
			});

			resizeHandle.setOnMouseReleased(e -> {
				if (mouseX != mouseDownX || mouseY != mouseDownY) {
					var oldX = imageView.getFitWidth() - (mouseX - mouseDownX);
					var oldY = imageView.getFitHeight() - (mouseY - mouseDownY);
					var newX = imageView.getFitWidth();
					var newY = imageView.getFitHeight();
					view.getUndoManager().add("resize image", () -> {
						imageView.setFitWidth(oldX);
						imageView.setFitHeight(oldY);
					}, () -> {
						imageView.setFitWidth(newX);
						imageView.setFitHeight(newY);
					});
				}
				e.consume();
			});
		}
	}
}

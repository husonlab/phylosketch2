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

package phylosketch.capturepane.pane;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import phylosketch.view.DrawView;

/**
 * setup mouse interaction for the capturepane pane
 * Daniel Huson, 1.2025
 */
public class SetupMouseInteraction {
	private static double mouseX;
	private static double mouseY;
	private static double mouseDownX;
	private static double mouseDownY;

	public static void apply(DrawView view, CapturePane capturePane, Node resizeHandle,
							 ObjectProperty<Rectangle> selectionRectangle) {

		if (false)
			capturePane.getMainPane().setOnMouseClicked(view.getOnMouseClicked());

		if (false) {
			selectionRectangle.set(new Rectangle(10, 10));
			selectionRectangle.get().setFill(Color.TRANSPARENT);
			selectionRectangle.get().setStroke(Color.LIGHTBLUE);

			capturePane.getMainPane().getChildren().add(selectionRectangle.get());

			capturePane.getImageView().setOnMousePressed(e -> {
				mouseX = mouseDownX = e.getScreenX();
				mouseY = mouseDownY = e.getScreenY();
				var rect = selectionRectangle.get();
				rect.setX(mouseX);
				rect.setY(mouseY);
				rect.setWidth(0);
				rect.setHeight(0);
				rect.setVisible(false);
				e.consume();
			});

			capturePane.getImageView().setOnMouseDragged(e -> {
				var dx = e.getScreenX() - mouseX;
				var dy = e.getScreenY() - mouseY;
				var rect = selectionRectangle.get();
				rect.setWidth(rect.getWidth() + dx);
				rect.setHeight(rect.getHeight() + dy);
				rect.setVisible(true);
				e.consume();

				mouseX = mouseDownX = e.getScreenX();
				mouseY = mouseDownY = e.getScreenY();
			});
		}

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

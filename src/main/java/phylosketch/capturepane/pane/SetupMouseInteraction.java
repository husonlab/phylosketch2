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

import javafx.geometry.Bounds;
import javafx.scene.Node;
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

	private static Bounds sourceRect;

	public static void apply(DrawView view, CapturePane capturePane, Node resizeHandle) {

		capturePane.getMainPane().setOnMouseClicked(view.getOnMouseClicked());

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

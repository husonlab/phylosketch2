/*
 * LabelUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoManager;

public class LabelUtils {
	private static double mouseDownX;
	private static double mouseDownY;
	private static double mouseX;
	private static double mouseY;
	private static boolean wasDragged;

	public static void makeDraggable(RichTextLabel label, ReadOnlyBooleanProperty allow, Pane pane, UndoManager undoManager) {

		label.setOnMousePressed(e -> {
			if (!label.getRawText().isBlank()) {
				mouseDownX = e.getScreenX();
				mouseDownY = e.getScreenY();
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
				wasDragged = false;
				e.consume();
			}

		});
		label.setOnMouseDragged(e -> {
			if (allow.get() && !label.getRawText().isBlank()) {
				var previous = pane.screenToLocal(mouseX, mouseY);
				var current = pane.screenToLocal(e.getScreenX(), e.getScreenY());
				var delta = new Point2D(current.getX() - previous.getX(), current.getY() - previous.getY());
				label.setLayoutX(label.getLayoutX() + delta.getX());
				label.setLayoutY(label.getLayoutY() + delta.getY());
				wasDragged = true;
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
			}
			e.consume();
		});
		label.setOnMouseReleased(e -> {
			if (wasDragged && !label.getRawText().isBlank()) {
				undoManager.add("move label", () -> {
					var previous = pane.screenToLocal(mouseDownX, mouseDownY);
					var current = pane.screenToLocal(e.getScreenX(), e.getScreenY());
					var delta = new Point2D(current.getX() - previous.getX(), current.getY() - previous.getY());

					label.setLayoutX(label.getLayoutX() - delta.getX());
					label.setLayoutY(label.getLayoutY() - delta.getY());
				}, () -> {
					var previous = pane.screenToLocal(mouseDownX, mouseDownY);
					var current = pane.screenToLocal(e.getScreenX(), e.getScreenY());
					var delta = new Point2D(current.getX() - previous.getX(), current.getY() - previous.getY());
					label.setLayoutX(label.getLayoutX() + delta.getX());
					label.setLayoutY(label.getLayoutY() + delta.getY());
				});
			}
			e.consume();
		});
	}

}

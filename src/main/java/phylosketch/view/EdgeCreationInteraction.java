/*
 * EdgeCreationInteraction.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import phylosketch.commands.NewEdgeCommmand;

import static phylosketch.paths.PathUtils.getCoordinates;

public class EdgeCreationInteraction {
	private static double mouseDownX;
	private static double mouseDownY;

	private final static Path path = new Path();

	static {
		path.getStyleClass().add("graph-edge");
	}

	/**
	 * setup the interaction
	 *
	 * @param view
	 */
	public static void setup(DrawPane view) {

		view.setOnMousePressed(me -> {
			mouseDownX = me.getSceneX();
			mouseDownY = me.getSceneY();

			if (view.getMode() == DrawPane.Mode.Edit) {
				var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
				path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
				if (!view.getEdgesGroup().getChildren().contains(path))
					view.getEdgesGroup().getChildren().add(path);
				me.consume();
			}
		});

		view.setOnMouseDragged(me -> {
			if (view.getMode() == DrawPane.Mode.Edit) {
				if (!path.getElements().isEmpty()) {
					view.getNodeSelection().clearSelection();
					view.getEdgeSelection().clearSelection();

					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					path.getElements().add(new LineTo(location.getX(), location.getY()));
				}
			}
			me.consume();
		});

		view.setOnMouseReleased(me -> {
			if (view.getMode() == DrawPane.Mode.Edit) {
				if (!path.getElements().isEmpty()) {
					view.getEdgesGroup().getChildren().remove(path);
					if (isGoodPath(path)) {
						NewEdgeCommmand.doAndAdd(view, path);
					}
					path.getElements().clear();
				}
			}
			me.consume();
		});
	}

	public static boolean isGoodPath(Path path) {
		Point2D first = null;
		for (var element : path.getElements()) {
			if (element instanceof LineTo || element instanceof MoveTo) {
				var coordinates = getCoordinates(element);
				if (first == null) {
					first = coordinates;
				} else {
					if (first.distance(coordinates) >= 10)
						return true;
				}
			}
		}
		return false;
	}
}

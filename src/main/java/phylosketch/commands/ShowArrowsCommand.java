/*
 * ShowArrowsCommand.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.commands;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Edge;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class ShowArrowsCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final int[] edgeIds;

	public ShowArrowsCommand(DrawPane view, boolean show) {
		super("arrows");

		edgeIds = view.getEdgeSelection().getSelectedItems().stream().mapToInt(e -> e.getId()).toArray();

		undo = () -> {
			var edges = Arrays.stream(edgeIds).mapToObj(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toList();
			showArrows(view, edges, !show);
		};
		redo = () -> {
			var edges = Arrays.stream(edgeIds).mapToObj(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toList();
			showArrows(view, edges, show);
		};

	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	public static void showArrows(DrawPane view, Collection<Edge> edges, boolean show) {
		if (!show) {
			for (var e : edges) {
				view.getEdgeArrowMap().remove(e);
			}
		} else {
			for (var e : edges) {
				if (!view.getEdgeArrowMap().containsKey(e)) {
					if (e.getData() instanceof Path path) {
						var shape = new Polygon(7.0, 0.0, -7.0, 4.0, -7.0, -4.0);
						shape.getStyleClass().add("graph-node");
						view.getEdgeArrowMap().put(e, shape);

						InvalidationListener listener = a -> {
							var points = PathUtils.extractPoints(path);
							if (points.size() >= 2) {
								var lastId = points.size() - 1;
								var last = points.get(lastId);
								var firstId = lastId;
								while (firstId > 0 && last.distance(points.get(firstId)) < 8) {
									firstId--;
								}
								var direction = last.subtract(points.get(firstId));
								direction = direction.multiply(1.0 / direction.magnitude());
								shape.setRotate(GeometryUtilsFX.computeAngle(direction));
								shape.setTranslateX(last.getX() - 12 * direction.getX());
								shape.setTranslateY(last.getY() - 12 * direction.getY());
							}
						};
						listener.invalidated(null);
						shape.setUserData(listener);
						path.getElements().addListener(new WeakInvalidationListener(listener));
						((Shape) e.getTarget().getData()).translateXProperty().addListener(new WeakInvalidationListener(listener));
						((Shape) e.getTarget().getData()).translateYProperty().addListener(new WeakInvalidationListener(listener));
					}
				}
			}
		}
	}
}

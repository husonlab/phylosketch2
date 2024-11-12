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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import phylosketch.commands.AddEdgeCommand;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;

import static phylosketch.paths.PathUtils.getCoordinates;

public class EdgeCreationInteraction {
	private static double mouseDownX;
	private static double mouseDownY;
	private static final BooleanProperty dragAllowed = new SimpleBooleanProperty(EdgeCreationInteraction.class, "dragAllowed", false);
	private static Timeline dragDelayTimeline;

	static {
		dragDelayTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> dragAllowed.set(true)));
		dragDelayTimeline.setCycleCount(1);
	}

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
		if (PhyloSketch.isDesktop()) {
			dragAllowed.addListener((v, o, n) -> {
				view.setCursor(n ? Cursor.CROSSHAIR : Cursor.DEFAULT);
			});
		}

		view.setOnMousePressed(me -> {
			mouseDownX = me.getSceneX();
			mouseDownY = me.getSceneY();

			if (view.getMode() == DrawPane.Mode.Edit) {
				var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
				path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
				if (!view.getEdgesGroup().getChildren().contains(path))
					view.getEdgesGroup().getChildren().add(path);

				// if we don't hit a node or edge, then need to press at least 0.5 seconds to create a node
				if (AddEdgeCommand.findNode(view, location) == null && AddEdgeCommand.findEdge(view, location) == null) {
					dragAllowed.set(false);
					dragDelayTimeline.playFromStart();
				} else {
					dragAllowed.set(true);
				}
				me.consume();
			}
		});

		view.setOnMouseDragged(me -> {
			if (view.getMode() == DrawPane.Mode.Edit) {
				if (dragAllowed.get()) {
					if (!path.getElements().isEmpty()) {
						view.getNodeSelection().clearSelection();
						view.getEdgeSelection().clearSelection();

						var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
						path.getElements().add(new LineTo(location.getX(), location.getY()));
					}
				}
				dragDelayTimeline.stop();
			}
			me.consume();
		});

		view.setOnMouseReleased(me -> {
			if (view.getMode() == DrawPane.Mode.Edit) {
				if (!path.getElements().isEmpty()) {
					view.getEdgesGroup().getChildren().remove(path);
					if (isGoodPath(path)) {
						path.getElements().setAll(PathUtils.createPath(PathSmoother.apply(PathUtils.extractPoints(path), 10), true).getElements());
						view.getUndoManager().doAndAdd(new AddEdgeCommand(view, path));
					}
					path.getElements().clear();
					dragDelayTimeline.stop();
					dragAllowed.set(false);
				}
			}
			me.consume();
		});
	}

	public static boolean isGoodPath(Path path) {
		Point2D first = null;
		for (var element : path.getElements()) {
				var coordinates = getCoordinates(element);
				if (first == null) {
					first = coordinates;
				} else {
					if (first.distance(coordinates) >= 10)
						return true;
			}
		}
		return false;
	}
}

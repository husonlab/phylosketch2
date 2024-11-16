/*
 * PaneInteraction.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import jloda.fx.util.BasicFX;
import jloda.util.Single;
import phylosketch.commands.AddEdgeCommand;
import phylosketch.commands.AddNodeCommand;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;

import static phylosketch.paths.PathUtils.getCoordinates;

public class PaneInteraction {
	public static final BooleanProperty inMultiTouchGesture = new SimpleBooleanProperty(PaneInteraction.class, "inMultiTouchGesture", false);

	private final static Path path = new Path();

	static {
		path.getStyleClass().add("graph-edge");
	}

	/**
	 * setup the interaction
	 */
	public static void setup(DrawPane view, BooleanProperty allowResize) {
		var inDrag = new Single<>(false);
		var inLongPressCreateNode = new Single<>(false);
		var mouseDownTime = new Single<>(Long.MAX_VALUE);

		view.setOnMouseClicked(me -> {
			if (me.getClickCount() == 1) {
				allowResize.set(false);
				for (var textField : BasicFX.getAllRecursively(view, TextField.class)) {
					Platform.runLater(() -> view.getChildren().remove(textField));
				}
			}

			if (view.getNodeSelection().size() > 0 || view.getEdgeSelection().size() > 0) {
				view.getNodeSelection().clearSelection();
				view.getEdgeSelection().clearSelection();
			}

			if (PhyloSketch.isDesktop() && view.getMode() == DrawPane.Mode.Edit && !inMultiTouchGesture.get()) {
				if (view.getGraph().getNumberOfNodes() == 0 || me.getClickCount() == 2) {
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					view.getUndoManager().doAndAdd(new AddNodeCommand(view, location));
				}
			}
			me.consume();
		});

		view.setOnMousePressed(me -> {
			inDrag.set(false);
			inLongPressCreateNode.set(false);
			mouseDownTime.set(Long.MAX_VALUE);

			if (view.getMode() == DrawPane.Mode.Edit && !inMultiTouchGesture.get()) {
				var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
				if (AddEdgeCommand.findNode(view, location) != null || AddEdgeCommand.findEdge(view, location) != null) {
					path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
					inDrag.set(true);
				} else if (!PhyloSketch.isDesktop()) {
					inLongPressCreateNode.set(true);
					if (view.getGraph().getNumberOfNodes() == 0) {
						mouseDownTime.set(0L); // definitely want to create a node
					} else
						mouseDownTime.set(System.currentTimeMillis());
				}
			}
			me.consume();
		});

		view.setOnMouseDragged(me -> {
			if (inDrag.get()) {
				view.setCursor(Cursor.CROSSHAIR);
				if (!path.getElements().isEmpty()) {
					if (!view.getEdgesGroup().getChildren().contains(path))
						view.getEdgesGroup().getChildren().add(path);
					view.getNodeSelection().clearSelection();
					view.getEdgeSelection().clearSelection();

					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					path.getElements().add(new LineTo(location.getX(), location.getY()));
				}
			}
			me.consume();
		});

		view.setOnMouseReleased(me -> {
			if (inLongPressCreateNode.get() && me.isStillSincePress() && System.currentTimeMillis() >= mouseDownTime.get() + 500) {
				var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
				view.getUndoManager().doAndAdd(new AddNodeCommand(view, location));
			} else if (inDrag.get()) {
				view.setCursor(Cursor.DEFAULT);
				view.getEdgesGroup().getChildren().remove(path);
				if (!path.getElements().isEmpty()) {
					if (isGoodPath(path)) {
						path.getElements().setAll(PathUtils.createPath(PathSmoother.apply(PathUtils.extractPoints(path), 10), true).getElements());
						view.getUndoManager().doAndAdd(new AddEdgeCommand(view, path));
					}
					path.getElements().clear();
				}
			}
		});


		view.setOnTouchPressed(e -> {
			inMultiTouchGesture.set(e.getTouchCount() > 1);
		});

		view.setOnTouchReleased(e -> {
			inMultiTouchGesture.set(e.getTouchCount() < 1);
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

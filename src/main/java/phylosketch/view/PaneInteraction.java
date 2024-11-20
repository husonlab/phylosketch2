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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import jloda.fx.util.BasicFX;
import jloda.fx.util.SelectionEffectBlue;
import phylosketch.commands.AddEdgeCommand;
import phylosketch.commands.AddNodeCommand;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;

import static phylosketch.paths.PathUtils.getCoordinates;

public class PaneInteraction {
	public static final BooleanProperty inDrawingEdge = new SimpleBooleanProperty(PaneInteraction.class, "inDrawingEdge", false);
	public static final BooleanProperty inMultiTouchGesture = new SimpleBooleanProperty(PaneInteraction.class, "inMultiTouchGesture", false);

	private final static Path path = new Path();

	static {
		path.getStyleClass().add("graph-edge");
	}

	/**
	 * setup the interaction
	 */
	public static void setup(DrawPane view, BooleanProperty allowResize) {
		if (false) { // for debugging zoom and pan interference
			var inMultiTouchLabel = new Label("multi-touch");
			var inDrawingEdgeLabel = new Label("drawing edge");

			inMultiTouchGesture.addListener((v, o, n) -> {
				if (n)
					view.getChildren().add(inMultiTouchLabel);
				else
					view.getChildren().remove(inMultiTouchLabel);
			});

			inDrawingEdge.addListener((v, o, n) -> {
				if (n)
					view.getChildren().add(inDrawingEdgeLabel);
				else
					view.getChildren().remove(inDrawingEdgeLabel);
			});
		}

		inMultiTouchGesture.addListener((v, o, n) -> {
			if (n)
				inDrawingEdge.set(false);
		});

		view.setOnContextMenuRequested(me -> {
			if (view.getMode() == DrawPane.Mode.Edit) {
				var createNodeMenuItem = new MenuItem("New Node");
				createNodeMenuItem.setOnAction(e -> {
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					view.getUndoManager().doAndAdd(new AddNodeCommand(view, location));
				});
				var menu = new ContextMenu();
				menu.getItems().add(createNodeMenuItem);
				menu.show(view, me.getScreenX(), me.getScreenY());
			}
			me.consume();
		});

		view.setOnMouseClicked(me -> {
			if (me.isStillSincePress() && me.getClickCount() == 1) {
				allowResize.set(false);
				for (var textField : BasicFX.getAllRecursively(view, TextField.class)) {
					Platform.runLater(() -> view.getChildren().remove(textField));
				}
			}

			if (me.isStillSincePress() && (view.getNodeSelection().size() > 0 || view.getEdgeSelection().size() > 0)) {
				view.getNodeSelection().clearSelection();
				view.getEdgeSelection().clearSelection();
			}

			if (view.getMode() == DrawPane.Mode.Edit && me.isStillSincePress() && !inMultiTouchGesture.get()) {
				if (view.getGraph().getNumberOfNodes() == 0 || me.getClickCount() == 2) {
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					view.getUndoManager().doAndAdd(new AddNodeCommand(view, location));
					if (false) {
						var shape = view.getShape(view.getGraph().getLastNode());
						shape.setEffect(SelectionEffectBlue.getInstance());
					}
				}
			}
			me.consume();
		});

		view.setOnMousePressed(me -> {
			inDrawingEdge.set(false);

			if (!inMultiTouchGesture.get() && view.getMode() == DrawPane.Mode.Edit) {
				path.getElements().clear();
				var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
				if (AddEdgeCommand.findNode(view, location) != null || AddEdgeCommand.findEdge(view, location) != null) {
					path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
					inDrawingEdge.set(true);

				}
			}
			me.consume();
		});

		view.setOnMouseDragged(me -> {
			if (inDrawingEdge.get()) {
				view.setCursor(Cursor.CROSSHAIR);
				if (!path.getElements().isEmpty()) {
					if (!view.getEdgesGroup().getChildren().contains(path))
						view.getEdgesGroup().getChildren().add(path);
					if (false) {
						view.getNodeSelection().clearSelection();
						view.getEdgeSelection().clearSelection();
					}
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					path.getElements().add(new LineTo(location.getX(), location.getY()));
				}
			}
			me.consume();
		});

		view.setOnMouseReleased(me -> {
			if (inDrawingEdge.get()) {
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
			me.consume();
		});

		view.setOnTouchPressed(e -> inMultiTouchGesture.set(e.getTouchCount() > 1));

		view.setOnTouchReleased(e -> inMultiTouchGesture.set(e.getTouchCount() < 1));
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

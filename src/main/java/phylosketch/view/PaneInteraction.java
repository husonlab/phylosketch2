/*
 * PaneInteraction.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import jloda.fx.util.BasicFX;
import jloda.graph.Edge;
import phylosketch.commands.CreateNodeCommand;
import phylosketch.commands.DrawEdgeCommand;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;
import phylosketch.window.MainWindowController;

import java.util.HashSet;
import java.util.stream.Collectors;

import static phylosketch.paths.PathUtils.getCoordinates;

public class PaneInteraction {
	private static double mouseX;
	private static double mouseY;
	private static double mouseDownX;
	private static double mouseDownY;

	public static final BooleanProperty inDrawingEdge = new SimpleBooleanProperty(PaneInteraction.class, "inDrawingEdge", false);
	public static final BooleanProperty inRubberBandSelection = new SimpleBooleanProperty(PaneInteraction.class, "inRubberBandSelection", false);
	public static final BooleanProperty inMultiTouchGesture = new SimpleBooleanProperty(PaneInteraction.class, "inMultiTouchGesture", false);

	private final static Path path = new Path();

	static {
		path.getStyleClass().add("graph-edge");
	}

	/**
	 * setup the interaction
	 */
	public static void setup(DrawView view, MainWindowController controller, DragLineBoxSupport dragLineBoxSupport, BooleanProperty allowResize) {
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
				if (n) {
					view.getChildren().add(inDrawingEdgeLabel);
				} else {
					view.getChildren().remove(inDrawingEdgeLabel);
				}
			});
		}


		inMultiTouchGesture.addListener((v, o, n) -> {
			if (n) {
				inDrawingEdge.set(false);
			}
		});

		/*
		var rubberBandSelection=new RubberBandSelection(view,(rectangle,extendSelection,executorService)->{
			if (PhyloSketch.isDesktop() && !extendSelection) {
				view.getNodeSelection().clearSelection();
				view.getEdgeSelection().clearSelection();
			}

			var nodes = view.getGraph().nodeStream().filter(v -> rectangle.contains(DrawView.getPoint(v))).collect(Collectors.toSet());
			nodes.forEach(v -> view.getNodeSelection().toggleSelection(v));

			var edges = new HashSet<Edge>();
			for (var e : view.getGraph().edges()) {
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget()) && DrawView.getPoints(e).stream().allMatch(rectangle::contains))
					edges.add(e);
			}
			edges.forEach(e -> view.getEdgeSelection().toggleSelection(e));

		});
		 */

		var hDragLine = dragLineBoxSupport.hDragLine();
		var vDragLine = dragLineBoxSupport.vDragLine();
		var box = dragLineBoxSupport.box();


		// will create a node if mouse is pressed and then not moved or released within two seconds
		var createNodePause = new PauseTransition(Duration.seconds(1.5));

		view.setOnMouseClicked(me -> {
			createNodePause.stop();

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

			if (view.getMode() == DrawView.Mode.Sketch && me.isStillSincePress() && !inMultiTouchGesture.get()) {
				if (me.getClickCount() == 2) {
					createNodePause.stop();
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					view.getUndoManager().doAndAdd(new CreateNodeCommand(view, location, null));
				}
			}
			me.consume();
		});

		createNodePause.setOnFinished(e -> {
			var location = view.screenToLocal(mouseX, mouseY);
			view.getUndoManager().doAndAdd(new CreateNodeCommand(view, location, null));
			path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
			view.setCursor(Cursor.CROSSHAIR);
			inDrawingEdge.set(true);
			inRubberBandSelection.set(false);
		});

		view.setOnMousePressed(me -> {
			inDrawingEdge.set(false);
			inRubberBandSelection.set(false);
			mouseX = mouseDownX = me.getScreenX();
			mouseY = mouseDownY = me.getScreenY();

			if (!inMultiTouchGesture.get()) {
				if (view.getMode() == DrawView.Mode.Sketch) {
					path.getElements().clear();
					var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
					if (DrawEdgeCommand.findNode(view, location) != null || DrawEdgeCommand.findEdge(view, location) != null) {
						path.getElements().setAll(new MoveTo(location.getX(), location.getY()));
						inDrawingEdge.set(true);
						view.setCursor(Cursor.CROSSHAIR);
					}
					if (!inDrawingEdge.get())
						createNodePause.playFromStart();
				}
				if (!inDrawingEdge.get() && !NodeInteraction.inMove) {
					inRubberBandSelection.set(true);
				}
			}
			me.consume();
		});

		view.setOnMouseDragged(me -> {
			createNodePause.stop();
			var location = view.sceneToLocal(me.getSceneX(), me.getSceneY());
			if (inDrawingEdge.get()) {
				if (!path.getElements().isEmpty()) {
					if (!view.getEdgesGroup().getChildren().contains(path))
						view.getEdgesGroup().getChildren().add(path);

					if (location.getX() >= box.getX() && location.getY() >= box.getY()) {
						path.getElements().add(new LineTo(location.getX(), location.getY()));
					}
					{
						var hasX = view.getGraph().nodeStream().mapToDouble(u -> DrawView.getPoint(u).getX()).anyMatch(x -> Math.abs(location.getX() - x) <= 1);
						var hasY = view.getGraph().nodeStream().mapToDouble(u -> DrawView.getPoint(u).getY()).anyMatch(y -> Math.abs(location.getY() - y) <= 1);
						if (hasX) {
							if (!view.getOtherGroup().getChildren().contains(hDragLine))
								view.getOtherGroup().getChildren().add(hDragLine);
						} else
							view.getOtherGroup().getChildren().remove(hDragLine);

						if (hasY) {
							if (!view.getOtherGroup().getChildren().contains(vDragLine))
								view.getOtherGroup().getChildren().add(vDragLine);
						} else
							view.getOtherGroup().getChildren().remove(vDragLine);
					}
				}
			} else if (inRubberBandSelection.get()) {
				var selectionRectangle = controller.getSelectionRectangle();
				if (!view.getOtherGroup().getChildren().contains(selectionRectangle)) {
					view.getOtherGroup().getChildren().add(selectionRectangle);
					selectionRectangle.applyCss();
				}
				var down = selectionRectangle.screenToLocal(mouseDownX, mouseDownY);
				var delta = location.subtract(down);

				selectionRectangle.setVisible(Math.abs(delta.getX()) > 5 || Math.abs(delta.getY()) > 5);
				selectionRectangle.setX(delta.getX() > 0 ? down.getX() : location.getX());
				selectionRectangle.setWidth(Math.abs(delta.getX()));
				selectionRectangle.setY(delta.getY() > 0 ? down.getY() : location.getY());
				selectionRectangle.setHeight(Math.abs(delta.getY()));
			}
			me.consume();
		});

		view.setOnMouseReleased(me -> {
			createNodePause.stop();
			view.setCursor(Cursor.DEFAULT);

			if (inDrawingEdge.get()) {
				view.getEdgesGroup().getChildren().remove(path);
				if (!path.getElements().isEmpty()) {
					if (isGoodPath(path)) {
						path.getElements().setAll(PathUtils.createPath(PathSmoother.apply(PathUtils.extractPoints(path), 10), true).getElements());
						view.getUndoManager().doAndAdd(new DrawEdgeCommand(view, path));
					}
					path.getElements().clear();
				}
			} else if (inRubberBandSelection.get()) {
				if (!me.isStillSincePress()) {
					if (PhyloSketch.isDesktop() && !me.isShiftDown()) {
						view.getNodeSelection().clearSelection();
						view.getEdgeSelection().clearSelection();
					}
					var bounds = controller.getSelectionRectangle().getLayoutBounds();

					var nodes = view.getGraph().nodeStream().filter(v -> bounds.contains(DrawView.getPoint(v))).collect(Collectors.toSet());
					nodes.forEach(v -> view.getNodeSelection().toggleSelection(v));

					var edges = new HashSet<Edge>();
					for (var e : view.getGraph().edges()) {
						if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget()) && DrawView.getPoints(e).stream().allMatch(bounds::contains))
							edges.add(e);
					}
					edges.forEach(e -> view.getEdgeSelection().toggleSelection(e));
				}
			}

			inRubberBandSelection.set(false);
			view.getOtherGroup().getChildren().remove(controller.getSelectionRectangle());
			view.getOtherGroup().getChildren().remove(hDragLine);
			view.getOtherGroup().getChildren().remove(vDragLine);
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

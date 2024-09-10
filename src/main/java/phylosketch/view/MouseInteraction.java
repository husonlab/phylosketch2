/*
 *  Copyright (C) 2018. Daniel H. Huson
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
 */

package phylosketch.view;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import jloda.fx.selection.SelectionModel;
import jloda.fx.util.GrayOutlineEffect;
import jloda.fx.util.RunAfterAWhile;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.ArrayList;

/**
 * mouse interaction
 * Daniel Huson, 9.2024
 */
public class MouseInteraction {
	public static final int INTERPOLATE_STEP=5;
	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	private static Path path;
	private static Point2D pathStart;
	private static Point2D pathEnd;

	private static Shape hitShape;
	private static Path hitPath;

	private static boolean inDragSelected;
	private static boolean wasDragged;

	private static final ArrayList<PathElement> internalElements = new ArrayList<>();

	/**
	 * setup mouse interaction
	 * @param view the draw pane
	 */
	public static void setup(DrawPane view) {
		var graph=view.getGraph();
		var nodesGroup=view.getNodesGroup();
		var edgesGroup=view.getEdgesGroup();
		var otherGroup=view.getOtherGroup();
		var tolerance=view.getTolerance();

		var nodeSelection = view.getNodeSelection();

		view.setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			internalElements.clear();
			path = null;

			hitPath = null;
			hitShape = null;
			inDragSelected = false;
			wasDragged = false;
			mouseDownX = e.getScreenX();
			mouseDownY = e.getScreenY();

			{
				var location = view.screenToLocal(mouseX, mouseY);

				var nodePointPair = DrawUtils.snapToExistingNode(location, nodesGroup, view.getTolerance());
				if (nodePointPair.getKey() != null && nodeSelection.isSelected(nodePointPair.getKey())) {
					inDragSelected = true;
				}
			}
			e.consume();
		});

		view.setOnMouseDragged(e -> {
			var previous = view.screenToLocal(mouseX, mouseY);

			if (inDragSelected) {
				var location = view.screenToLocal(e.getScreenX(), e.getScreenY());
				var delta = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
				if (!DrawUtils.hasCollisions(graph, nodeSelection.getSelectedItems(), delta.getX(), delta.getY())) {
					for (var v : nodeSelection.getSelectedItems()) {
						var shape = (Shape) v.getData();
						shape.setTranslateX(shape.getTranslateX() + delta.getX());
						shape.setTranslateY(shape.getTranslateY() + delta.getY());
					}
					MoveEdges.apply(graph, nodeSelection.getSelectedItems(), delta.getX(), delta.getY());
					mouseX = e.getScreenX();
					mouseY = e.getScreenY();
					wasDragged = true;
				}
				e.consume();
				return;
			}

			var nodePointPair = DrawUtils.snapToExistingNode(previous, nodesGroup, tolerance);

			if (nodePointPair.getKey() != null) {
				if (hitShape != null) {
					hitShape.setEffect(null);
				}
				hitShape = (Shape) nodePointPair.getKey().getData();
				hitShape.setEffect(GrayOutlineEffect.getInstance());
			} else {
				var paths = edgesGroup.getChildren().stream().filter(n -> n instanceof Path).map(n -> (Path) n).toList();
				var pointOnPath = PathUtils.pointOnPath(previous, paths, tolerance);
				if (pointOnPath != null) {
					if (hitPath != null)
						hitPath.setEffect(null);
					hitPath = pointOnPath.getFirst();
					hitPath.setEffect(GrayOutlineEffect.getInstance());
				}
			}

			if (path == null) {
				path = new Path();
				path.getStyleClass().add("graph-edge");

				previous = snapToExisting(previous, tolerance, nodesGroup, edgesGroup);
				pathStart = previous;

				path.getElements().add(new MoveTo(previous.getX(), previous.getY()));
			}
			var point = view.screenToLocal(e.getScreenX(), e.getScreenY());
			//point=snapToExisting(point,pane,path,tolerance.doubleValue());

			if (point.distance(previous) > tolerance) {
				var pathStarting=path.getElements().size()==1;
				var next=new LineTo(point.getX(), point.getY());
				var start=path.getElements().get(path.getElements().size()-1);
				path.getElements().addAll(DrawUtils.interpolate(start,next,INTERPOLATE_STEP));
				path.getElements().add(next);
				if (pathStarting) {
					// previous = snapToExisting(previous, tolerance.get());
					// var start = new Circle(previous.getX(), previous.getY(), 3);
					var end = new Circle(3);
					path.getElements().addListener((InvalidationListener) a -> {
						if (!path.getElements().isEmpty() && path.getElements().get(path.getElements().size() - 1) instanceof LineTo lineTo) {
							end.setCenterX(lineTo.getX());
							end.setCenterY(lineTo.getY());
						}
					});
					edgesGroup.getChildren().add(path);
				}
				mouseX = e.getScreenX();
				mouseY = e.getScreenY();
			}

			RunAfterAWhile.apply(path, () -> Platform.runLater(() -> {
				if (path != null) {
					var first = path.getElements().get(0);
					var last = path.getElements().get(path.getElements().size() - 1);
					var middle = DrawUtils.asRectangular(path);
					internalElements.clear();
					for (var element : path.getElements()) {
						if (element != first && element != last) {
							internalElements.add(element);
						}
					}
					if (middle != null) {
						var points=new ArrayList<PathElement>();
						points.add(first);
						points.addAll(DrawUtils.interpolate(first,middle,INTERPOLATE_STEP));
						points.add(middle);
						points.addAll(DrawUtils.interpolate(middle,last,INTERPOLATE_STEP));
						points.add(last);
						path.getElements().setAll(points);
					} else {
						var points=new ArrayList<PathElement>();
						points.add(first);
						points.addAll(DrawUtils.interpolate(first,last,INTERPOLATE_STEP));
						points.add(last);
						path.getElements().setAll(points);
					}
				}
			}), 1000);

			if (!internalElements.isEmpty()) {
				var first = (MoveTo) path.getElements().get(0);
				var last = (LineTo) path.getElements().get(path.getElements().size() - 1);
				path.getElements().setAll(first);
				path.getElements().addAll(internalElements);
				path.getElements().add(last);
				internalElements.clear();
			}
			e.consume();
		});

		view.setOnMouseReleased(e -> {
			if (wasDragged) {
				var mouseDown = view.screenToLocal(mouseDownX, mouseDownY);
				var location = view.screenToLocal(e.getScreenX(), e.getScreenY());
				var delta = new Point2D(location.getX() - mouseDown.getX(), location.getY() - mouseDown.getY());
				if (delta.magnitude() > 0) {
					view.getUndoManager().add("drag", () -> {
						for (var v : nodeSelection.getSelectedItems()) {
							var shape = (Shape) v.getData();
							shape.setTranslateX(shape.getTranslateX() - delta.getX());
							shape.setTranslateY(shape.getTranslateY() - delta.getY());
						}
						MoveEdges.apply(graph, nodeSelection.getSelectedItems(), -delta.getX(), -delta.getY());
					}, () -> {
						for (var v : nodeSelection.getSelectedItems()) {
							var shape = (Shape) v.getData();
							shape.setTranslateX(shape.getTranslateX() + delta.getX());
							shape.setTranslateY(shape.getTranslateY() + delta.getY());
						}
						MoveEdges.apply(graph, nodeSelection.getSelectedItems(), delta.getX(), delta.getY());
					});
				}
			}
			else if (path != null) {
				RunAfterAWhile.apply(path, null);

				if (!e.isStillSincePress()) {
					pathEnd = view.screenToLocal(e.getScreenX(), e.getScreenY());

					pathEnd = snapToExisting(pathEnd, tolerance, nodesGroup, edgesGroup);
					path.getElements().add(new LineTo(pathEnd.getX(), pathEnd.getY()));

					if (path != null && !path.getElements().isEmpty()) {
						if (pathStart.distance(pathEnd) >= tolerance) {
							view.getUndoManager().doAndAdd(new AddEdgeCommand(pathStart, pathEnd, path, view));
						} else edgesGroup.getChildren().remove(path);
					}

					if (false) {
						var paths = edgesGroup.getChildren().stream().filter(n -> n instanceof Path).map(n -> (Path) n).toList();
						var intersections = PathUtils.allIntersections(path, paths, false);
						for (var intersection : intersections) {
							var circle = new Circle(intersection.getSecond().getX(), intersection.getSecond().getY(), 3);
							circle.setFill(Color.TRANSPARENT);
							circle.setStroke(Color.RED);
							otherGroup.getChildren().add(circle);
						}
					}

					path = null;

					if (hitPath != null) {
						hitPath.setEffect(null);
						hitPath = null;
					}
					if (hitShape != null) {
						hitShape.setEffect(null);
						hitShape = null;
					}
				}
			}
			e.consume();
		});
	}

	private static Point2D snapToExisting(Point2D point, double tolerance, Group nodesGroup, Group edgesGroup) {
		var pair = DrawUtils.snapToExistingNode(point, nodesGroup, tolerance);
		if (pair.getKey() != null)
			return pair.getValue();
		else
			return DrawUtils.snapToExistingEdge(point, edgesGroup, tolerance).getValue();
	}
}

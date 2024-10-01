/*
 * NewEdgeCommmand.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.*;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.algorithms.IsDAG;
import jloda.util.Pair;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.List;

import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * creates a new edge from a given path
 * Daniel Huson, 9.2024
 */
public class NewEdgeCommmand {
	/**
	 * create a new edge
	 *
	 * @param view  the view
	 * @param path0 the path
	 */
	public static boolean doAndAdd(DrawPane view, Path path0) {
		final var graph = view.getGraph();
		final var undoManager = view.getUndoManager();

		var path = PathUtils.createPath(PathSmoother.apply(PathUtils.extractPoints(path0), 20), false);

		final var start = getCoordinates(path.getElements().get(0));
		final var startNode = findNode(view, start);
		final var startEdgeHit = findEdge(view, start);

		final var end = getCoordinates(path.getElements().get(path.getElements().size() - 1));
		final var endNode = findNode(view, end);
		final var endEdgeHit = findEdge(view, end);

		if (startNode != null && endNode != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startNode, endNode))))
			return false;
		if (startNode != null && endEdgeHit != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startNode, endEdgeHit.e().getSource()))))
			return false;
		if (startEdgeHit != null && endNode != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startEdgeHit.e.getSource(), endNode))))
			return false;
		if (startEdgeHit != null && endEdgeHit != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startEdgeHit.e.getSource(), endEdgeHit.e().getSource()))))
			return false;

		final int finalStartNodeId;
		final Path startPath;
		final Point2D finalStart;
		final Pair<Path, Path> startParts;
		final Integer oldStartEdgeId;
		final Integer oldStartEdgeSourceId;
		final Integer oldStartEdgeTargetId;
		final Integer startEdgeSourceId;
		final Integer startEdgeTargetId;
		final Integer newStartNodeId;

		if (startNode != null) { // start node already exists
			finalStartNodeId = startNode.getId();

			if (startNode.getData() instanceof Shape shape) {
				finalStart = new Point2D(shape.getTranslateX(), shape.getTranslateY());
			} else {
				finalStart = start;
			}

			{
				var diff = finalStart.subtract(start);
				if (diff.magnitude() > 1) {
					PathReshape.apply(path, 0, diff.getX(), diff.getY());
				}
			}

			newStartNodeId = null;
			startPath = null;
			startParts = null;
			oldStartEdgeId = null;
			oldStartEdgeSourceId = null;
			oldStartEdgeTargetId = null;
			startEdgeSourceId = null;
			startEdgeTargetId = null;
		} else if (startEdgeHit != null) { // splitting start edge
			oldStartEdgeId = startEdgeHit.e().getId();
			oldStartEdgeSourceId = startEdgeHit.e().getSource().getId();
			oldStartEdgeTargetId = startEdgeHit.e().getTarget().getId();
			var shape = new Circle(3);
			var t = view.createNode(shape);
			finalStartNodeId = t.getId();
			newStartNodeId = finalStartNodeId;
			startPath = startEdgeHit.path();
			startParts = startEdgeHit.splitPath();
			var splitPoint = getCoordinates(startParts.getSecond().getElements().get(0));
			var diff = splitPoint.subtract(start);
			if (diff.magnitude() > 1) {
				PathReshape.apply(path, 0, diff.getX(), diff.getY());
				finalStart = splitPoint;
			} else
				finalStart = start;
			shape.setTranslateX(finalStart.getX());
			shape.setTranslateY(finalStart.getY());

			var startEdge = startEdgeHit.e();
			view.createEdge(startEdge.getSource(), t, startParts.getFirst()).getId();
			startEdgeSourceId = startEdge.getSource().getId();
			view.createEdge(t, startEdge.getTarget(), startParts.getSecond()).getId();
			startEdgeTargetId = startEdge.getTarget().getId();
			view.deleteEdge(startEdge);
		} else { // creating new start node
			finalStart = start;
			var shape = new Circle(3);
			shape.setTranslateX(start.getX());
			shape.setTranslateY(start.getY());
			finalStartNodeId = view.createNode(shape).getId();
			newStartNodeId = finalStartNodeId;
			startPath = null;
			startParts = null;
			oldStartEdgeId = null;
			oldStartEdgeSourceId = null;
			oldStartEdgeTargetId = null;
			startEdgeSourceId = null;
			startEdgeTargetId = null;
		}

		final int finalEndNodeId;
		final Point2D finalEnd;
		final Path endPath;
		final Pair<Path, Path> endParts;
		final Integer oldEndEdgeId;
		final Integer oldEndEdgeSourceId;
		final Integer oldEndEdgeTargetId;
		final Integer endEdgeSourceId;
		final Integer endEdgeTargetId;
		final Integer newEndNodeId;

		if (endNode != null) {
			finalEndNodeId = endNode.getId();

			if (endNode.getData() instanceof Shape shape) {
				finalEnd = new Point2D(shape.getTranslateX(), shape.getTranslateY());
			} else {
				finalEnd = end;
			}

			{
				var diff = finalEnd.subtract(end);
				if (diff.magnitude() > 1) {
					PathReshape.apply(path, path.getElements().size() - 1, diff.getX(), diff.getY());
				}
			}
			newEndNodeId = null;
			endPath = null;
			endParts = null;
			oldEndEdgeId = null;
			oldEndEdgeSourceId = null;
			oldEndEdgeTargetId = null;
			endEdgeSourceId = null;
			endEdgeTargetId = null;
		} else if (endEdgeHit != null) {
			oldEndEdgeId = endEdgeHit.e().getId();
			oldEndEdgeSourceId = endEdgeHit.e().getSource().getId();
			oldEndEdgeTargetId = endEdgeHit.e().getTarget().getId();
			var shape = new Circle(3);
			var t = view.createNode(shape);

			finalEndNodeId = t.getId();
			newEndNodeId = finalEndNodeId;
			endPath = endEdgeHit.path();
			endParts = endEdgeHit.splitPath();

			var splitPoint = getCoordinates(endParts.getSecond().getElements().get(0));
			var diff = splitPoint.subtract(end);
			if (diff.magnitude() > 1) {
				PathReshape.apply(path, path.getElements().size() - 1, diff.getX(), diff.getY());
				finalEnd = splitPoint;
			} else {
				finalEnd = end;
			}
			shape.setTranslateX(finalEnd.getX());
			shape.setTranslateY(finalEnd.getY());

			var endEdge = endEdgeHit.e();
			view.createEdge(endEdge.getSource(), t, endParts.getFirst()).getId();
			endEdgeSourceId = endEdge.getSource().getId();
			view.createEdge(t, endEdge.getTarget(), endParts.getSecond()).getId();
			endEdgeTargetId = endEdge.getTarget().getId();
			view.deleteEdge(endEdge);
		} else {
			finalEnd = end;
			var shape = new Circle(3);
			shape.setTranslateX(end.getX());
			shape.setTranslateY(end.getY());
			finalEndNodeId = view.createNode(shape).getId();
			newEndNodeId = finalEndNodeId;
			endPath = null;
			endParts = null;
			oldEndEdgeId = null;
			oldEndEdgeSourceId = null;
			oldEndEdgeTargetId = null;
			endEdgeSourceId = null;
			endEdgeTargetId = null;
		}

		var newEdgeId = view.createEdge(graph.findNodeById(finalStartNodeId), graph.findNodeById(finalEndNodeId), path).getId();

		undoManager.add("create edge", () -> { // undo
					view.deleteEdge(graph.findEdgeById(newEdgeId));

					if (newStartNodeId != null) {
						view.deleteNode(graph.findNodeById(newStartNodeId));
					}
					if (oldStartEdgeId != null) {
						view.createEdge(graph.findNodeById(oldStartEdgeSourceId), graph.findNodeById(oldStartEdgeTargetId), startPath, oldStartEdgeId);
					}

					if (newEndNodeId != null) {
						view.deleteNode(graph.findNodeById(newEndNodeId));
					}
					if (oldEndEdgeId != null) {
						view.createEdge(graph.findNodeById(oldEndEdgeSourceId), graph.findNodeById(oldEndEdgeTargetId), endPath, oldEndEdgeId);
					}
				},
				() -> { // redo

					if (newStartNodeId != null) {
						var shape = new Circle(3);
						shape.setTranslateX(finalStart.getX());
						shape.setTranslateY(finalStart.getY());
						var a = view.createNode(shape, null, newStartNodeId);

						if (startEdgeSourceId != null) {
							view.createEdge(graph.findNodeById(startEdgeSourceId), a, startParts.getFirst());
						}
						if (startEdgeTargetId != null) {
							view.createEdge(a, graph.findNodeById(startEdgeTargetId), startParts.getSecond());
						}
					}
					if (oldStartEdgeId != null) {
						view.deleteEdge(graph.findEdgeById(oldStartEdgeId));
					}

					if (newEndNodeId != null) {
						var shape = new Circle(3);
						shape.setTranslateX(finalEnd.getX());
						shape.setTranslateY(finalEnd.getY());
						var a = view.createNode(shape, null, newEndNodeId);
						if (endEdgeSourceId != null) {
							view.createEdge(graph.findNodeById(endEdgeSourceId), a, endParts.getFirst());
						}
						if (endEdgeTargetId != null) {
							view.createEdge(a, graph.findNodeById(endEdgeTargetId), endParts.getSecond());
						}
					}
					if (oldEndEdgeId != null) {
						view.deleteEdge(graph.findEdgeById(oldEndEdgeId));
					}
					view.createEdge(graph.findNodeById(finalStartNodeId), graph.findNodeById(finalEndNodeId), path, newEdgeId);
				}
		);

		return true;
	}

	private static Node findNode(DrawPane view, Point2D local) {
		var bestDistance = 10.0;
		Node best = null;

		for (var node : view.getNodesGroup().getChildren()) {
			if (node instanceof Shape shape && shape.getUserData() instanceof Node v && v.getOwner() != null) {
				if (local.distance(shape.getTranslateX(), shape.getTranslateY()) < bestDistance) {
					bestDistance = local.distance(shape.getTranslateX(), shape.getTranslateY());
					best = v;
				}
			}
		}
		return best;
	}

	private static EdgeHit findEdge(DrawPane view, Point2D local) {
		var bestDistance = 10.0;
		Edge bestEdge = null;
		Path bestPath = null;
		int bestPathIndex = -1;

		for (var node : view.getEdgesGroup().getChildren()) {
			if (node instanceof Path path && path.getUserData() instanceof Edge e && e.getOwner() != null) {
				ObservableList<PathElement> elements = path.getElements();
				for (int i = 0; i < elements.size(); i++) {
					var element = elements.get(i);
					var coordinates = getCoordinates(element);
					if (coordinates.distance(local) < bestDistance) {
						bestDistance = coordinates.distance(local);
						bestEdge = e;
						bestPath = path;
						bestPathIndex = i;
					}
				}
			}
		}
		if (bestEdge != null) {
			return new EdgeHit(bestEdge, bestPath, bestPathIndex);
		} else
			return null;
	}

	public record EdgeHit(Edge e, Path path, int elementIndex) {

		public Pair<Path, Path> splitPath() {
			var part1 = new Path();
			part1.getStyleClass().add("graph-edge");
			for (var i = 0; i <= elementIndex; i++) {
				part1.getElements().add(path.getElements().get(i));
			}
			var part2 = new Path();
			part2.getStyleClass().add("graph-edge");
			var coords = getCoordinates(path.getElements().get(elementIndex));
			part2.getElements().add(new MoveTo(coords.getX(), coords.getY()));
			for (var i = elementIndex + 1; i < path.getElements().size(); i++) {
				part2.getElements().add(path.getElements().get(i));
			}
			return new Pair<>(part1, part2);
		}
	}
}


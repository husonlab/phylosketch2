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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.algorithms.IsDAG;
import jloda.util.Pair;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.List;

import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * creates a new edge from a given path
 * Daniel Huson, 9.2024
 */
@Deprecated
public class CreateEdgeCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final List<Point2D> points;

	private int startEdgeId = -1;
	private boolean startEdgeArrow = false;
	private int startEdgeSourceId = -1;
	private int startEdgeTargetId = -1;
	private Path startPath;

	private int newStartNodeId = -1;
	private int newStartEdgeSourcePartId = -1;
	private int newStartEdgeTargetPartId = -1;

	private int endEdgeId = -1;
	private boolean endEdgeArrow = false;
	private int endEdgeSourceId = -1;
	private int endEdgeTargetId = -1;
	private Path endPath;

	private int newEndNodeId = -1;
	private int newEndEdgeSourcePartId = -1;
	private int newEndEdgeTargetPartId = -1;
	private int newEdgeId = -1;

	/**
	 * constructor
	 *
	 * @param view  the view
	 * @param path0 the new path
	 */
	public CreateEdgeCommand(DrawPane view, Path path0) {
		super("create edge");

		this.points = PathUtils.extractPoints(path0);
		{
			var path = PathUtils.createPath(points, true);
			var start = getCoordinates(path.getElements().get(0));
			var startNode = findNode(view, start);
			var startEdgeHit = findEdge(view, start);

			var end = getCoordinates(path.getElements().get(path.getElements().size() - 1));
			var endNode = findNode(view, end);
			var endEdgeHit = findEdge(view, end);

			if (startNode != null && endNode != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startNode, endNode))))
				return;
			if (startNode != null && endEdgeHit != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startNode, endEdgeHit.e().getTarget()))))
				return;
			if (startEdgeHit != null && endNode != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startEdgeHit.e.getSource(), endNode))))
				return;
			if (startEdgeHit != null && endEdgeHit != null && startEdgeHit.e == endEdgeHit.e && startEdgeHit.elementIndex >= endEdgeHit.elementIndex)
				return; // back to earlier point in same edge
			if (startEdgeHit != null && endEdgeHit != null && !IsDAG.apply(view.getGraph(), List.of(new Pair<>(startEdgeHit.e.getSource(), endEdgeHit.e().getTarget()))))
				return;
		}

		undo = () -> {
			var graph = view.getGraph();
			if (newEdgeId != -1) {
				var f = graph.findEdgeById(newEdgeId);
				view.deleteEdge(f);
			}

			if (newStartNodeId != -1)
				view.deleteNode(graph.findNodeById(newStartNodeId));

			if (newEndNodeId != -1)
				view.deleteNode(graph.findNodeById(newEndNodeId));

			if (startEdgeId != -1) {
				var e = view.createEdge(graph.findNodeById(startEdgeSourceId), graph.findNodeById(startEdgeTargetId), startPath, startEdgeId);
				view.setShowArrow(e, startEdgeArrow);
			}

			if (endEdgeId != -1) {
				var e = view.createEdge(graph.findNodeById(endEdgeSourceId), graph.findNodeById(endEdgeTargetId), endPath, endEdgeId);
				view.setShowArrow(e, endEdgeArrow);
			}
		};

		redo = () -> { // redo
			// setup stuff:
			final var graph = view.getGraph();
			final var path = PathUtils.createPath(points, true);
			final Point2D startPoint;
			final boolean mustCreateStartNode;
			final boolean mustSplitStartEdge;
			final List<Path> startParts;
			int startNodeId = -1;

			final Point2D endPoint;
			final boolean mustCreateEndNode;
			final boolean mustSplitEndEdge;
			final List<Path> endParts;
			int endNodeId = -1;

			{
				final var start = getCoordinates(path.getElements().get(0));
				final var startNode = findNode(view, start);
				final var startEdgeHit = findEdge(view, start);

				final var end = getCoordinates(path.getElements().get(path.getElements().size() - 1));
				final var endNode = findNode(view, end);
				final var endEdgeHit = findEdge(view, end);

				if (startNode != null) { // start node already exists
					mustCreateStartNode = false;
					mustSplitStartEdge = false;
					startParts = null;
					startPoint = view.getPoint(startNode);
					startNodeId = startNode.getId();
				} else if (startEdgeHit != null) { // splitting start edge
					mustCreateStartNode = false;
					mustSplitStartEdge = true;

					var e = startEdgeHit.e();
					startEdgeId = e.getId();
					startEdgeArrow = view.getEdgeArrowMap().containsKey(e);
					startEdgeSourceId = e.getSource().getId();
					startEdgeTargetId = e.getTarget().getId();
					startPath = PathUtils.copy(startEdgeHit.path());
					startParts = PathUtils.split(startEdgeHit.path(), false, startEdgeHit.elementIndex());
					var splitPoint = getCoordinates(startParts.get(1).getElements().get(0));
					var diff = splitPoint.subtract(start);
					if (diff.magnitude() > 1) {
						PathReshape.apply(path, 0, diff.getX(), diff.getY());
						startPoint = splitPoint;
					} else
						startPoint = start;
				} else { // need a new start node
					mustCreateStartNode = true;
					mustSplitStartEdge = false;
					startParts = null;
					startPoint = PathUtils.getCoordinates(path.getElements().get(0));
				}

				if (endNode != null) { // end node already exists
					mustCreateEndNode = false;
					mustSplitEndEdge = false;
					endParts = null;
					endPoint = view.getPoint(endNode);
					endNodeId = endNode.getId();
				} else if (endEdgeHit != null) { // splitting end edge
					mustCreateEndNode = false;
					mustSplitEndEdge = true;
					var e = endEdgeHit.e();
					endEdgeId = e.getId();
					endEdgeArrow = view.getEdgeArrowMap().containsKey(e);
					endEdgeSourceId = e.getSource().getId();
					endEdgeTargetId = e.getTarget().getId();
					endPath = PathUtils.copy(endEdgeHit.path());
					endParts = PathUtils.split(endEdgeHit.path(), false, endEdgeHit.elementIndex());
					var splitPoint = getCoordinates(endParts.get(1).getElements().get(0));
					var diff = splitPoint.subtract(end);
					if (diff.magnitude() > 1) {
						PathReshape.apply(path, 0, diff.getX(), diff.getY());
						endPoint = splitPoint;
					} else
						endPoint = end;
				} else {
					mustCreateEndNode = true;
					mustSplitEndEdge = false;
					endParts = null;
					endPoint = PathUtils.getCoordinates(path.getElements().get(path.getElements().size() - 1));
				}
			}

			var redoArrows = new ArrayList<Edge>();

			Node s; // start node for new edge
			if (mustSplitStartEdge) {
				{
					s = view.createNode(startPoint, newStartNodeId);
					newStartNodeId = s.getId();
				}

				{
					var v = graph.findNodeById(startEdgeSourceId);
					var e = view.createEdge(v, s, startParts.get(0), newStartEdgeSourcePartId);
					newStartEdgeSourcePartId = e.getId();
					if (startEdgeArrow) {
						redoArrows.add(e);
					}
				}
				if (startEdgeId != endEdgeId) {
					var w = graph.findNodeById(startEdgeTargetId);
					var e = view.createEdge(s, w, startParts.get(1), newStartEdgeTargetPartId);
					newStartEdgeTargetPartId = e.getId();
					if (startEdgeArrow)
						redoArrows.add(e);
				}
			} else if (mustCreateStartNode) {
				s = view.createNode(startPoint, newStartNodeId);
				newStartNodeId = s.getId();
			} else {
				s = graph.findNodeById(startNodeId);
			}

			Node t; // end node for new edge
			if (mustSplitEndEdge) {
				{
					t = view.createNode(endPoint, newEndNodeId);
					newEndNodeId = t.getId();
				}
				if (startEdgeId != endEdgeId) {
					var v = graph.findNodeById(endEdgeSourceId);
					var e = view.createEdge(v, t, endParts.get(0), newEndEdgeSourcePartId);
					newEndEdgeSourcePartId = e.getId();
					if (endEdgeArrow)
						redoArrows.add(e);
				}
				{
					var w = graph.findNodeById(endEdgeTargetId);
					var e = view.createEdge(t, w, endParts.get(1), newEndEdgeTargetPartId);
					newEndEdgeTargetPartId = e.getId();
					if (endEdgeArrow)
						redoArrows.add(e);
				}
			} else if (mustCreateEndNode) {
				t = view.createNode(endPoint, newEndNodeId);
				newEndNodeId = t.getId();
			} else {
				t = graph.findNodeById(endNodeId);
			}

			if (mustSplitStartEdge && mustSplitEndEdge && startEdgeId == endEdgeId) {
				var edgePath = view.getPath(graph.findEdgeById(startEdgeId));
				var parts = PathUtils.split(edgePath, startPoint, endPoint);
				var p = graph.findNodeById(newStartNodeId);
				var q = graph.findNodeById(newEndNodeId);
				var e = view.createEdge(p, q, parts.get(1), newEndEdgeSourcePartId);
				newEndEdgeSourcePartId = e.getId();
				if (endEdgeArrow)
					redoArrows.add(e);
			}

			Edge newEdge; // new edge
			{
				extendPathIfNecessary(startPoint, path, endPoint);
				if (s.getInDegree() > 0 && s.getOutDegree() == 0 && view.getShape(s) instanceof Circle circle) {
					circle.setRadius(circle.getRadius() / 2);
				}
				if (t.getInDegree() == 0 && t.getOutDegree() > 0 && view.getShape(t) instanceof Circle circle) {
					circle.setRadius(circle.getRadius() / 2);
				}
				newEdge = view.createEdge(s, t, path, newEdgeId);

				newEdgeId = newEdge.getId();
			}

			redoArrows.add(newEdge);

			{
				Platform.runLater(() -> {
					for (var f : redoArrows) {
						view.setShowArrow(f, true);
					}
				});
			}

			if (mustSplitStartEdge) {
				view.deleteEdge(graph.findEdgeById(startEdgeId));
			}
			if (mustSplitEndEdge && startEdgeId != endEdgeId) {
				view.deleteEdge(graph.findEdgeById(endEdgeId));
			}
		};
	}

	private static void extendPathIfNecessary(Point2D startPoint, Path path, Point2D endPoint) {
		var pathPoints = PathUtils.extractPoints(path);
		var extendStart = (pathPoints.get(0).distance(startPoint) > 1);
		var endEnd = (pathPoints.get(pathPoints.size() - 1).distance(endPoint) > 1);
		if (extendStart || endEnd) {
			var points = new ArrayList<Point2D>();
			if (extendStart)
				points.add(startPoint);
			points.addAll(pathPoints);
			if (endEnd)
				points.add(endPoint);
			path.getElements().setAll(PathUtils.createPath(points, true).getElements());
		}
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	public static Node findNode(DrawPane view, Point2D local) {
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

	public static EdgeHit findEdge(DrawPane view, Point2D local) {
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
	}
}


/*
 * DrawEdgeCommand.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.algorithms.IsDAG;
import jloda.util.Pair;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.List;

import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * the draw edge command
 * Daniel Huson, 12.2024
 */
public class DrawEdgeCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private int startNodeId = -1;

	private CreateNodeCommand newSourceNodeCommand;
	private InsertNodeInEdgeCommand insertSourceNodeCommand;

	private int endNodeId = -1;
	private CreateNodeCommand newTargetNodeCommand;
	private InsertNodeInEdgeCommand insertTargetNodeCommand;

	private InsertTwoNodesInEdgeCommand insertTwoNodesInEdgeCommand;

	private NewEdgeCommand newEdgeCommand;

	/**
	 * constructor
	 *
	 * @param view  the window
	 * @param path0 the drawn path
	 */
	public DrawEdgeCommand(DrawPane view, Path path0) {
		super("draw edge");

		var points = PathUtils.extractPoints(path0);
		var startPoint = points.get(0);
		var endPoint = points.get(points.size() - 1);

		if (startPoint.distance(endPoint) <= 5)
			return; // too short

		var startNode = findNode(view, startPoint);
		var startEdgeHit = (startNode == null ? findEdge(view, startPoint) : null);

		var endNode = findNode(view, endPoint);
		var endEdgeHit = (endNode == null ? findEdge(view, endPoint) : null);

		{
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

		var sameStartAndEndEdge = (startEdgeHit != null && endEdgeHit != null && startEdgeHit.e() == endEdgeHit.e());

		if (startNode != null) {
			startNodeId = startNode.getId();
			startPoint = view.getPoint(startNode);
		} else if (startEdgeHit != null) {
			startPoint = PathUtils.nudgeOntoPath(startEdgeHit.path(), startPoint);
			if (!sameStartAndEndEdge) {
				insertSourceNodeCommand = new InsertNodeInEdgeCommand(view, startEdgeHit.e(), startPoint);
			}
		} else {
			newSourceNodeCommand = new CreateNodeCommand(view, startPoint);
		}

		if (endNode != null) {
			endNodeId = endNode.getId();
			endPoint = view.getPoint(endNode);
		} else if (endEdgeHit != null) {
			endPoint = PathUtils.nudgeOntoPath(endEdgeHit.path(), endPoint);
			if (!sameStartAndEndEdge) {
				insertTargetNodeCommand = new InsertNodeInEdgeCommand(view, endEdgeHit.e(), endPoint);
			}
		} else {
			newTargetNodeCommand = new CreateNodeCommand(view, endPoint);
		}

		if (sameStartAndEndEdge) {
			insertTwoNodesInEdgeCommand = new InsertTwoNodesInEdgeCommand(view, startEdgeHit.e(), startPoint, endPoint);
		}

		var adjustedPoints = new ArrayList<Point2D>();
		adjustedPoints.add(startPoint);
		adjustedPoints.addAll(points);
		adjustedPoints.add(endPoint);
		var smoothedPoints = PathSmoother.apply(adjustedPoints, 10);
		var path = PathUtils.createPath(smoothedPoints, true);


		undo = () -> {
			if (newSourceNodeCommand != null) {
				newSourceNodeCommand.undo();
			}
			if (insertSourceNodeCommand != null) {
				insertSourceNodeCommand.undo();
			}
			if (insertTargetNodeCommand != null) {
				insertTargetNodeCommand.undo();
			}
			if (insertTwoNodesInEdgeCommand != null) {
				insertTwoNodesInEdgeCommand.undo();
			}
			if (newTargetNodeCommand != null) {
				newTargetNodeCommand.undo();
			}
			if (newEdgeCommand != null) {
				newEdgeCommand.undo();
			}
		};

		redo = () -> {
			int sourceId;
			if (startNodeId != -1) {
				sourceId = startNodeId;
			} else if (newSourceNodeCommand != null) {
				newSourceNodeCommand.redo();
				sourceId = newSourceNodeCommand.getNewNodeId();
			} else if (insertSourceNodeCommand != null) {
				insertSourceNodeCommand.redo();
				sourceId = insertSourceNodeCommand.getNodeId(); // always the same value
			} else {
				insertTwoNodesInEdgeCommand.redo();
				sourceId = insertTwoNodesInEdgeCommand.getNode1Id(); // always the same value
			}
			int targetId;
			if (endNodeId != -1) {
				targetId = endNodeId;
			} else if (insertTargetNodeCommand != null) {
				insertTargetNodeCommand.redo();
				targetId = insertTargetNodeCommand.getNodeId(); // always the same value
			} else if (insertTwoNodesInEdgeCommand != null) {
				targetId = insertTwoNodesInEdgeCommand.getNode2Id(); // always the same value
			} else {
				newTargetNodeCommand.redo();
				targetId = newTargetNodeCommand.getNewNodeId(); // always the same value
			}

			if (newEdgeCommand == null) {
				var v = view.getGraph().findNodeById(sourceId);
				var w = view.getGraph().findNodeById(targetId);
				newEdgeCommand = new NewEdgeCommand(view, v, w, path);
			}
			newEdgeCommand.redo();
		};
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

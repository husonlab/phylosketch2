/*
 * LayoutPhylogenyCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.geometry.Point2D;
import jloda.fx.phylo.embed.Averaging;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.AService;
import jloda.fx.window.NotificationManager;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.Pair;
import phylosketch.draw.DrawNetwork;
import phylosketch.paths.PathUtils;
import phylosketch.utils.ScaleUtils;
import phylosketch.view.DrawView;

import java.util.*;

/**
 * layout network
 * Daniel Huson,m 3.2024
 */
public class LayoutPhylogenyCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> oldPoints = new HashMap<>();
	private final Map<Integer, List<Point2D>> oldPaths = new HashMap<>();
	private final Map<Integer, Point2D> oldLabelPoints = new HashMap<>();
	private final Map<Integer, Double> oldLabelAngles = new HashMap<>();


	/**
	 * layout all selected (or if none selected, all) components
	 *
	 * @param view the view
	 */
	public LayoutPhylogenyCommand(DrawView view, LayoutRootedPhylogeny.Layout layout, LayoutRootedPhylogeny.Scaling scaling) {
		super("layout");
		var command = new CompositeCommand("layout");
		var nodes = view.getSelectedOrAllNodes();
		for (var component : ConnectedComponents.components(view.getGraph())) {
			if (CollectionUtils.intersects(component, nodes)) {
				var reticulateEdges = new ArrayList<Edge>();
				for (var v : nodes) {
					reticulateEdges.addAll(v.outEdgesStream(false).filter(e -> component.contains(e.getTarget()) && view.getGraph().isReticulateEdge(e) && !view.getGraph().isTransferAcceptorEdge(e)).toList());
				}
				command.add(new LayoutPhylogenyCommand(view, component, layout, scaling));
				if (!reticulateEdges.isEmpty()) {
					if (false)
						command.add(new QuadraticCurveCommand(view, reticulateEdges));
				}
				command.add(new LayoutLabelsCommand(view, null, component));
			}
		}
		if (command.isUndoable() && command.isRedoable()) {
			undo = command::undo;
			redo = command::redo;
		} else {
			undo = null;
			redo = null;
		}
	}

	/**
	 * layout of a single component
	 *
	 * @param view      the view
	 * @param component the nodes of the component
	 */
	private LayoutPhylogenyCommand(DrawView view, Collection<Node> component, LayoutRootedPhylogeny.Layout layout, LayoutRootedPhylogeny.Scaling scaling) {
		super("layout");

		if (isRootedComponent(component)) {

			for (var v : component) {
				oldPoints.put(v.getId(), DrawView.getPoint(v));
				for (var e : v.outEdges()) {
					oldPaths.put(e.getId(), PathUtils.extractPoints(DrawView.getPath(e)));
				}
				var label = DrawView.getLabel(v);
				if (label != null) {
					oldLabelPoints.put(v.getId(), new Point2D(label.getLayoutX(), label.getLayoutY()));
					oldLabelAngles.put(v.getId(), label.getRotate());
				}
			}

			undo = () -> {
				for (var vId : oldPoints.keySet()) {
					var v = view.getGraph().findNodeById(vId);
					view.setLocation(v, oldPoints.get(vId));
					var label = DrawView.getLabel(v);
					if (label != null) {
						label.setRotate(oldLabelAngles.get(vId));
						label.setLayoutX(oldLabelPoints.get(vId).getX());
						label.setLayoutY(oldLabelPoints.get(vId).getY());
						label.ensureUpright();
					}
				}
				for (var eId : oldPaths.keySet()) {
					var e = view.getGraph().findEdgeById(eId);
					DrawView.setPoints(e, oldPaths.get(eId));
				}
			};

			redo = () -> {
				var graph = view.getGraph();
				var xMin = component.stream().mapToDouble(DrawView::getX).min().orElse(0);
				var xMax = component.stream().mapToDouble(DrawView::getX).max().orElse(0);
				var yMin = component.stream().mapToDouble(DrawView::getY).min().orElse(0);
				var yMax = component.stream().mapToDouble(DrawView::getY).max().orElse(0);

				if (layout != LayoutRootedPhylogeny.Layout.Rectangular) {
					var dx = xMax - xMin;
					var dy = yMax - yMin;
					var d = Math.min(dx, dy);
					var xGap = xMax - xMin - d;
					var yGap = yMax - yMin - d;
					xMin += 0.5 * xGap;
					xMax -= 0.5 * xGap;
					yMin += 0.5 * yGap;
					yMax -= 0.5 * yGap;
				}

				var tree = new PhyloTree();
				try (NodeArray<Node> tree2GraphNodeMap = tree.newNodeArray(); EdgeArray<Edge> tree2GraphEdgeMap = tree.newEdgeArray()) {
					try (NodeArray<Node> graph2TreeNode = graph.newNodeArray(); EdgeArray<Edge> graph2TreeEdge = graph.newEdgeArray()) {
						tree.copy(graph, graph2TreeNode, graph2TreeEdge);
						graph.nodeStream().filter(u -> !component.contains(u)).map(graph2TreeNode::get).forEach(tree::deleteNode);
						for (var v : component) {
							tree2GraphNodeMap.put(graph2TreeNode.get(v), v);
							for (var e : v.outEdges()) {
								var f = graph2TreeEdge.get(e);
								tree2GraphEdgeMap.put(f, e);
								if ((graph.hasReticulateEdges() && graph.isReticulateEdge(e)) || e.getTarget().getInDegree() > 1) {
									tree.setReticulate(f, true);
								}
								if (graph.hasTransferAcceptorEdges() && graph.isTransferAcceptorEdge(e)) {
									tree.setTransferAcceptor(f, true);
								}
							}
						}
					}
					var root = tree.nodeStream().filter(v -> v.getInDegree() == 0).findAny().orElse(null);
					if (root != null) {
						tree.setRoot(root);
						var xMinF = xMin;
						var xMaxF = xMax;
						var yMinF = yMin;
						var yMaxF = yMax;

						AService.run(() -> {
									var nodeAngleMap = tree.newNodeDoubleArray();
									NodeArray<Point2D> nodePointMap = tree.newNodeArray();
									LayoutRootedPhylogeny.apply(tree, layout, scaling, Averaging.LeafAverage, true, new Random(666), nodeAngleMap, nodePointMap);
									ScaleUtils.scaleToBox(nodePointMap, xMinF, xMaxF, yMinF, yMaxF);
									return new Pair<>(nodeAngleMap, nodePointMap);
								},
								p -> {
									DrawNetwork.apply(view, tree, tree2GraphNodeMap, tree2GraphEdgeMap, p.getSecond(), layout);
									p.getFirst().close();
									p.getSecond().close();
								},
								e -> NotificationManager.showError("Layout failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
					}
				}
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	private boolean isRootedComponent(Collection<Node> component) {
		var seen = new HashSet<Node>();
		var stack = new Stack<Node>();
		var v = component.iterator().next();
		stack.push(v);
		seen.add(v);
		Node root = null;
		while (!stack.isEmpty()) {
			v = stack.pop();
			if (v.getInDegree() == 0) {
				if (root == null) {
					root = v;
				} else {
					return false;
				}
			}
			for (var w : v.adjacentNodes()) {
				if (!seen.contains(w)) {
					stack.push(w);
					seen.add(w);
					if (seen.size() > component.size())
						return false;
				}
			}
		}
		return component.size() == seen.size();
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
}

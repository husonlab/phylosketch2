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
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CollectionUtils;
import phylosketch.draw.DrawNetwork;
import phylosketch.embed.CircularPhylogenyLayout;
import phylosketch.embed.HeightAndAngles;
import phylosketch.embed.RectangularPhylogenyLayout;
import phylosketch.io.ReorderChildren;
import phylosketch.main.PhyloSketch;
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

	/**
	 * layout all selected (or if none selected, all) components
	 *
	 * @param view    the view
	 */
	public LayoutPhylogenyCommand(DrawView view) {
		super("layout");
		var command = new CompositeCommand("layout");
		var nodes = view.getSelectedOrAllNodes();
		for (var component : ConnectedComponents.components(view.getGraph())) {
			if (CollectionUtils.intersects(component, nodes)) {
				command.add(new LayoutPhylogenyCommand(view, component));
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
	private LayoutPhylogenyCommand(DrawView view, Collection<Node> component) {
		super("layout");

		if (isRootedComponent(component)) {

			var oldPoints = new HashMap<Integer, Point2D>();
			var oldPaths = new HashMap<Integer, List<Point2D>>();

			for (var v : component) {
				oldPoints.put(v.getId(), DrawView.getPoint(v));
				for (var e : v.outEdges()) {
					oldPaths.put(e.getId(), PathUtils.extractPoints(DrawView.getPath(e)));
				}
			}

			var newPoints = new HashMap<Integer, Point2D>();
			var newPaths = new HashMap<Integer, List<Point2D>>();

			undo = () -> {
				for (var vId : oldPoints.keySet()) {
					var v = view.getGraph().findNodeById(vId);
					view.setLocation(v, oldPoints.get(vId));
				}
				for (var eId : oldPaths.keySet()) {
					var e = view.getGraph().findEdgeById(eId);
					DrawView.setPoints(e, oldPaths.get(eId));
				}
			};

			redo = () -> {
				if (newPoints.isEmpty()) {
					var graph = view.getGraph();
					var xMin = component.stream().mapToDouble(DrawView::getX).min().orElse(0);
					var xMax = component.stream().mapToDouble(DrawView::getX).max().orElse(0);
					var yMin = component.stream().mapToDouble(DrawView::getY).min().orElse(0);
					var yMax = component.stream().mapToDouble(DrawView::getY).max().orElse(0);

					System.err.println("Graph has weights: " + view.getGraph().hasEdgeWeights());

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
							ReorderChildren.apply(tree, v -> DrawView.getPoint(tree2GraphNodeMap.get(v)), ReorderChildren.SortBy.Location);
							LSAUtils.setLSAChildrenAndTransfersMap(tree);
							try (NodeArray<Point2D> points = tree.newNodeArray()) {

								System.err.println("Tree has weights: " + tree.hasEdgeWeights());

								var circular = true;

								if (circular)
									CircularPhylogenyLayout.apply(tree, tree.hasEdgeWeights(), HeightAndAngles.Averaging.ChildAverage, PhyloSketch.test, points);
								else
									RectangularPhylogenyLayout.apply(tree, tree.hasEdgeWeights(), HeightAndAngles.Averaging.ChildAverage, PhyloSketch.test, points);
								ScaleUtils.scaleToBox(points, xMin, xMax, yMin, yMax);
								DrawNetwork.apply(view, tree, tree2GraphNodeMap, tree2GraphEdgeMap, points, circular);
								for (var v : tree.nodes()) {
									if (tree2GraphNodeMap.containsKey(v)) {
										var w = tree2GraphNodeMap.get(v);
										newPoints.put(w.getId(), DrawView.getPoint(w));
										for (var e : w.outEdges()) {
											newPaths.put(e.getId(), PathUtils.extractPoints(DrawView.getPath(e)));
										}
									}
								}
							} catch (Exception ex) {
								Basic.caught(ex);
							}
						}
					}
				} else {
					for (var vId : newPoints.keySet()) {
						var v = view.getGraph().findNodeById(vId);
						view.setLocation(v, newPoints.get(vId));
					}
					for (var eId : newPaths.keySet()) {
						var e = view.getGraph().findEdgeById(eId);
						DrawView.setPoints(e, newPaths.get(eId));
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

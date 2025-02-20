/*
 * OrderingGraph.java Copyright (C) 2025 Daniel H. Huson
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

package xtra;

import javafx.geometry.Point2D;
import jloda.graph.*;

import java.util.*;

import static jloda.graph.DAGTraversals.postOrderTraversal;

/**
 * the ordering graph
 * Daniel Huson, 2.2025
 */
public class OrderingGraph {
	private final Graph graph = new Graph();
	private final Map<Node, Node> phyloNodeToLayoutNodeMap = new HashMap<>();
	private final List<Node> layoutNodes = new ArrayList<>();
	private final EdgeDoubleArray edgeWeightMap = graph.newEdgeDoubleArray();
	private final NodeIntArray nodeHeightMap = graph.newNodeIntArray();

	/**
	 * computes the children ordering graph for a node v
	 *
	 * @param v           the current node
	 * @param lsaChildren the node to LSA children map
	 */
	public OrderingGraph(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var nodesBelowMap = new HashMap<Node, Collection<Node>>();
		for (var w : lsaChildren.get(v)) {
			var nodesBelow = nodesBelowMap.computeIfAbsent(w, k -> new HashSet<>());
			postOrderTraversal(w, lsaChildren::get, nodesBelow::add);
		}

		// setup layout graph:
		var aboveLayoutNode = graph.newNode();
		nodeHeightMap.put(aboveLayoutNode, 1);
		layoutNodes.add(aboveLayoutNode);

		for (var w : lsaChildren.get(v)) {
			var layoutNode = graph.newNode();
			layoutNodes.add(layoutNode);
			layoutNode.setData(w);
			nodeHeightMap.put(layoutNode, (int) nodesBelowMap.get(w).stream().filter(Node::isLeaf).count());

			phyloNodeToLayoutNodeMap.put(w, layoutNode);

			layoutNode.setInfo(w.getInfo());
			var nodesBelow = nodesBelowMap.get(w);
			for (var u : nodesBelow) {
				phyloNodeToLayoutNodeMap.put(u, layoutNode);
			}
		}

		var belowLayoutNode = graph.newNode();
		nodeHeightMap.put(belowLayoutNode, 1);
		layoutNodes.add(belowLayoutNode);

		var min = Double.MAX_VALUE;
		for (var w : nodesBelowMap.keySet()) {
			var nodesBelowW = nodesBelowMap.get(w);
			for (var p : nodesBelowW) {
				for (var f : p.outEdgesStream(false).filter(f -> f.getTarget().getInDegree() >= 2).toList()) {
					var q = f.getTarget();
					if (!nodesBelowW.contains(q)) {
						var dy = Math.abs(points.get(p).getY() - points.get(q).getY());
						min = Math.min(min, dy);
					}
				}
			}
		}

		for (var w : nodesBelowMap.keySet()) {
			var nodesBelowW = nodesBelowMap.get(w);
			for (var p : nodesBelowW) {
				var pLayoutNode = phyloNodeToLayoutNodeMap.get(p);
				for (var f : p.outEdgesStream(false).filter(f -> f.getTarget().getInDegree() >= 2).toList()) {
					var q = f.getTarget();
					Node qLayoutNode;

					if (!nodesBelowW.contains(q)) {
						var yP = points.get(p).getY();
						var yQ = points.get(q).getY();
						var up = (yP >= yQ);
						qLayoutNode = phyloNodeToLayoutNodeMap.getOrDefault(q, (up ? aboveLayoutNode : belowLayoutNode));

						var dy = Math.abs(points.get(p).getY() - points.get(q).getY()) / min;
						if (!pLayoutNode.isAdjacent(qLayoutNode)) {
							var layoutEdge = graph.newEdge(pLayoutNode, qLayoutNode);
							edgeWeightMap.put(layoutEdge, dy);
						} else {
							var layoutEdge = pLayoutNode.getCommonEdge(qLayoutNode);
							edgeWeightMap.put(layoutEdge, edgeWeightMap.get(layoutEdge) + dy);
						}
					}
				}
			}
		}
	}

	public Graph getGraph() {
		return graph;
	}

	public Map<Node, Node> getPhyloNodeToLayoutNodeMap() {
		return phyloNodeToLayoutNodeMap;
	}

	public List<Node> getLayoutNodes() {
		return layoutNodes;
	}

	public double getEdgeWeight(Edge e) {
		return edgeWeightMap.getOrDefault(e, 0.0);
	}

	public int getNodeHeight(Node v) {
		return nodeHeightMap.getOrDefault(v, 0);
	}

	public Node getPhylogenyNode(Node on) {
		return (Node) on.getData();
	}
}

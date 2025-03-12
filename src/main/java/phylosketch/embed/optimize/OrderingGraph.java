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

package phylosketch.embed.optimize;

import javafx.geometry.Point2D;
import jloda.graph.*;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.PhyloTree;

import java.util.*;
import java.util.function.Function;

import static jloda.graph.DAGTraversals.postOrderTraversal;

/**
 * the ordering graph
 * Daniel Huson, 2.2025
 */
public class OrderingGraph {
	private final Graph graph = new Graph();
	private final Map<Node, Node> phyloNodeToOrderingNodeMap = new HashMap<>();
	private final List<Node> orderingNodes = new ArrayList<>();
	private final EdgeDoubleArray edgeWeightMap = graph.newEdgeDoubleArray();
	private final NodeIntArray nodeHeightMap = graph.newNodeIntArray();

	/**
	 * computes the children ordering graph for a node v
	 *
	 * @param v           the current node
	 * @param lsaChildren the node to LSA children map
	 * @param points0 the points map or null
	 */
	public OrderingGraph(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points0) {
		Function<Node, Point2D> points = (points0 != null ? points0::get : u -> new Point2D(0, 0));

		var tree = (PhyloTree) v.getOwner();

		var nodesBelowMap = new HashMap<Node, Collection<Node>>();
		for (var w : lsaChildren.get(v)) {
			var nodesBelow = nodesBelowMap.computeIfAbsent(w, k -> new HashSet<>());
			postOrderTraversal(w, lsaChildren::get, nodesBelow::add);
		}

		// setup layout graph:
		var aboveLayoutNode = graph.newNode();
		nodeHeightMap.put(aboveLayoutNode, 1);
		orderingNodes.add(aboveLayoutNode);

		for (var w : lsaChildren.get(v)) {
			var orderingNodes = graph.newNode();
			this.orderingNodes.add(orderingNodes);
			orderingNodes.setData(w);
			nodeHeightMap.put(orderingNodes, (int) nodesBelowMap.get(w).stream().filter(Node::isLeaf).count());

			phyloNodeToOrderingNodeMap.put(w, orderingNodes);

			orderingNodes.setInfo(w.getInfo());
			var nodesBelow = nodesBelowMap.get(w);
			for (var u : nodesBelow) {
				phyloNodeToOrderingNodeMap.put(u, orderingNodes);
			}
		}

		var belowLayoutNode = graph.newNode();
		nodeHeightMap.put(belowLayoutNode, 1);
		orderingNodes.add(belowLayoutNode);

		for (var w : nodesBelowMap.keySet()) {
			var nodesBelowW = nodesBelowMap.get(w);
			for (var p : nodesBelowW) {
				var pLayoutNode = phyloNodeToOrderingNodeMap.get(p);
				for (var f : p.adjacentEdgesStream(false).filter(f -> !f.nodes().contains(v) && tree.isReticulateEdge(f) && !tree.isTransferAcceptorEdge(f)).toList()) {
					var q = f.getOpposite(p);

					Node qLayoutNode;

					if (!nodesBelowW.contains(q)) {
						var yP = points.apply(p).getY();
						var yQ = points.apply(q).getY();
						var up = (yP >= yQ);
						qLayoutNode = phyloNodeToOrderingNodeMap.getOrDefault(q, (up ? aboveLayoutNode : belowLayoutNode));

						if (!pLayoutNode.isAdjacent(qLayoutNode)) {
							var layoutEdge = graph.newEdge(pLayoutNode, qLayoutNode);
							edgeWeightMap.put(layoutEdge, 1.0);
						} else {
							var layoutEdge = pLayoutNode.getCommonEdge(qLayoutNode);
							edgeWeightMap.put(layoutEdge, edgeWeightMap.get(layoutEdge) + 1.0);
						}
					}
				}
			}
		}
	}

	public Graph getGraph() {
		return graph;
	}

	public Map<Node, Node> getPhyloNodeToOrderingNodeMap() {
		return phyloNodeToOrderingNodeMap;
	}

	public List<Node> getOrderingNodes() {
		return orderingNodes;
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

	/**
	 * for the ordering of LSA children of the root node v, computes all lists of entangled nodes
	 *
	 * @return partitioning of LSA children of v into entangled subsets
	 */
	public List<List<Node>> computeEntangledSubLists() {
		var parts = new ArrayList<List<Node>>();
		for (var component : ConnectedComponents.components(graph)) {
			var list = component.stream().map(this::getPhylogenyNode).filter(Objects::nonNull).toList();
			if (!list.isEmpty())
				parts.add(new ArrayList<>(list));
		}
		return parts;
	}
}

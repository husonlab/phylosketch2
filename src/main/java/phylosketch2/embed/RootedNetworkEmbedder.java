/*
 * RootedNetworkEmbedder.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch2.embed;

import javafx.geometry.Point2D;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import jloda.util.NumberUtils;

import java.util.*;

/**
 * computes an embedding of a rooted network
 * Daniel Huson, 2.2020
 */
public class RootedNetworkEmbedder {

	public static NodeArray<Point2D> apply(PhyloTree graph) {
		final NodeArray<Point2D> nodePointMap = graph.newNodeArray();

		try (NodeArray<Node> reticulation2LSA = new NodeArray<>(graph);
			 NodeArray<List<Node>> node2LSAChildren = new NodeArray<>(graph)) {
			LSATreeUtilities.computeLSAOrdering(graph, reticulation2LSA, node2LSAChildren);
			Optional<Node> root = graph.nodeStream().filter(v -> v.getInDegree() == 0).findFirst();
			if (root.isPresent()) {
				graph.setRoot(root.get());
				NodeIntArray levels = computeLevels(graph, node2LSAChildren, 1);
				final int maxLevel = NumberUtils.max(levels.values());
				NodeDoubleArray yCoord = computeYCoordinates(graph, node2LSAChildren, graph.getRoot());

				computeCoordinatesCladogramRec(graph.getRoot(), node2LSAChildren, yCoord, maxLevel, levels, nodePointMap);
			}
		}

		if (true) {
			double reticulateOffset;
			try (var nodeXMap = computeNodeXMap(graph, 0)) {
				reticulateOffset = 0.1 * nodeXMap.values().stream().mapToDouble(d -> d).max().orElse(0);
			}
			try (var nodeXMap = computeNodeXMap(graph, reticulateOffset)) {
				nodePointMap.replaceAll((k, v) -> new Point2D(nodeXMap.get(k), v.getY()));
			}
		}
		return nodePointMap;
	}

	public static NodeArray<Double> computeNodeXMap(PhyloTree graph, double reticulateOffset) {
		var nodeXMap = graph.newNodeDoubleArray();
		var nodes = IteratorUtils.asList(graph.nodes());
		nodeXMap.put(graph.getRoot(), 0.0);
		while (!nodes.isEmpty()) {
			for (var v : nodes) {
				var ok = true;
				var max = 0.0;
				for (var p : v.parents()) {
					if (nodeXMap.containsKey(p)) {
						max = Math.max(max, nodeXMap.get(p));
					} else {
						ok = false;
						break;
					}
				}
				if (ok) {
					if (v.getInDegree() == 1) {
						nodeXMap.put(v, nodeXMap.get(v.getParent()) + graph.getWeight(v.getFirstInEdge()));
					} else {
						nodeXMap.put(v, reticulateOffset + v.parentsStream(false).mapToDouble(nodeXMap::get).max().orElse(0));
					}
					nodes.remove(v);
					break;
				}
			}
		}
		return nodeXMap;
	}

	/**
	 * recursively compute node coordinates from edge angles:
	 *
	 * @param v Node
	 */
	private static void computeCoordinatesCladogramRec(Node v, NodeArray<List<Node>> node2LSAChildren, NodeDoubleArray yCoord, int maxLevel, NodeIntArray levels, Map<Node, Point2D> nodePointMap) {
		nodePointMap.put(v, new Point2D(50 * (maxLevel + 1 - levels.get(v)), 50 * yCoord.get(v)));
		for (Node w : node2LSAChildren.get(v)) {
			computeCoordinatesCladogramRec(w, node2LSAChildren, yCoord, maxLevel, levels, nodePointMap);
		}
	}


	/**
	 * compute the levels in the tree or network (max number of edges from node to a leaf)
	 *
	 * @return levels
	 */
	private static NodeIntArray computeLevels(PhyloTree graph, NodeArray<List<Node>> node2GuideTreeChildren, int add) {
		NodeIntArray levels = new NodeIntArray(graph);
		computeLevelsRec(graph, node2GuideTreeChildren, graph.getRoot(), levels, add, new HashSet<>());
		return levels;
	}

	/**
	 * compute node levels
	 */
	private static void computeLevelsRec(PhyloTree graph, NodeArray<List<Node>> node2GuideTreeChildren, Node v, NodeIntArray levels, int add, Set<Node> path) {
		path.add(v);
		int level = 0;
		Set<Node> below = new HashSet<>();
		for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
			Node w = f.getTarget();
			below.add(w);
			if (levels.get(w) == null)
				computeLevelsRec(graph, node2GuideTreeChildren, w, levels, add, path);
			level = Math.max(level, levels.getInt(w) + (graph.isTransferEdge(f) ? 0 : add));
		}
		final Collection<Node> lsaChildren = node2GuideTreeChildren.get(v);
		if (lsaChildren != null) {
			for (Node w : lsaChildren) {
				if (!below.contains(w) && !path.contains(w)) {
					if (levels.get(w) == null)
						computeLevelsRec(graph, node2GuideTreeChildren, w, levels, add, path);
					level = Math.max(level, levels.getInt(w) + add);
				}
			}
		}
		levels.set(v, level);
		path.remove(v);
	}

	/**
	 * compute the y-coordinates for the parallel view
	 *
	 * @return y-coordinates
	 */
	public static NodeDoubleArray computeYCoordinates(PhyloTree graph, NodeArray<List<Node>> node2LSAChildren, Node root) {
		final NodeDoubleArray yCoord = new NodeDoubleArray(graph);
		final List<Node> leafOrder = new ArrayList<>();
		computeYCoordinateOfLeavesRec(root, node2LSAChildren, 0, yCoord, leafOrder);
		if (graph.getNumberReticulateEdges() > 0)
			fixSpacing(leafOrder, yCoord);
		computeYCoordinateOfInternalRec(root, node2LSAChildren, yCoord);
		return yCoord;
	}

	/**
	 * recursively compute the y coordinate for a parallel or triangular diagram
	 *
	 * @param leafNumber rank of leaf in vertical ordering
	 * @return index of last leaf
	 */
	private static int computeYCoordinateOfLeavesRec(Node v, NodeArray<List<Node>> node2LSAChildren, int leafNumber, NodeDoubleArray yCoord, List<Node> nodeOrder) {
		List<Node> list = node2LSAChildren.get(v);

		if (list.isEmpty()) {
			// String taxonName = tree.getLabel(v);
			yCoord.put(v, (double) ++leafNumber);
			nodeOrder.add(v);
		} else {
			for (Node w : list) {
				leafNumber = computeYCoordinateOfLeavesRec(w, node2LSAChildren, leafNumber, yCoord, nodeOrder);
			}
		}
		return leafNumber;
	}


	/**
	 * recursively compute the y coordinate for the internal nodes of a parallel diagram
	 */
	private static void computeYCoordinateOfInternalRec(Node v, NodeArray<List<Node>> node2LSAChildren, NodeDoubleArray yCoord) {
		if (v.getOutDegree() > 0) {
			double first = Double.NEGATIVE_INFINITY;
			double last = Double.NEGATIVE_INFINITY;

			for (Node w : node2LSAChildren.get(v)) {
				double y = yCoord.getDouble(w);
				if (y == 0) {
					computeYCoordinateOfInternalRec(w, node2LSAChildren, yCoord);
					y = yCoord.getDouble(w);
				}
				last = y;
				if (first == Double.NEGATIVE_INFINITY)
					first = last;
			}
			yCoord.put(v, 0.5 * (last + first));
		}
	}

	/**
	 * fix spacing so that space between any two true leaves is 1
	 */
	public static void fixSpacing(List<Node> leafOrder, NodeDoubleArray yCoord) {
		final Node[] nodes = leafOrder.toArray(new Node[0]);
		double leafPos = 0;
		for (int lastLeaf = -1; lastLeaf < nodes.length; ) {
			int nextLeaf = lastLeaf + 1;
			while (nextLeaf < nodes.length && nodes[nextLeaf].getOutDegree() > 0)
				nextLeaf++;
			// assign fractional positions to intermediate nodes
			int count = (nextLeaf - lastLeaf) - 1;
			if (count > 0) {
				double add = 1.0 / (count + 1); // if odd, use +2 to avoid the middle
				double value = leafPos;
				for (int i = lastLeaf + 1; i < nextLeaf; i++) {
					value += add;
					yCoord.put(nodes[i], value);
				}
			}
			// assign whole positions to actual leaves:
			if (nextLeaf < nodes.length) {
				yCoord.put(nodes[nextLeaf], ++leafPos);
			}
			lastLeaf = nextLeaf;
		}
	}
}

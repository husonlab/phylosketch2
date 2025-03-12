/*
 *  HeightAndAngles.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.embed;

import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * computes the y-coordinates for the rectangular layout
 */
public class HeightAndAngles {
	public enum Averaging {
		ChildAverage, LeafAverage;
	}

	/**
	 * compute the y-coordinates for the parallel (rectangular) view
	 * @param tree the phylogeny
	 * @param nodeHeightMap the node height map
	 * @param averaging the averaging strategy
	 * @param fixSpacing should spaces between true leaves be fixed
	 */
	public static void apply(PhyloTree tree, Map<Node, Double> nodeHeightMap, Averaging averaging, boolean fixSpacing) {
		apply(tree, tree.getRoot(), nodeHeightMap, averaging, fixSpacing);
	}

	/**
	 * compute the y-coordinates for a parallel (rectangular) view
	 * @param tree the phylogeny
	 * @param root the root
	 * @param heights the node height map
	 * @param averaging the averaging strategy
	 * @param fixSpacingBetweenTrueLeaves should spaces between true leaves be fixed
	 */
	public static void apply(PhyloTree tree, Node root, Map<Node, Double> heights, Averaging averaging, boolean fixSpacingBetweenTrueLeaves) {
		var leafOrder = new LinkedList<Node>();
		computeYCoordinateOfLeavesRec(tree, root, 0, heights, leafOrder);
		if (fixSpacingBetweenTrueLeaves && tree.getNumberReticulateEdges() > 0)
			fixSpacingBetweenTrueLeaves(leafOrder, heights);
		if (averaging == Averaging.ChildAverage) {
			computeHeightInternalNodesAsChildAverageRec(tree, root, heights);
		} else {
			try (NodeArray<Pair<Double, Double>> minMaxBelowMap = tree.newNodeArray()) {
				tree.nodeStream().filter(tree::isLsaLeaf).forEach(v -> minMaxBelowMap.put(v, new Pair<>(heights.get(v), heights.get(v))));

				LSAUtils.postorderTraversalLSA(tree, tree.getRoot(), v -> {
					if (!tree.isLsaLeaf(v)) {
						var min = minMaxBelowMap.get(tree.getFirstChildLSA(v)).getFirst();
						var max = minMaxBelowMap.get(tree.getLastChildLSA(v)).getSecond();
						heights.put(v, 0.5 * (min + max));
						minMaxBelowMap.put(v, new Pair<>(min, max));
					}
				});
			}
		}
	}

	/**
	 * compute the angles for a radial (circular) view
	 *
	 * @param tree       the phylogeny
	 * @param angles     the node angle map
	 * @param averaging  the averaging strategy
	 * @param fixSpacing should spaces between true leaves be fixed
	 */
	public static void computeAngles(PhyloTree tree, Map<Node, Double> angles, Averaging averaging, boolean fixSpacing) {
		computeAngles(tree, tree.getRoot(), angles, averaging, fixSpacing);
	}

	/**
	 * compute the angles for a radial (circular) view
	 *
	 * @param tree       the phylogeny
	 * @param root       the root noe
	 * @param angles     the node angle map
	 * @param averaging  the averaging strategy
	 * @param fixSpacing should spaces between true leaves be fixed
	 */
	public static void computeAngles(PhyloTree tree, Node root, Map<Node, Double> angles, Averaging averaging, boolean fixSpacing) {
		HeightAndAngles.apply(tree, root, angles, averaging, fixSpacing);
		var max = angles.values().stream().mapToDouble(a -> a).max().orElse(0);
		var factor = 360.0 / max;
		angles.replaceAll((v, value) -> value * factor);
	}

	/**
	 * recursively compute the y coordinate for a parallel or triangular diagram
	 *
	 * @return index of last leaf
	 */
	private static int computeYCoordinateOfLeavesRec(PhyloTree tree, Node v, int leafNumber, Map<Node, Double> yCoord, List<Node> nodeOrder) {
		if (v.isLeaf() || tree.isLsaLeaf(v)) {
			// String taxonName = tree.getLabel(v);
			yCoord.put(v, (double) ++leafNumber);
			nodeOrder.add(v);
		} else {
			for (Node w : tree.lsaChildren(v)) {
				leafNumber = computeYCoordinateOfLeavesRec(tree, w, leafNumber, yCoord, nodeOrder);
			}
		}
		return leafNumber;
	}

	/**
	 * recursively compute the y coordinate for the internal nodes of a parallel diagram
	 */
	private static void computeHeightInternalNodesAsChildAverageRec(PhyloTree tree, Node v, Map<Node, Double> nodeHeightMap) {
		if (v.getOutDegree() > 0) {
			double first = Double.NEGATIVE_INFINITY;
			double last = Double.NEGATIVE_INFINITY;
			for (Node w : tree.lsaChildren(v)) {
				var height = nodeHeightMap.get(w);
				if (height == null) {
					computeHeightInternalNodesAsChildAverageRec(tree, w, nodeHeightMap);
					height = nodeHeightMap.get(w);
				}
				last = height;
				if (first == Double.NEGATIVE_INFINITY)
					first = last;
			}
			nodeHeightMap.put(v, 0.5 * (last + first));

		}
	}

	/**
	 * fix spacing so that space between any two true leaves is 100
	 */
	private static void fixSpacingBetweenTrueLeaves(Collection<Node> lsaTreeLeafOrder, Map<Node, Double> yCoord) {
		var nodes = lsaTreeLeafOrder.toArray(new Node[0]);
		double leafPos = 0;
		for (int lastTrueLeaf = -1; lastTrueLeaf < nodes.length; ) {
			int nextTrueLeaf = lastTrueLeaf + 1;
			while (nextTrueLeaf < nodes.length && !nodes[nextTrueLeaf].isLeaf())
				nextTrueLeaf++;
			// assign fractional positions to intermediate nodes
			int count = (nextTrueLeaf - lastTrueLeaf) - 1;
			if (count > 0) {
				double add = 1.0 / (count + 1); // if odd, use +2 to avoid the middle
				double value = leafPos;
				for (int i = lastTrueLeaf + 1; i < nextTrueLeaf; i++) {
					value += add;
					yCoord.put(nodes[i], value);
				}
			}
			// assign whole positions to actual leaves:
			if (nextTrueLeaf < nodes.length) {
				yCoord.put(nodes[nextTrueLeaf],++leafPos);
			}
			lastTrueLeaf = nextTrueLeaf;
		}
	}
}

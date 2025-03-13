/*
 *  CircularPhylogenyLayout.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.geometry.Point2D;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.DAGTraversals;
import jloda.graph.Node;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import jloda.util.Single;
import phylosketch.embed.optimize.OptimizeLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static phylosketch.embed.optimize.OptimizeLayout.computeScore;

/**
 * compute a circular layout
 * Daniel Huson, 12.2021, 3.2025
 */
public class CircularPhylogenyLayout {
	/**
	 * compute circular tree or network layout
	 *
	 * @param tree              the phylogeny
	 * @param toScale           draw edges to scale
	 * @param averaging         parent averaging method
	 * @param optimizeCrossings optimize crossings?
	 * @param points            the node layout points
	 */
	public static void apply(PhyloTree tree, boolean toScale, HeightAndAngles.Averaging averaging, boolean optimizeCrossings, Map<Node, Point2D> points) {
		apply(tree, toScale, averaging, optimizeCrossings, new Random(666), null, points);
	}

	/**
	 * compute layout for a circular cladogram
	 */
	public static void apply(PhyloTree tree, boolean toScale, HeightAndAngles.Averaging averaging, boolean optimizeCrossings, Random random, Map<Node, Double> nodeAngleMap0, Map<Node, Point2D> points) {
		var nodeAngleMap = (nodeAngleMap0 != null ? nodeAngleMap0 : new HashMap<Node, Double>());

		if (!tree.hasLSAChildrenMap())
			LSAUtils.setLSAChildrenAndTransfersMap(tree);

		var originalScore = (optimizeCrossings ? computeScore(tree, tree.getLSAChildrenMap(), points, true) : Integer.MAX_VALUE);

		points.clear();

		// compute radius:
		try (var nodeRadiusMap = tree.newNodeDoubleArray()) {
			if (!toScale) {
				final var maxDepth = computeMaxDepth(tree);
				try (var visited = tree.newNodeSet()) {
					tree.postorderTraversal(tree.getRoot(), v -> !visited.contains(v), v -> {
						if (tree.isLeaf(v)) {
							nodeRadiusMap.put(v, (double) maxDepth);
						} else {
							nodeRadiusMap.put(v, IteratorUtils.asStream(tree.lsaChildren(v)).mapToDouble(nodeRadiusMap::get).min().orElse(maxDepth) - 1);
						}
					});
				}
			} else {
				var percentOffset = 50.0;

				var averageWeight = tree.edgeStream().mapToDouble(tree::getWeight).average().orElse(1);
				var smallOffsetForReticulateEdge = (percentOffset / 100.0) * averageWeight;

				nodeRadiusMap.put(tree.getRoot(), 0.0);
				tree.preorderTraversal(v -> {
					var max = 0.0;
					for (var e : v.inEdges()) {
						var w = e.getSource();
						var wRadius = nodeRadiusMap.get(w);
						if (wRadius != null) {
							if (tree.isReticulateEdge(e)) {
								max = Math.max(max, wRadius + smallOffsetForReticulateEdge);
							} else
								max = Math.max(max, wRadius + tree.getWeight(e));
						}
						var vRadius = nodeRadiusMap.get(v);
						if (vRadius == null || max > vRadius)
							nodeRadiusMap.put(v, max);
					}
				});
			}

			// compute angle
			HeightAndAngles.computeAngles(tree, nodeAngleMap, averaging, !optimizeCrossings);

			tree.nodeStream().forEach(v -> points.put(v, GeometryUtilsFX.computeCartesian(nodeRadiusMap.get(v), nodeAngleMap.get(v))));

			if (optimizeCrossings) {
				if (originalScore == Integer.MAX_VALUE) {
					originalScore = computeScore(tree, tree.getLSAChildrenMap(), points, true);
				}
				if (originalScore > 0) {
					DAGTraversals.preOrderTraversal(tree.getRoot(), tree.getLSAChildrenMap(), v -> OptimizeLayout.optimizeOrdering(v, tree.getLSAChildrenMap(), points, random, false));
					var newScore = computeScore(tree, tree.getLSAChildrenMap(), points, true);
					System.err.printf("Layout optimization: %d -> %d%n", originalScore, newScore);
				}
				apply(tree, toScale, averaging, false, points);
			}
		}
	}

	/**
	 * compute the maximum number of edges from the root to a leaf
	 *
	 * @param tree the tree
	 * @return length of longest path
	 */
	public static int computeMaxDepth(PhyloTree tree) {
		var max = new Single<>(0);
		tree.breathFirstTraversal(tree.getRoot(), 0, (level, v) -> max.set(Math.max(max.get(), level)));
		return max.get();
	}
}

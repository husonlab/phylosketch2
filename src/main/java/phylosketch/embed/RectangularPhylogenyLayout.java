/*
 *  RectangularPhylogenyLayout.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.graph.DAGTraversals;
import jloda.graph.Node;
import jloda.graph.NodeDoubleArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import jloda.util.ProgramProperties;
import phylosketch.embed.optimize.OptimizeLayout;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import static phylosketch.embed.optimize.OptimizeLayout.computeCost;

/**
 * computes the rectangular layout for a rooted tree or network
 * Daniel Huson, 12.2021, 3.2025
 */
public class RectangularPhylogenyLayout {
	/**
	 * compute rectangular tree or network layout
	 * @param tree the phylogeny
	 * @param toScale draw edges to scale
	 * @param averaging parent averaging method
	 * @param points the node layout points
	 */
	public static void apply(PhyloTree tree, boolean toScale, HeightAndAngles.Averaging averaging, boolean optimize, Map<Node, Point2D> points) {
		apply(tree, toScale, averaging, optimize ? OptimizeLayout.How.Rectangular : OptimizeLayout.How.None, new Random(666), points);
	}

	/**
	 * compute rectangular tree or network layout
	 * @param tree the phylogeny
	 * @param toScale draw edges to scale
	 * @param averaging parent averaging method
	 * @param random random number generator for crossing optimization heuristic
	 * @param points the node layout points
	 */
	public static void apply(PhyloTree tree, boolean toScale, HeightAndAngles.Averaging averaging, OptimizeLayout.How how, Random random, Map<Node, Point2D> points) {
		if (!tree.hasLSAChildrenMap())
			LSAUtils.setLSAChildrenAndTransfersMap(tree);

		var originalScore = (how != OptimizeLayout.How.None ? computeCost(tree, tree.getLSAChildrenMap(), points, how) : Integer.MAX_VALUE);

		points.clear();

		points.put(tree.getRoot(), new Point2D(0, 0));
		try (var yCoord = tree.newNodeDoubleArray()) {
			HeightAndAngles.apply(tree, yCoord, averaging, how == OptimizeLayout.How.None);
			if (toScale) {
				setCoordinatesPhylogram(tree, yCoord, points);
			} else {
				var combiningNodes = 0.0;
				var transferNodes = 0.0;
				for (var v : tree.nodes()) {
					if (v.getInDegree() > 1) {
						if (v.inEdgesStream(false).anyMatch(tree::isTransferAcceptorEdge)) {
							transferNodes += 1;
						} else combiningNodes += 1;
					}
				}
				if (transferNodes > combiningNodes) {
					var longestPath = new HashMap<Node, Double>();
					longestPath.put(tree.getRoot(), 0.0);
					computeLongestPathsRec(tree, tree.getRoot(), longestPath);
					for (var i = 0; i < 5; i++) {
						var changed = false;
						for (var e : tree.edges()) {
							if (tree.isTransferEdge(e)) {
								var longestPathSource = longestPath.get(e.getSource());
								var longestPathTarget = longestPath.get(e.getTarget());
								if (longestPathTarget > longestPathSource) {
									longestPath.put(e.getSource(), longestPathTarget);
									changed = true;
								}
							}
						}
						if (changed) {
							computeLongestPathsRec(tree, tree.getRoot(), longestPath);
						} else
							break;
					}

					if (false) {
						for (var i = 0; i < 5; i++) {
							var changed = false;
							for (var e : tree.edges()) {
								if (tree.isTransferEdge(e)) {
									var longestPathSource = longestPath.get(e.getSource());
									var longestPathTarget = longestPath.get(e.getTarget());
									if (longestPathTarget <= longestPathSource) {
										longestPath.put(e.getSource(), longestPathSource - 0.05);
										changed = true;
									}
								}
							}
							if (!changed)
								break;
						}
					}

					var max = longestPath.values().stream().mapToDouble(d -> d).max().orElse(0.0);
					for (var v : tree.nodes()) {
						if (v.isLeaf())
							points.put(v, new Point2D(max, yCoord.get(v)));
						else
							points.put(v, new Point2D(longestPath.get(v), yCoord.get(v)));
					}
				} else {
					try (var levels = tree.newNodeIntArray()) {
						// compute levels: max length of path from node to a leaf
						tree.postorderTraversal(v -> {
							if (v.isLeaf())
								levels.put(v, 0);
							else {
								var level = 0;
								if (true) {
									for (var e : v.outEdges()) {
										var w = e.getTarget();

										if (tree.isTransferEdge(e))
											level = Math.max(level, levels.get(w) - 1);
										else
											level = Math.max(level, levels.get(w));
									}
								} else {
									for (var w : v.children()) {
										level = Math.max(level, levels.get(w));
									}
								}
								var prev = (levels.get(v) != null ? levels.get(v) : 0);
								if (level + 1 > prev)
									levels.set(v, level + 1);
							}
						});
						for (var v : tree.nodes()) {
							var dx = 0.0;
							if (v.getInDegree() <= 1 && v.outEdgesStream(false).filter(tree::isTransferEdge).count() == v.getOutDegree() - 1)
								dx = 0.01 * levels.get(v);
							points.put(v, new Point2D(-(levels.get(v) + dx), yCoord.get(v)));
						}
					}
				}
			}
			if (how != OptimizeLayout.How.None) {
				if (originalScore == Integer.MAX_VALUE) {
					originalScore = computeCost(tree, tree.getLSAChildrenMap(), points, how);
				}
				if (originalScore > 0) {
					DAGTraversals.preOrderTraversal(tree.getRoot(), tree.getLSAChildrenMap(), v -> OptimizeLayout.optimizeOrdering(v, tree.getLSAChildrenMap(), points, random, how));
					var newScore = computeCost(tree, tree.getLSAChildrenMap(), points, how);
					if (false)
						System.err.printf("Layout optimization: %d -> %d%n", originalScore, newScore);
				}
				apply(tree, toScale, averaging, false, points);
			}
		}
	}

	private static void computeLongestPathsRec(PhyloTree tree, Node v, HashMap<Node, Double> longestPath) {
		var vDist = longestPath.get(v);
		for (var f : v.outEdges()) {
			var w = f.getTarget();
			var wDist = (tree.isTreeEdge(f) || tree.isTransferAcceptorEdge(f) ? vDist + 1.0 : vDist);
			if (tree.isReticulateEdge(f) && !tree.isTransferEdge(f) && !tree.isTransferAcceptorEdge(f)) {
				wDist += 0.5;
			}
			if (!longestPath.containsKey(w) || wDist > longestPath.get(w))
				longestPath.put(w, wDist);
			computeLongestPathsRec(tree, w, longestPath);
		}
	}

	/**
	 * This code assumes that all edges are directed away from the root.
	 */
	public static void setCoordinatesPhylogram(PhyloTree tree, NodeDoubleArray yCoord, Map<Node, Point2D> nodePointMap) {
		var percentOffset = ProgramProperties.get("ReticulationOffsetPercent", 50.0);

		var averageWeight = tree.edgeStream().mapToDouble(tree::getWeight).average().orElse(1);
		var smallOffsetForReticulateEdge = (percentOffset / 100.0) * averageWeight;

		var rootHeight = yCoord.get(tree.getRoot());

		try (var assigned = tree.newNodeSet()) {
			// assign coordinates:
			var queue = new LinkedList<Node>();
			queue.add(tree.getRoot());
			while (!queue.isEmpty()) // breath-first assignment
			{
				var w = queue.remove(0); // pop
				var ok = true;
				if (w.getInDegree() == 1) // has regular in edge
				{
					var e = w.getFirstInEdge();
					var v = e.getSource();
					var location = nodePointMap.get(v);

					if (!assigned.contains(v)) // can't process yet
					{
						ok = false;
					} else {
						var height = yCoord.get(e.getTarget());
						var u = e.getTarget();
						nodePointMap.put(u, new Point2D(location.getX() + tree.getWeight(e), height));
						assigned.add(u);
					}
				} else if (w.getInDegree() > 1 && w.inEdgesStream(false).anyMatch(tree::isTransferAcceptorEdge)) { // todo: just added this, might cause problems...
					var e = w.inEdgesStream(false).filter(tree::isTransferAcceptorEdge).findAny().orElse(w.getFirstInEdge());
					var v = e.getSource();
					var location = nodePointMap.get(v);
					if (!assigned.contains(v)) // can't process yet
					{
						ok = false;
					} else {
						var height = yCoord.get(e.getTarget());
						var u = e.getTarget();
						double weight;
						if (tree.hasEdgeWeights() && tree.getEdgeWeights().containsKey(e))
							weight = tree.getEdgeWeights().get(e);
						else if (tree.isTreeEdge(e) || tree.isTransferAcceptorEdge(e))
							weight = 1.0;
						else if (tree.isTransferEdge(e))
							weight = 0.01;
						else weight = 0.1;
						nodePointMap.put(u, new Point2D(location.getX() + weight, height));
						assigned.add(u);
					}
				} else if (w.getInDegree() > 1) {
					var x = Double.NEGATIVE_INFINITY;
					for (var f : w.inEdges()) {
						var u = f.getSource();
						var location = nodePointMap.get(u);
						if (location == null) {
							ok = false;
						} else {
							x = Math.max(x, location.getX());
						}
					}
					if (ok && x > Double.NEGATIVE_INFINITY) {
						x += smallOffsetForReticulateEdge;
						nodePointMap.put(w, new Point2D(x, yCoord.get(w)));
						assigned.add(w);
					}
				} else  // is root node
				{
					nodePointMap.put(w, new Point2D(0, rootHeight));
					assigned.add(w);
				}

				if (ok)  // add children to end of queue:
					queue.addAll(IteratorUtils.asList(w.children()));
				else  // process this node again later
					queue.add(w);
			}
		}
	}
}


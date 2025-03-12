/*
 * OptimizeLayout.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.Single;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

import static jloda.graph.DAGTraversals.postOrderTraversal;
import static jloda.graph.DAGTraversals.preOrderTraversal;

/**
 * optimize the layout of a rooted phylogenetic network
 * Daniel Huson, 2.2025
 */
public class OptimizeLayout {
	/**
	 * optimize the LSA children of a given node v
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param points      the node layout points
	 * @return true, if optimization algorithm applied
	 */
	public static boolean optimizeOrdering(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points, Random random) {
		var yLeaves = lsaChildren.keySet().stream().filter(u -> !lsaChildren.containsKey(u) || lsaChildren.get(u).isEmpty()).mapToDouble(u -> points.get(u).getY()).sorted().toArray();

		var originalOrdering = new ArrayList<>(lsaChildren.get(v));
		var crossEdges = computeCrossEdges(v, originalOrdering, lsaChildren);
		var originalScore = computeScore(v, originalOrdering, crossEdges, yLeaves, lsaChildren, points);
		var bestOrdering = new Single<>(new ArrayList<>(originalOrdering));
		var bestScore = new Single<>(originalScore);

		var permutations = (originalOrdering.size() <= 8 ? Permutations.generateAllPermutations(originalOrdering) : Permutations.generateRandomPermutations(originalOrdering, 100000, random));
		for (var permuted : permutations) {
			var score = computeScore(v, permuted, crossEdges, yLeaves, lsaChildren, points);
			if (score < bestScore.get()) {
				bestScore.set(score);
				bestOrdering.set(new ArrayList<>(permuted));
			}
		}
		if (bestScore.get() < originalScore) {
			updateLSAChildrenOrderAndPoints(v, bestOrdering.get(), lsaChildren, points);
		}

		return true;
	}

	/**
	 * reverse the children of a given node v. This is for illustration purposes
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param points      the node layout points
	 */
	public static void reverseOrdering(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var yLeaves = lsaChildren.keySet().stream().filter(u -> !lsaChildren.containsKey(u) || lsaChildren.get(u).isEmpty()).mapToDouble(u -> points.get(v).getY()).sorted().toArray();
		var originalOrdering = new ArrayList<>(lsaChildren.get(v));
		var crossEdges = computeCrossEdges(v, originalOrdering, lsaChildren);
		var originalScore = computeScore(v, originalOrdering, crossEdges, yLeaves, lsaChildren, points);
		var reverseOrdering = CollectionUtils.reverse(originalOrdering);
		var reverseScore = computeScore(v, reverseOrdering, crossEdges, yLeaves, lsaChildren, points);
		System.err.println("Reverse: " + originalScore + " -> " + reverseScore);
		lsaChildren.put(v, reverseOrdering);
	}

	/**
	 * compute the total layout score
	 *
	 * @param tree        the phylogeny
	 * @param lsaChildren children mapping
	 * @param points      the node to point map
	 * @return the score
	 */
	public static int computeScore(PhyloTree tree, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		try {
			var yLeaves = lsaChildren.keySet().stream().filter(u -> !lsaChildren.containsKey(u) || lsaChildren.get(u).isEmpty()).mapToDouble(v -> points.get(v).getY()).sorted().toArray();
			var score = new LongAdder();
			preOrderTraversal(tree.getRoot(), lsaChildren::get, v -> {
				var ordering = lsaChildren.get(v);
				var crossEdges = computeCrossEdges(v, ordering, lsaChildren);
				score.add(computeScore(v, ordering, crossEdges, yLeaves, lsaChildren, points));
			});
			return (int) score.sum();
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}


	/**
	 * computes the score for a proposed new ordering of children of a node v
	 *
	 * @param v           the node
	 * @param newOrdering the proposed new ordering
	 * @param crossEdges  the reticulate cross edges
	 * @param lsaChildren the LSA map
	 * @param points      the current points (proposed new order has not been applied)
	 * @return the total y extent of all
	 */
	private static int computeScore(Node v, List<Node> newOrdering, Collection<List<Edge>> crossEdges, double[] yLeaves, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var delta = computeDelta(newOrdering, lsaChildren, points);
		var nodeIndexMap = computeNodeIndexMap(newOrdering, lsaChildren);

		var n = delta.length;
		var fromAbove = new int[n];
		var fromBelow = new int[n];

		var score = 0;
		for (var edges : crossEdges) {
			for (var e : edges) {
				Node p;
				Node q;
				if (nodeIndexMap.containsKey(e.getSource())) {
					p = e.getSource();
					q = e.getTarget();
				} else {
					p = e.getTarget();
					q = e.getSource();
				}
				int pIndex = nodeIndexMap.get(p);
				int qIndex = nodeIndexMap.getOrDefault(q, -1); // -1 indicates an edge to outside
				if (pIndex != qIndex) {
					var yp = points.get(p).getY() + delta[pIndex];
					var yq = (qIndex != -1 ? points.get(q).getY() + delta[qIndex] : points.get(q).getY());
					if (true)
						score += 1000 * NumberUtils.countInRange(yLeaves, Math.min(yp, yq), Math.max(yp, yq));
					else score += (int) (1000 * Math.abs(yp - yq));

					if (qIndex != -1) {
						if (yp < yq)
							fromAbove[qIndex]++;
						else fromBelow[qIndex]++;
					}
				}
			}
		}

		// each one-sided component adds slightly to the score
		for (var i = 0; i < n; i++) {
			if ((fromBelow[i] == 0) != (fromAbove[i] == 0)) {
				score += 1;
			}
		}
		return score;
	}

	/**
	 * computes the delta to apply to the y-coordinates when reordering subtrees
	 *
	 * @param newOrdering the new order
	 * @param lsaChildren the lsa children
	 * @param points      the  points
	 * @return the delta array
	 */
	public static double[] computeDelta(List<Node> newOrdering, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var n = newOrdering.size();

		var low = new double[n];
		var high = new double[n];

		Arrays.fill(low, Double.MAX_VALUE);
		Arrays.fill(high, Double.MIN_VALUE);

		for (int i = 0; i < newOrdering.size(); i++) {
			var index = i;
			var w = newOrdering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> {
				var y = points.get(u).getY();
				low[index] = Math.min(low[index], y);
				high[index] = Math.max(high[index], y);
			});
		}
		var min = Double.MAX_VALUE;
		var max = Double.MIN_VALUE;
		var extent = 0.0;
		for (var i = 0; i < n; i++) {
			extent += (high[i] - low[i]);
			min = Math.min(min, low[i]);
			max = Math.max(max, high[i]);
		}
		var gap = (max - min - extent) / (n - 1);

		var delta = new double[n];
		var pos = new double[n];
		for (int i = 0; i < newOrdering.size(); i++) {
			if (i == 0) {
				pos[i] = min;
			} else {
				pos[i] = pos[i - 1] + (high[i - 1] - low[i - 1]) + gap;
			}
			delta[i] = pos[i] - low[i];
		}
		return delta;
	}

	/**
	 * computes mapping of all nodes below nodes in the ordering to the index of their ancestor in the ordering
	 *
	 * @param ordering    ordering of LSA siblings
	 * @param lsaChildren the node to LSA children map
	 * @return each node mapped to index of ancestor in ordering
	 */
	public static Map<Node, Integer> computeNodeIndexMap(List<Node> ordering, Map<Node, List<Node>> lsaChildren) {
		var nodeIndex = new HashMap<Node, Integer>();
		for (int i = 0; i < ordering.size(); i++) {
			var index = i;
			var w = ordering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> nodeIndex.put(u, index));
		}
		return nodeIndex;
	}

	private static Collection<List<Edge>> computeCrossEdges(Node v, List<Node> ordering, Map<Node, List<Node>> lsaChildren) {
		var tree = (PhyloTree) v.getOwner();
		var nodeIndexMap = computeNodeIndexMap(ordering, lsaChildren);

		var nodeCrossEdges = new HashMap<Integer, List<Edge>>();
		for (int i = 0; i < ordering.size(); i++) {
			var index = i;
			var w = ordering.get(i);
			var edges = new ArrayList<Edge>();
			nodeCrossEdges.put(index, edges);
			postOrderTraversal(w, lsaChildren::get, u -> {
				for (var e : u.outEdges()) {
					if (tree.isReticulateEdge(e) && !tree.isTransferAcceptorEdge(e) && nodeIndexMap.getOrDefault(e.getTarget(), -1) != index) {
						edges.add(e);
					}
				}
				for (var e : u.inEdges()) {
					if (e.getSource() != v && tree.isReticulateEdge(e) && !tree.isTransferAcceptorEdge(e) && nodeIndexMap.getOrDefault(e.getSource(), -1) != index) {
						edges.add(e);
					}
				}
			});
		}
		return nodeCrossEdges.values();
	}

	/**
	 * compute the average vertical gap between leaves
	 *
	 * @param tree   the phylogeny
	 * @param points the node locations
	 * @return the average gap
	 */
	public static double computeLeafDy(PhyloTree tree, Map<Node, Point2D> points) {
		var array = tree.nodeStream().filter(Node::isLeaf).mapToDouble(v -> points.get(v).getY()).sorted().toArray();
		double sum = 0.0;
		for (int i = 1; i < array.length; i++) {
			sum += Math.abs(array[i] - array[i - 1]);
		}
		return sum / (array.length - 1);
	}

	/**
	 * for a given point with a new order of LSA children, updates the points for all nodes in the subtrees
	 *
	 * @param v           the node
	 * @param newOrdering the new order
	 * @param lsaChildren the lsa children
	 * @param points      the  points
	 */
	public static void updateLSAChildrenOrderAndPoints(Node v, List<Node> newOrdering, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var delta = computeDelta(newOrdering, lsaChildren, points);

		for (int i = 0; i < newOrdering.size(); i++) {
			var index = i;
			var w = newOrdering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> points.computeIfPresent(u, (k, point) -> new Point2D(point.getX(), point.getY() + delta[index])));
		}

		lsaChildren.put(v, newOrdering);
	}
}

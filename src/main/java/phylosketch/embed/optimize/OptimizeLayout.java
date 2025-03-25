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
	public static boolean optimizeOrdering(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points, Random random, How how) {
		var originalOrdering = new ArrayList<>(lsaChildren.get(v));
		var crossEdges = computeCrossEdges(v, originalOrdering, lsaChildren);
		if (!crossEdges.isEmpty()) {
			var originalCost = Integer.MAX_VALUE;
			var bestOrdering = new Single<List<Node>>(originalOrdering);
			var bestCost = new Single<>(originalCost);

			double span;
			{
				var yMin = points.values().stream().mapToDouble(Point2D::getY).min().orElse(0.0);
				var yMax = points.values().stream().mapToDouble(Point2D::getY).max().orElse(0.0);
				span = yMax - yMin + 1;
			}

			if (true) { // use simulated annealing for larger problems
				if (originalOrdering.size() <= 8) {
					for (var permuted : Permutations.generateAllPermutations(originalOrdering)) {
						var cost = computeCost(v, span, permuted, crossEdges, lsaChildren, points, how);
						if (cost < bestCost.get()) {
							bestCost.set(cost);
							bestOrdering.set(new ArrayList<>(permuted));
							if (bestCost.get() == 0)
								break;
						}
					}
				} else {
					var simulatedAnnealing = new SimulatedAnnealingMinLA<Node>();
					var pair = simulatedAnnealing.apply(originalOrdering, random, (permuted) -> computeCost(v, span, permuted, crossEdges, lsaChildren, points, how));
					bestOrdering.set(pair.getFirst());
					bestCost.set(pair.getSecond());
				}
			} else {
				var permutations = (originalOrdering.size() <= 8 ? Permutations.generateAllPermutations(originalOrdering) : Permutations.generateRandomPermutations(originalOrdering, 100000, random));
				for (var permuted : permutations) {
					var cost = computeCost(v, span, permuted, crossEdges, lsaChildren, points, how);
					if (cost < bestCost.get()) {
						bestCost.set(cost);
						bestOrdering.set(new ArrayList<>(permuted));
						if (bestCost.get() == 0)
							break;
					}
				}
			}
			if (bestCost.get() < originalCost) {
				updateLSAChildrenOrderAndPoints(v, bestOrdering.get(), lsaChildren, points);
				return true;
			}
		}
		return false;
	}

	/**
	 * compute the total layout cost
	 *
	 * @param tree        the phylogeny
	 * @param lsaChildren children mapping
	 * @param points      the node to point map
	 * @return the cost
	 */
	public static int computeCost(PhyloTree tree, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points, How how) {
		try {
			double span;
			{
				var yMin = points.values().stream().mapToDouble(Point2D::getY).min().orElse(0.0);
				var yMax = points.values().stream().mapToDouble(Point2D::getY).max().orElse(0.0);
				span = yMax - yMin + 1;
			}

			var cost = new LongAdder();
			preOrderTraversal(tree.getRoot(), lsaChildren::get, v -> {
				var ordering = lsaChildren.get(v);
				var crossEdges = computeCrossEdges(v, ordering, lsaChildren);
				cost.add(computeCost(v, span, ordering, crossEdges, lsaChildren, points, how));
			});
			return (int) cost.sum();
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * computes the cost for a proposed new ordering of children of a node v
	 *
	 * @param v           the node
	 * @param newOrdering the proposed new ordering
	 * @param crossEdges  the reticulate cross edges
	 * @param lsaChildren the LSA map
	 * @param points      the current points (proposed new order has not been applied)
	 * @return the total y extent of all
	 */
	private static int computeCost(Node v, double span, List<Node> newOrdering, Collection<List<Edge>> crossEdges, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points, How how) {
		var delta = computeDelta(newOrdering, lsaChildren, points);
		var nodeIndexMap = computeNodeIndexMap(newOrdering, lsaChildren);

		var n = delta.length;
		var fromAbove = new int[n];
		var fromBelow = new int[n];

		var cost = 0;
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
					var d = Math.abs(yp - yq);
					if (how == How.Circular) {
						if (d > 0.5 * span)
							d = (span - d);
					}
					if (false) {
						if (v.getOwner() instanceof PhyloTree tree && tree.hasEdgeConfidences() && tree.getEdgeConfidences().containsKey(e)) {
							var confidence = Math.max(1, tree.getConfidence(e));
							cost += (int) (confidence * 1000 * d);
						}

					} else {
						cost += (int) (1000 * d);
					}

					if (qIndex != -1) {
						if (yp < yq)
							fromAbove[qIndex]++;
						else fromBelow[qIndex]++;
					}
				}
			}
		}

		// each one-sided component adds slightly to the cost
		for (var i = 0; i < n; i++) {
			if ((fromBelow[i] == 0) != (fromAbove[i] == 0)) {
				cost += 1;
			}
		}
		return cost;
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

	public enum How {Rectangular, Circular, None}
}

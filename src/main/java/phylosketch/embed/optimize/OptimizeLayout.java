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
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import jloda.util.Single;
import phylosketch.embed.HeightAndAngles;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static jloda.graph.DAGTraversals.postOrderTraversal;
import static jloda.graph.DAGTraversals.preOrderTraversal;

/**
 * new ideas for processing points
 * Daniel Huson, 2.2025
 */
public class OptimizeLayout {

	/*
	todo: the following network breaks optimization if only optimised once
	 (((((B)#H2,(C)#H3))#H1,(A,#H2)),((D,#H3),#H1));
	 */

	/**
	 * optimize the ordering of LSA children so to minimize the y-extent of all reticulate edges
	 *
	 * @param tree   the rooted network
	 * @param points the node layout points
	 */
	public static void optimizeOrdering(PhyloTree tree, Map<Node, Point2D> points) {
		BiFunction<Node, Node, Double> scoring = (v, w) -> Math.abs(points.get(v).getY() - points.get(w).getY());
		// todo: using the scoring function everywhere

		if (tree.hasReticulateEdges()) {
			var origScore = tree.edgeStream().filter(e -> e.getTarget().getInDegree() >= 2)
					.mapToDouble(e -> scoring.apply(e.getSource(), e.getTarget())).sum();
			var lsaChildren = tree.getLSAChildrenMap();
			var leafDy = computeLeafDy(tree, points);
			preOrderTraversal(tree.getRoot(), lsaChildren::get, v -> optimizeOrdering(v, leafDy, lsaChildren, points));
			if (false)
				HeightAndAngles.fixSpacing(tree, points);
			var newScore = tree.edgeStream().filter(e -> e.getTarget().getInDegree() >= 2)
					.mapToDouble(e -> scoring.apply(e.getSource(), e.getTarget())).sum();
			System.err.println("Optimize: " + Math.round(origScore) + " -> " + Math.round(newScore));

		}
	}

	public static double computeLeafDy(PhyloTree tree, Map<Node, Point2D> points) {
		var array = tree.nodeStream().filter(Node::isLeaf).mapToDouble(v -> points.get(v).getY()).sorted().toArray();
		double sum = 0.0;
		for (int i = 1; i < array.length; i++) {
			sum += Math.abs(array[i] - array[i - 1]);
		}
		return sum / (array.length - 1);
	}

	/**
	 * optmize the LSA children of a given node v
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param points      the node layout points
	 */
	public static void optimizeOrdering(Node v, double leafDy, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var originalOrdering = new ArrayList<>(lsaChildren.get(v));
		var crossEdges = computeCrossEdges(originalOrdering, lsaChildren);
		var originalScore = computeScore(v, leafDy, originalOrdering, crossEdges, lsaChildren, points);
		if (originalScore > 0) {
			Single<List<Node>> bestOrdering = new Single<>(new ArrayList<>(originalOrdering));
			var bestScore = new Single<>(originalScore);

			if (originalOrdering.size() <= 7) {
				generatePermutationsRec(originalOrdering, 0, ordering -> {
					var score = computeScore(v, leafDy, ordering, crossEdges, lsaChildren, points);
					if (score < bestScore.get()) {
						bestScore.set(score);
						bestOrdering.set(new ArrayList<>(ordering));
					}
				});
			} else {
				bestOrdering.set(greedyOptimize(v, lsaChildren, points));
				bestScore.set(computeScore(v, leafDy, bestOrdering.get(), crossEdges, lsaChildren, points));
			}
			if (bestScore.get() < originalScore) {
				updateLSAChildrenOrderAndPoints(v, bestOrdering.get(), lsaChildren, leafDy, points);
			}
		}
	}

	/**
	 * use a greedy heuristic to reorder children when the number of children is too big for considering all permutations
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param points      the node layout points
	 * @return the optimized order of children of v
	 */
	public static List<Node> greedyOptimize(Node v, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var orderingGraph = new OrderingGraph(v, lsaChildren, points);

		try (NodeArray<Node> otherEnd = orderingGraph.getGraph().newNodeArray()) {
			for (var p : orderingGraph.getGraph().nodes()) {
				otherEnd.put(p, p);
			}

			var aboveNode = orderingGraph.getLayoutNodes().get(0);
			var belowNode = orderingGraph.getLayoutNodes().get(orderingGraph.getLayoutNodes().size() - 1);
			var ends = List.of(belowNode, aboveNode);

			var edges = IteratorUtils.asList(orderingGraph.getGraph().edges());
			edges.sort((a, b) -> -Double.compare(orderingGraph.getEdgeWeight(a), orderingGraph.getEdgeWeight(b)));

			var selectedEdges = new HashSet<Edge>();
			for (var e : edges) {
				// ensure that above and below nodes have degree <=1 in selection
				if (CollectionUtils.intersects(e.nodes(), ends) && selectedEdges.stream().anyMatch(f -> CollectionUtils.intersects(f.nodes(), ends))) {
					continue;
				}

				var p = e.getSource();
				var q = e.getTarget();
				if (otherEnd.get(p) != null && otherEnd.get(q) != null && otherEnd.get(p) != q) {
					selectedEdges.add(e);
					var pp = otherEnd.get(p);
					var qq = otherEnd.get(q);
					otherEnd.put(p, null);
					otherEnd.put(q, null);
					otherEnd.put(pp, qq);
					otherEnd.put(qq, pp);
				}
			}
			for (var e : CollectionUtils.difference(orderingGraph.getGraph().getEdgesAsList(), selectedEdges)) {
				orderingGraph.getGraph().deleteEdge(e);
			}

			var ordering = new ArrayList<Node>();

			for (var p : orderingGraph.getLayoutNodes()) {
				if (p.getDegree() == 0) {
					ordering.add(p);
				} else if (p.getDegree() == 1) {
					if (!ordering.contains(p)) {
						if (otherEnd.get(p) == aboveNode || p == belowNode) {
							p = otherEnd.get(p);
						}
						Edge inEdge = null;
						var ok = true;
						while (ok) {
							ordering.add(p);
							ok = false;
							for (var f : p.adjacentEdges()) {
								if (f != inEdge) {
									p = f.getOpposite(p);
									inEdge = f;
									if (!ordering.contains(p)) {
										ok = true;
									}
									break;
								}
							}
						}
					}
				} else if (p.getDegree() > 2)
					System.err.println("Error: wrong degree: " + p.getDegree());
			}
			return ordering.stream().map(orderingGraph::getPhylogenyNode).filter(Objects::nonNull).toList();
		}
	}

	/**
	 * reverse the children of a given node v. This is for illustration purposes
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param points      the node layout points
	 */
	public static void reverseOrdering(Node v, double leafDy, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var originalOrdering = new ArrayList<>(lsaChildren.get(v));
		var crossEdges = computeCrossEdges(originalOrdering, lsaChildren);
		var originalScore = computeScore(v, leafDy, originalOrdering, crossEdges, lsaChildren, points);
		var reverseOrdering = CollectionUtils.reverse(originalOrdering);
		var reverseScore = computeScore(v, leafDy, reverseOrdering, crossEdges, lsaChildren, points);
		System.err.println("Reverse: " + originalScore + " -> " + reverseScore);
		updateLSAChildrenOrderAndPoints(v, reverseOrdering, lsaChildren, leafDy, points);
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
	public static double computeScore(Node v, double leafDy, List<Node> newOrdering, Collection<List<Edge>> crossEdges, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
		var deltaTransform = computeDeltaTransform(newOrdering, lsaChildren, leafDy, points);
		var nodeIndexMap = computeNodeIndexMap(v, newOrdering, lsaChildren);

		var score = 0.0;
		for (var edges : crossEdges) {
			for (var e : edges) {
				var p = e.getSource();
				int pIndex = nodeIndexMap.get(p);
				var q = e.getTarget();
				int qIndex = nodeIndexMap.getOrDefault(q, -1); // -1 indicates an edge to outside
				if (pIndex != qIndex) {
					var yp = deltaTransform[pIndex].apply(points.get(p).getY());
					var yq = (qIndex != -1 ? deltaTransform[qIndex].apply(points.get(q).getY()) : points.get(q).getY());
					score += Math.abs(yp - yq);
				}
			}
		}
		return score;
	}

	/**
	 * for a given point with a new order of LSA children, updates the points for all nodes in the subtrees
	 *
	 * @param v           the node
	 * @param newOrdering the new order
	 * @param lsaChildren the lsa children
	 * @param points      the  points
	 */
	public static void updateLSAChildrenOrderAndPoints(Node v, List<Node> newOrdering, Map<Node, List<Node>> lsaChildren, double leafDy, Map<Node, Point2D> points) {
		var transform = computeDeltaTransform(newOrdering, lsaChildren, leafDy, points);

		for (int i = 0; i < newOrdering.size(); i++) {
			var index = i;
			var w = newOrdering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> {
				var point = points.get(u);
				point = new Point2D(point.getX(), transform[index].apply(point.getY()));
				points.put(u, point);
			});
		}

		lsaChildren.put(v, newOrdering);
	}

	/**
	 * computes the delta to apply to the y-coordinates when reordering subtrees
	 *
	 * @param v           the node
	 * @param newOrdering the new order
	 * @param lsaChildren the lsa children
	 * @param points      the  points
	 * @return the delta array
	 */
	@Deprecated
	public static double[] computeDelta(Node v, List<Node> newOrdering, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points) {
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

	public static Function<Double, Double>[] computeDeltaTransform(List<Node> newOrdering, Map<Node, List<Node>> lsaChildren, double leafDy, Map<Node, Point2D> points) {
		if (false) {
			var n = newOrdering.size();
			var transform = (Function<Double, Double>[]) new Function[n];

			var delta = computeDelta(null, newOrdering, lsaChildren, points);
			for (var i = 0; i < n; i++) {
				var index = i;
				transform[index] = y -> y + delta[index];
			}
			return transform;
		}

		var n = newOrdering.size();

		var low = new double[n];
		var high = new double[n];

		Arrays.fill(low, Double.MAX_VALUE);
		Arrays.fill(high, Double.MIN_VALUE);
		var hasLeaf = new boolean[n];

		for (int i = 0; i < newOrdering.size(); i++) {
			var index = i;
			var w = newOrdering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> {
				var y = points.get(u).getY();
				low[index] = Math.min(low[index], y);
				high[index] = Math.max(high[index], y);
				if (u.isLeaf())
					hasLeaf[index] = true;
			});
		}
		var numHasLeaf = 0;
		for (var has : hasLeaf) {
			if (has)
				numHasLeaf++;
		}

		if (false) {
			for (var i = 0; i < n; i++) {
				hasLeaf[i] = true;
			}
		}

		var min = Arrays.stream(low).min().orElse(0);

		var transform = (Function<Double, Double>[]) new Function[n];

		var pos = new double[n];
		var i = 0;
		var prevLeafI = 0;
		while (i < newOrdering.size()) {
			var index = i;
			if (hasLeaf[i]) {
				if (i == 0) {
					pos[i] = min;
					transform[i] = y -> y + (min - low[index]);
				} else {
					pos[i] = pos[prevLeafI] + (high[prevLeafI] - low[prevLeafI]) + leafDy;
					transform[i] = y -> y + (pos[index] - low[index]);
				}
				prevLeafI = i;

				i++;
			} else {
				// determine all consecutive non-leaf components that must be placed in same gap:
				var first = i; // first inclusive
				var last = i; // last inclusive
				var span = high[first] - low[first];
				while (last + 1 < newOrdering.size() && !hasLeaf[last + 1]) {
					last++;
					span += high[last] - low[last];
				}
				var count = (last - first) + 1;
				var gap = Math.max(0.1 * leafDy, (leafDy - span) / (count + 1));
				double scaleFactor;
				if (gap * (count + 1) + span <= leafDy)
					scaleFactor = 1.0;
				else
					scaleFactor = leafDy / (gap * (count + 1) + span);

				if (count == 1) {
					if (first == 0) { // note: in this case: first=index=i // todo: this needs debugging
						transform[i] = y -> y - low[index] + min + gap;
					} else {
						transform[i] = y -> y - low[index] + (pos[first - 1] - high[first - 1] + low[first - 1]) + gap;
					}
				} else { // count>=2 // todo: this needs debugging
					var pos2 = high[first - 1] + gap;
					for (var h = 0; h < count; h++) {
						var low1 = low[first];
						var low2 = pos2;
						var high1 = high[first];
						var d1 = high1 - low1;
						var d2 = scaleFactor * d1;
						transform[h] = y -> low2 + (y - low1) / d1 * d2;
						pos2 += d2 + gap;
					}
				}
				i = last + 1;
			}
		}
		return transform;
	}

	/**
	 * generate all permutations of elements ands apply the given consumer to each
	 *
	 * @param nodes    the list of elements. Note that elements will be permuted during computation
	 * @param index    the current index
	 * @param consumer the consumer to apply to each permutation
	 */
	private static <T> void generatePermutationsRec(List<T> nodes, int index, Consumer<List<T>> consumer) {
		if (index == nodes.size() - 1) {
			consumer.accept(nodes);
			return;
		}

		for (var i = index; i < nodes.size(); i++) {
			Collections.swap(nodes, i, index);
			generatePermutationsRec(nodes, index + 1, consumer);
			Collections.swap(nodes, i, index);
		}
	}

	public static Map<Node, Integer> computeNodeIndexMap(Node v, List<Node> ordering, Map<Node, List<Node>> lsaChildren) {
		var nodeIndex = new HashMap<Node, Integer>();
		for (int i = 0; i < ordering.size(); i++) {
			var index = i;
			var w = ordering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> {
				nodeIndex.put(u, index);
			});
		}
		return nodeIndex;
	}

	private static Collection<List<Edge>> computeCrossEdges(List<Node> ordering, Map<Node, List<Node>> lsaChildren) {
		var nodeIndexMap = new HashMap<Node, Integer>();
		for (int i = 0; i < ordering.size(); i++) {
			var index = i;
			var w = ordering.get(i);
			postOrderTraversal(w, lsaChildren::get, u -> {
				nodeIndexMap.put(u, index);
			});
		}

		var nodeCrossEdges = new HashMap<Integer, List<Edge>>();
		for (int i = 0; i < ordering.size(); i++) {
			var index = i;
			var w = ordering.get(i);
			var edges = new ArrayList<Edge>();
			nodeCrossEdges.put(index, edges);
			postOrderTraversal(w, lsaChildren::get, u -> {
				for (var e : u.outEdges()) {
					if (nodeIndexMap.getOrDefault(e.getTarget(), -1) != index) {
						edges.add(e);
					}
				}
			});
		}
		return nodeCrossEdges.values();
	}
}

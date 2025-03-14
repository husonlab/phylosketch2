/*
 * ReorderChildren.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.io;

import javafx.geometry.Point2D;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * rearrange the tree lexicographically or by position
 * Daniel Huson, 3.2024
 */
public class ReorderChildren {
	public enum SortBy {Alphabetical, Location}

	/**
	 * rearrange tree
	 *
	 * @param tree the phylogeny
	 */
	public static void apply(PhyloTree tree, Function<Node, Point2D> points, SortBy sortBy) {
		if (sortBy == SortBy.Alphabetical) {
			try (NodeArray<String> nodeLabelMap = tree.newNodeArray()) {
				LSAUtils.postorderTraversalLSA(tree, tree.getRoot(), v -> {
					nodeLabelMap.put(v, determineBestLabel(tree, nodeLabelMap, v));
					var list = new ArrayList<Pair<String, Edge>>();
					for (var e : v.outEdges()) {
						list.add(new Pair<>(nodeLabelMap.get(e.getTarget()), e));
					}
					list.sort(Comparator.comparing(Pair::getFirst));
					var newOrder = new ArrayList<Edge>(list.size());
					for (var pair : list) {
						newOrder.add(pair.getSecond());
					}
					v.rearrangeAdjacentEdges(newOrder);
				});
			}
		} else { // by position
			try (var nodePositionMap = tree.newNodeDoubleArray()) {
				LSAUtils.postorderTraversalLSA(tree, tree.getRoot(), v -> {
					var list = new ArrayList<Pair<Double, Edge>>();
					if (v.isLeaf()) {
						nodePositionMap.put(v, points.apply(v).getY());
					} else {
						var min = Double.MAX_VALUE;
						for (var e : v.outEdges()) {
							var value = nodePositionMap.get(e.getTarget());
							list.add(new Pair<>(value, e));
							min = Math.min(min, value);
						}
						nodePositionMap.put(v, min);
						list.sort(Comparator.comparing(Pair::getFirst));
						var newOrder = new ArrayList<Edge>(list.size());
						for (var pair : list) {
							newOrder.add(pair.getSecond());
						}
						v.rearrangeAdjacentEdges(newOrder);
					}
				});
			}
		}
	}

	private static String determineBestLabel(PhyloTree tree, NodeArray<String> nodeLabelMap, Node v) {
		if (v.isLeaf()) {
			return tree.getLabel(v);
		} else {
			var set = new TreeSet<String>();
			for (var w : v.children()) {
				set.add(nodeLabelMap.get(w));
			}
			return set.first();
		}
	}
}

/*
 * TraversalByLayout.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.geometry.Point2D;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import jloda.util.IteratorUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * traverses a tree or network based on the provided root position
 * Daniel Huson, 1.2025
 */
public class TraversalByLayout {
	/**
	 * run the taversal
	 *
	 * @param roots        the roots to start from
	 * @param nodes        the nodes
	 * @param rootPosition the root position to use
	 * @param consumer     the consumer to show to each node
	 */
	public static void apply(Collection<Node> roots, Collection<Node> nodes, RootPosition rootPosition, Consumer<Node> consumer) {
		Function<Node, Collection<Node>> sortedChildren = (v) -> {
			var sorted = IteratorUtils.asList(v.children());
			sorted.sort((a, b) -> switch (rootPosition.side()) {
				case Left, Right -> Double.compare(DrawView.getPoint(a).getY(), DrawView.getPoint(b).getY());
				case Bottom, Top -> Double.compare(DrawView.getPoint(a).getX(), DrawView.getPoint(b).getX());
				case Center -> {
					var apt = new Point2D(DrawView.getPoint(a).getX(), DrawView.getPoint(a).getY());
					var bpt = new Point2D(DrawView.getPoint(b).getX(), DrawView.getPoint(b).getY());
					var angle = GeometryUtilsFX.computeObservedAngle(rootPosition.location(), apt, bpt);
					yield Double.compare(angle, 0);
				}
			});
			return sorted;
		};
		var visited = new HashSet<Node>();

		roots.forEach(root -> traversalRec(root, visited, nodes, sortedChildren, consumer));
	}

	private static void traversalRec(Node v, HashSet<Node> visited, Collection<Node> nodes, Function<Node, Collection<Node>> sortedChildren,
									 Consumer<Node> consumer) {
		if (!visited.contains(v)) {
			visited.add(v);
			if (nodes.contains(v)) {
				consumer.accept(v);
			}
			for (var child : sortedChildren.apply(v)) {
				traversalRec(child, visited, nodes, sortedChildren, consumer);
			}
		}
	}


}

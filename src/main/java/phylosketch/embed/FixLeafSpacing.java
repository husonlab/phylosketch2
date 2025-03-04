/*
 * FixLeafSpacing.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.embed;

import javafx.geometry.Point2D;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Map;

/**
 * fix spacing between leaves of phylogeny
 * Daniel Huson, 3.2025
 */
public class FixLeafSpacing {
	/**
	 * apply a piece-wise linear map to all nodes so that leaves are spaced evenly
	 *
	 * @param tree   the phylogeny
	 * @param leafDy the desired distance between leaves
	 * @param points map of nodes to points
	 */
	public static void apply(PhyloTree tree, double leafDy, Map<Node, Point2D> points) {
		var source = tree.nodeStream().filter(Node::isLeaf).mapToDouble(u -> points.get(u).getY()).sorted().toArray();
		var target = createIncreasingArray(source.length, 0, leafDy);
		{
			var tmp = new double[source.length + 1];
			System.arraycopy(source, 0, tmp, 1, source.length);
			source = tmp;
			source[0] = (source[1] > 0 ? 0 : 2 * source[1] - source[2]);
		}
		{
			var tmp = new double[target.length + 1];
			System.arraycopy(target, 0, tmp, 1, target.length);
			target = tmp;
			target[0] = target[1] - leafDy;
		}
		var map = new PiecewiseLinearMap(source, target);
		tree.nodeStream().forEach(u -> points.put(u, new Point2D(points.get(u).getX(), map.map(points.get(u).getY()))));
	}

	/**
	 * creates an array of increasing values
	 *
	 * @param length the length
	 * @param start  the start value
	 * @param step   the step
	 * @return array
	 */
	public static double[] createIncreasingArray(int length, double start, double step) {
		var array = new double[length];
		for (var i = 0; i < length; i++) {
			array[i] = start;
			start += step;
		}
		return array;
	}
}

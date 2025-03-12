/*
 * TriangularPhylogenyLayout.java Copyright (C) 2025 Daniel H. Huson
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

import java.util.HashMap;
import java.util.Map;

@Deprecated // this makes no sense
public class TriangularPhylogenyLayout {

	/**
	 * Given the points for a rectangular layout, computes a circular one
	 *
	 * @param tree   the phylogeny
	 * @param points the points of a rectangular layout
	 * @return points of a triangular layout
	 */
	public static Map<Node, Point2D> apply(PhyloTree tree, Map<Node, Point2D> points) {
		var array = tree.nodeStream().filter(Node::isLeaf).mapToDouble(v -> points.get(v).getY()).sorted().toArray();
		var numLeaves = array.length;
		var leafDy = 0.0;
		for (var i = 1; i < numLeaves; i++) {
			leafDy += array[i] - array[i - 1];
		}
		leafDy /= numLeaves;

		var x1 = tree.nodeStream().mapToDouble(v -> points.get(v).getX()).min().orElse(0.0);
		var x2 = tree.nodeStream().mapToDouble(v -> points.get(v).getX()).max().orElse(0.0);

		var y1 = points.get(tree.getRoot()).getY();
		var minY2 = array[0] + leafDy;
		var maxY2 = array[array.length - 1] - leafDy;

		var minZ2 = 0.0;
		var maxZ2 = (numLeaves + 1) * leafDy;
		var z1 = maxZ2 / 2;


		System.err.printf("old triangle: (%.0f,%.0f) - (%.0f,%.0f) and (%.0f,%.0f)%n", x1, y1, x2, minY2, x2, maxY2);
		System.err.printf("new triangle: (%.0f,%.0f) - (%.0f,%.0f) and (%.0f,%.0f)%n", x1, z1, x2, minZ2, x2, maxZ2);

		var newPoints = new HashMap<Node, Point2D>();

		for (var entry : points.entrySet()) {
			var point = entry.getValue();

			var y = TriangularPhylogenyLayout.mapToNewLocation(point.getX(), point.getY(), x1, y1, x2, minY2, maxY2, z1, minZ2, maxZ2);
			System.err.println(point + ": " + point.getY() + " -> " + y);
			newPoints.put(entry.getKey(), new Point2D(point.getX(), y));
		}
		return newPoints;
	}


	private static Double mapToNewLocation(double x, double y,
										   double x1, double y1,
										   double x2, double y2min, double y2max,
										   double z1, double z2min, double z2max) {
		if (x == x1 && y == y1)
			return z1;

		// Compute y-values on the bounding lines at x
		double yMinLine = interpolate(x, x1, y1, x2, y2min);
		double yMaxLine = interpolate(x, x1, y1, x2, y2max);

		// Compute z-values on the bounding lines at x
		double zMinLine = interpolate(x, x1, z1, x2, z2min);
		double zMaxLine = interpolate(x, x1, z1, x2, z2max);

		// Ensure correct ordering
		if (yMinLine > yMaxLine) {
			double temp = yMinLine;
			yMinLine = yMaxLine;
			yMaxLine = temp;
		}
		if (zMinLine > zMaxLine) {
			double temp = zMinLine;
			zMinLine = zMaxLine;
			zMaxLine = temp;
		}

		// If y is within bounds, map linearly
		if (y >= yMinLine && y <= yMaxLine) {
			return mapValue(y, yMinLine, yMaxLine, zMinLine, zMaxLine);
		}
		// If y is above the upper bound, apply log mapping
		else if (y > yMaxLine) {
			double d = y - yMaxLine; // Distance above the upper line
			double logD = Math.min(d, 10 * Math.log(d + 1)); // Avoid log(0) issues
			return zMaxLine + logD;
		}
		// If y is below the lower bound, apply log mapping
		else {
			double d = yMinLine - y; // Distance below the lower line
			double logD = Math.min(d, 10 * Math.log(d + 1)); // Avoid log(0) issues
			return zMinLine - logD;
		}
	}

	private static double interpolate(double x, double x1, double y1, double x2, double y2) {
		if (x1 == x2) {
			throw new IllegalArgumentException("x1 and x2 cannot be the same (vertical line).");
		}
		// Linear interpolation formula
		return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
	}

	private static double mapValue(double value, double srcMin, double srcMax, double destMin, double destMax) {
		// Linear mapping from [srcMin, srcMax] to [destMin, destMax]
		return destMin + (value - srcMin) * (destMax - destMin) / (srcMax - srcMin);
	}
}
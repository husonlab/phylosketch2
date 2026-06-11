/*
 * RootPosition.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Node;

import java.util.Collection;

/**
 * the root position
 * Daniel Huson, 2025
 *
 * @param side     which side
 * @param location coordinates
 */
public record RootPosition(phylosketch.view.RootPosition.Side side, Point2D location) {
	public enum Side {Top, Bottom, Left, Right, Center}

	/**
	 * compute the root position
	 *
	 * @param nodes nodes
	 * @return root position
	 */
	public static RootPosition compute(Collection<Node> nodes) {
		var minX = nodes.stream().mapToDouble(DrawView::getX).min().orElse(0.0);
		var maxX = nodes.stream().mapToDouble(DrawView::getX).max().orElse(0.0);
		var minY = nodes.stream().mapToDouble(DrawView::getY).min().orElse(0.0);
		var maxY = nodes.stream().mapToDouble(DrawView::getY).max().orElse(0.0);

		var averageRootX = nodes.stream().filter(v -> v.getInDegree() == 0)
				.mapToDouble(DrawView::getX).average().orElse(0.5 * (minX + maxX));
		var averageRootY = nodes.stream().filter(v -> v.getInDegree() == 0)
				.mapToDouble(DrawView::getY).average().orElse(0.5 * (minY + maxY));
		var rootPoint = new Point2D(averageRootX, averageRootY);

		Side side;
		if (nodes.size() <= 1) {
			side = Side.Center;
		} else {
			var sumX = 0.0;
			var sumY = 0.0;
			var count = 0;
			for (var v : nodes) {
				if (v.isLeaf()) {
					var pv = DrawView.getPoint(v);
					for (var u : v.parents()) {
						var dir = pv.subtract(DrawView.getPoint(u)); // parent -> leaf
						var len = dir.magnitude();
						if (len > 0) {
							sumX += dir.getX() / len; // unit vector
							sumY += dir.getY() / len;
							count++;
						}
					}
				}
			}
			if (count == 0) {
				side = Side.Center;
			} else {
				var meanX = sumX / count;
				var meanY = sumY / count;
				var concentration = Math.hypot(meanX, meanY); // ~1 aligned, ~0 radial

				final double MIN_CONCENTRATION = 0.5; // tune
				if (concentration < MIN_CONCENTRATION) {
					side = Side.Center;
				} else if (Math.abs(meanX) >= Math.abs(meanY)) {
					side = (meanX > 0) ? Side.Left : Side.Right; // leaves right => root left
				} else {
					// JavaFX: +y points DOWN the screen
					side = (meanY > 0) ? Side.Top : Side.Bottom;  // leaves below => root on top
				}
			}
		}
		return new RootPosition(side, rootPoint);
	}

	private static double zScore(double value, Collection<Double> values) {
		double mean = values.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
		double std = Math.sqrt(values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0));
		return std == 0.0 ? 0.0 : (value - mean) / std;
	}
}

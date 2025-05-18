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
		var width = (maxX - minX);
		var minY = nodes.stream().mapToDouble(DrawView::getY).min().orElse(0.0);
		var maxY = nodes.stream().mapToDouble(DrawView::getY).max().orElse(0.0);
		var height = (maxY - minY);

		var averageRootX = nodes.stream().filter(v -> v.getInDegree() == 0).mapToDouble(DrawView::getX).average().orElse(0.0);
		var averageRootY = nodes.stream().filter(v -> v.getInDegree() == 0).mapToDouble(DrawView::getY).average().orElse(0.0);

		Side side;

		if (nodes.size() <= 1) {
			side = Side.Center;
		} else {
			var xValues = nodes.stream().filter(v -> v.getOutDegree() == 0).map(DrawView::getX).toList();
			var averageLeafX = xValues.stream().mapToDouble(d -> d).average().orElse(0.0);
			var yValues = nodes.stream().filter(v -> v.getOutDegree() == 0).map(DrawView::getY).toList();
			var averageLeafY = yValues.stream().mapToDouble(d -> d).average().orElse(0.0);

			if (averageLeafX > minX + 0.1 * width && averageLeafX < maxX - 0.1 * width
				&& averageLeafY > minY + 0.1 * height && averageLeafY < maxY - 0.1 * height) {
				side = Side.Center;
			} else {

				var zScoreX = zScore(averageRootX, xValues);
				var zScoreY = zScore(averageRootY, yValues);

				if (Math.abs(zScoreX) > Math.abs(zScoreY)) {
					if (averageLeafX - averageRootX > 0)
						side = Side.Left;
					else
						side = Side.Right;
				} else {
					if (averageLeafY - averageRootY > 0)
						side = Side.Top;
					else
						side = Side.Bottom;
				}
			}
		}
		return new RootPosition(side, new Point2D(averageRootX, averageRootY));
	}

	private static double zScore(double value, Collection<Double> values) {
		double mean = values.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
		double std = Math.sqrt(values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0));
		return std == 0.0 ? 0.0 : (value - mean) / std;
	}
}

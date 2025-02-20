/*
 * ScaleUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.utils;

import javafx.geometry.Point2D;
import jloda.graph.Node;

import java.util.Map;

/**
 * utilities for scaling stuff
 * Daniel Huson, 2.2025
 */
public class ScaleUtils {
	public static void scaleToBox(Map<Node, Point2D> nodePointMap, double xMin, double xMax, double yMin, double yMax) {
		var pxMin = nodePointMap.values().stream().mapToDouble(Point2D::getX).min().orElse(0);
		var pxMax = nodePointMap.values().stream().mapToDouble(Point2D::getX).max().orElse(0);
		var pyMin = nodePointMap.values().stream().mapToDouble(Point2D::getY).min().orElse(0);
		var pyMax = nodePointMap.values().stream().mapToDouble(Point2D::getY).max().orElse(0);

		for (var v : nodePointMap.keySet()) {
			var point = nodePointMap.get(v);
			var px = point.getX();
			var py = point.getY();

			var x = xMin + (px - pxMin) * (xMax - xMin) / (pxMax - pxMin);

			var y = yMin + (py - pyMin) * (yMax - yMin) / (pyMax - pyMin);

			nodePointMap.put(v, new Point2D(x, y));
		}
	}
}

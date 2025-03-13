/*
 * CircleSegment.java Copyright (C) 2025 Daniel H. Huson
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

import java.util.ArrayList;
import java.util.List;

/**
 * computes the points on a circle segment
 * Daniel Huson, 3.2024
 */
public class CircleSegment {
	/**
	 * computes the points on a circle segment, if start > end, go other way around
	 *
	 * @param center     center of circle
	 * @param radius     radius of circle
	 * @param startAngle start angle
	 * @param endAngle   end angle
	 * @return points in segment
	 */
	public static List<Point2D> apply(Point2D center, double radius, double startAngle, double endAngle) {
		return apply(center, radius, startAngle, endAngle, 5.0);
	}

	/**
	 * computes the points on a circle segment
	 *
	 * @param center     center of circle
	 * @param radius     radius of circle
	 * @param startAngle start angle
	 * @param endAngle   end angle
	 * @param d          distance between points
	 * @return points in segment
	 */
	public static List<Point2D> apply(Point2D center, double radius, double startAngle, double endAngle, double d) {
		List<Point2D> points = new ArrayList<>();

		// Normalize angles to [0, 360)
		startAngle = (startAngle % 360 + 360) % 360;
		endAngle = (endAngle % 360 + 360) % 360;

		// Handle wrapping: if endAngle < startAngle, wrap around the circle
		if (endAngle < startAngle) {
			endAngle += 360;
		}

		// Convert degrees to radians
		double startRadians = Math.toRadians(startAngle);
		double endRadians = Math.toRadians(endAngle);
		double totalAngle = endRadians - startRadians;

		// Calculate the number of segments needed
		double arcLength = radius * totalAngle; // Arc length = r * theta (in radians)
		int numSegments = Math.max(2, (int) Math.ceil(arcLength / d));

		// Generate points
		for (int i = 0; i <= numSegments; i++) {
			double angle = startRadians + (i * totalAngle / numSegments);
			double x = center.getX() + radius * Math.cos(angle);
			double y = center.getY() + radius * Math.sin(angle);
			points.add(new Point2D(x, y));
		}

		return points;
	}
}

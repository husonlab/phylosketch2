/*
 * CubicCurve.java Copyright (C) 2025 Daniel H. Huson
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
 * computes points on a cubic curve
 * Daniel Huson, 2.2025
 */
public class CubicCurve {

	public static List<Point2D> apply(Point2D start, Point2D control1, Point2D control2, Point2D end, double delta) {
		var points = new ArrayList<Point2D>();
		points.add(start);

		var totalLength = estimateCurveLength(start, control1, control2, end);
		int numPoints = Math.max(2, (int) (totalLength / delta));

		for (var i = 1; i <= numPoints; i++) {
			var t = (double) i / numPoints;
			points.add(getPointOnCurve(start, control1, control2, end, t));
		}

		return points;
	}

	private static Point2D getPointOnCurve(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
		var x = Math.pow(1 - t, 3) * p0.getX() +
				3 * Math.pow(1 - t, 2) * t * p1.getX() +
				3 * (1 - t) * Math.pow(t, 2) * p2.getX() +
				Math.pow(t, 3) * p3.getX();

		var y = Math.pow(1 - t, 3) * p0.getY() +
				3 * Math.pow(1 - t, 2) * t * p1.getY() +
				3 * (1 - t) * Math.pow(t, 2) * p2.getY() +
				Math.pow(t, 3) * p3.getY();

		return new Point2D(x, y);
	}

	private static double estimateCurveLength(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
		var chord = p0.distance(p3);
		var controlNet = p0.distance(p1) + p1.distance(p2) + p2.distance(p3);
		return (chord + controlNet) / 2.0;
	}
}


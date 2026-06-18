/*
 * QuadraticCurve.java Copyright (C) 2025 Daniel H. Huson
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
 * computes points that lie on a quadratic curve
 */
public class QuadraticCurve {
	/**
	 * compute the curve
	 *
	 * @param start   start node
	 * @param control control node
	 * @param end     end node
	 * @return points in curve
	 */
	public static List<Point2D> apply(Point2D start, Point2D control, Point2D end) {
		return apply(start, control, end, 5.0);
	}

	/**
	 * compute the curve
	 *
	 * @param start   start node
	 * @param control control node
	 * @param end     end node
	 * @param delta   min distance between consecutive points
	 * @return points in curve
	 */
	public static List<Point2D> apply(Point2D start, Point2D control, Point2D end, double delta) {
		var points = new ArrayList<Point2D>();

		points.add(start);

		var t = 0.0;
		var step = 0.01;

		while (t < 1.0) {
			var currentPoint = calculateQuadraticBezierPoint(start, control, end, t);
			var previousPoint = points.get(points.size() - 1);
			var distance = currentPoint.distance(previousPoint);

			if (distance >= delta) {
				points.add(currentPoint);
			}

			t += step;
		}
		points.add(end);

		return points;
	}

	private static Point2D calculateQuadraticBezierPoint(Point2D start, Point2D control, Point2D end, double t) {
		var x = (1 - t) * (1 - t) * start.getX() + 2 * (1 - t) * t * control.getX() + t * t * end.getX();
		var y = (1 - t) * (1 - t) * start.getY() + 2 * (1 - t) * t * control.getY() + t * t * end.getY();
		return new Point2D(x, y);
	}

	/**
	 * compute the control point for a bow edge, with 5% offset
	 *
	 * @param start start
	 * @param end   end
	 * @return control point
	 */
	public static Point2D computeControlForBowEdge(Point2D start, Point2D end) {
		return computeControlForBowEdge(start, end, 0.05);
	}

	/**
	 * compute the control point for a bow edge, with a given height (as proportion of distance)
	 *
	 * @param start        start
	 * @param end          end
	 * @param offsetFactor height as proportion of distance
	 * @return control point
	 */
	public static Point2D computeControlForBowEdge(Point2D start, Point2D end, double offsetFactor) {
		var midpoint = start.midpoint(end);
		var d = start.distance(end);
		var dx = end.getX() - start.getX();
		var dy = end.getY() - start.getY();
		var perpendicular = new Point2D(-dy / d, dx / d);
		var offset = offsetFactor * d;
		return midpoint.add(perpendicular.multiply(offset));
	}
}
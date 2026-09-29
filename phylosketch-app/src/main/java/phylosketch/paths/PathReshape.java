/*
 * PathReshape.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.paths;

import javafx.geometry.Point2D;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;

import java.util.ArrayList;

import static phylosketch.paths.PathUtils.extractPoints;
import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * path reshaping
 * Daniel Huson, 9.2024
 */
public class PathReshape {
	/**
	 * reshape a path using coordinate changes for a given element
	 *
	 * @param path  the path
	 * @param index the element index
	 * @param dx    change in x coordinate
	 * @param dy    change in y coordinate
	 */
	public static void apply(EdgePath path, int index, double dx, double dy) {
		var n = path.getElements().size();

		if (index < 0 || index >= n)
			throw new IndexOutOfBoundsException();

		var factor = computeScalingFactors(path, index);
		var elements = new ArrayList<PathElement>();
		for (var i = 0; i < n; i++) {
			var point = getCoordinates(path.getElements().get(i));
			var newPoint = point.add(factor[i] * dx, factor[i] * dy);
			if (i == 0) {
				elements.add(new MoveTo(newPoint.getX(), newPoint.getY()));
			} else {
				elements.add(new LineTo(newPoint.getX(), newPoint.getY()));
			}
		}
		path.getElements().setAll(elements);
	}

	public static void apply(Path path, double dx, double dy) {
		path.getElements().setAll(PathUtils.createElements(extractPoints(path).stream().map(p -> new Point2D(p.getX() + dx, p.getY() + dy)).toList()));
	}

	/**
	 * reshapes a copy of a path whose two ends move by different amounts, as when the two nodes of an edge are moved
	 * separately. Moved by the same amount, any path is simply translated. Otherwise, a straight, rectangular or
	 * quadratic path keeps its type, and a corner or control point that lines up with the two ends, as in a
	 * rectangular layout, still does. Any other path becomes freeform, each point moving by a mix of the two amounts,
	 * see {@link #move}, so that the horizontal and vertical stretches of a hand-drawn rectangular edge stay so
	 *
	 * @param path       the path
	 * @param startDelta how far the start of the path moves
	 * @param endDelta   how far the end of the path moves
	 * @return the reshaped copy
	 */
	public static EdgePath moveEnds(EdgePath path, Point2D startDelta, Point2D endDelta) {
		var result = path.copy();
		var n = path.getElements().size();
		if (n < 2)
			return result;
		if (startDelta.equals(endDelta)) {
			if (!startDelta.equals(Point2D.ZERO))
				result.set(PathTransforms.translate(path, startDelta.getX(), startDelta.getY()).getElements(), path.getType());
			return result;
		}
		var start = getCoordinates(path.getElements().get(0));
		var end = getCoordinates(path.getElements().get(n - 1));
		switch (path.getType()) {
			case Straight -> {
				if (n == 2) {
					result.setStraight(start.add(startDelta), end.add(endDelta));
					return result;
				}
			}
			case Rectangular -> {
				if (n == 3) {
					var corner = getCoordinates(path.getElements().get(1));
					result.setRectangular(start.add(startDelta), moveCorner(corner, start, end, startDelta, endDelta), end.add(endDelta));
					return result;
				}
			}
			case QuadCurve -> {
				if (n == 2 && path.getElements().get(1) instanceof QuadCurveTo quadCurveTo) {
					var control = new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY());
					result.setQuadCurve(start.add(startDelta), moveCorner(control, start, end, startDelta, endDelta), end.add(endDelta));
					return result;
				}
			}
		}
		var points = PathUtils.getPoints(path.copyToFreeform());
		var length = new double[points.size()];
		for (var i = 1; i < points.size(); i++)
			length[i] = length[i - 1] + points.get(i).distance(points.get(i - 1));
		var total = length[points.size() - 1];
		var moved = new ArrayList<Point2D>();
		for (var i = 0; i < points.size(); i++) {
			var t = (total > 0 ? length[i] / total : (double) i / Math.max(1, points.size() - 1));
			moved.add(move(points.get(i), t, start, end, startDelta, endDelta));
		}
		result.setFreeform(moved);
		return result;
	}

	/**
	 * moves a corner or control point along with the ends of its path. One that lines up with the start in x and with
	 * the end in y, or the other way round, keeps doing so; any other moves as a point halfway along the path
	 */
	private static Point2D moveCorner(Point2D corner, Point2D start, Point2D end, Point2D startDelta, Point2D endDelta) {
		if (Math.abs(corner.getX() - start.getX()) < 0.01 && Math.abs(corner.getY() - end.getY()) < 0.01)
			return corner.add(startDelta.getX(), endDelta.getY());
		else if (Math.abs(corner.getX() - end.getX()) < 0.01 && Math.abs(corner.getY() - start.getY()) < 0.01)
			return corner.add(endDelta.getX(), startDelta.getY());
		else
			return move(corner, 0.5, start, end, startDelta, endDelta);
	}

	/**
	 * moves a point of a path whose ends move by different amounts, separately in x and in y: a coordinate equal to
	 * that of the start moves by the amount of the start, one equal to that of the end by the amount of the end, and
	 * one in between by a mix in proportion. As points with the same x move alike in x, and those with the same y
	 * alike in y, horizontal and vertical stretches, as in a rectangular edge, stay so. Where both ends share a
	 * coordinate, the mix in it follows how far along the path the point lies instead
	 *
	 * @param t how far along the path the point lies, from 0 at the start to 1 at the end
	 */
	private static Point2D move(Point2D point, double t, Point2D start, Point2D end, Point2D startDelta, Point2D endDelta) {
		var u = proportion(point.getX(), start.getX(), end.getX(), t);
		var w = proportion(point.getY(), start.getY(), end.getY(), t);
		return point.add((1 - u) * startDelta.getX() + u * endDelta.getX(), (1 - w) * startDelta.getY() + w * endDelta.getY());
	}

	private static double proportion(double value, double from, double to, double t) {
		if (Math.abs(to - from) < 0.01)
			return t;
		else
			return Math.max(0, Math.min(1, (value - from) / (to - from)));
	}

	/**
	 * computes scaling factors for different indices ranging 1 for the given index and 0 for the getLeft- and rightmost
	 * points
	 *
	 * @param path  the path
	 * @param index the index of the point that is to be moved
	 * @return scaling factors for the movement of each of the points
	 */
	private static double[] computeScalingFactors(Path path, int index) {
		var points = PathUtils.extractPoints(path);
		var n = points.size();

		var factor = new double[n];
		{
			for (var i = 0; i < index; i++) {
				factor[i + 1] = factor[i] + points.get(i + 1).distance(points.get(i));
			}
			for (var i = 1; i < index; i++) {
				factor[i] /= factor[index];
			}
			factor[index] = 1;
			for (var i = index; i < n - 1; i++) {
				factor[i + 1] = factor[i] + points.get(i + 1).distance(points.get(i));
			}
			for (var i = index + 1; i < n; i++) {
				factor[i] = 1 - factor[i] / factor[n - 1];
			}
		}
		return factor;
	}
}

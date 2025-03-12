/*
 * PathUtils.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class PathUtils {
	public static ArrayList<PathElement> toPathElements(List<Point2D> points) {
		var result = new ArrayList<PathElement>();
		for (var i = 0; i < points.size(); i++) {
			if (i == 0)
				result.add(new MoveTo(points.get(i).getX(), points.get(i).getY()));
			else
				result.add(new LineTo(points.get(i).getX(), points.get(i).getY()));
		}
		return result;
	}

	public static ArrayList<Point2D> getPoints(Path path) {
		return new ArrayList<>(path.getElements().stream().map(PathUtils::getCoordinates).toList());
	}

	public static List<PathElement> copy(List<PathElement> elements) {
		var result = new ArrayList<PathElement>();
		for (var element : elements) {
			if (element instanceof MoveTo to) {
				result.add(new MoveTo(to.getX(), to.getY()));
			} else if (element instanceof LineTo to) {
				result.add(new LineTo(to.getX(), to.getY()));
			}
		}
		return result;
	}

	public static Path copy(Path path) {
		return createPath(extractPoints(path), false);
	}

	public static List<Point2D> extractPoints(Path path) {
		var points = new ArrayList<Point2D>();
		for (var element : path.getElements()) {
			points.add(getCoordinates(element));
		}
		return points;
	}

	public static Point2D getCoordinates(PathElement pathElement) {
		if (pathElement instanceof MoveTo moveTo) {
			return new Point2D(moveTo.getX(), moveTo.getY());
		} else if (pathElement instanceof LineTo lineTo) {
			return new Point2D(lineTo.getX(), lineTo.getY());
		} else {
			return new Point2D(0, 0);
		}
	}

	public static Path createPath(List<Point2D> points, boolean normalize) {
		var path = new Path();
		path.getStyleClass().add("graph-edge");
		path.getElements().addAll(createElements(points));
		if (normalize)
			path.getElements().setAll(PathNormalize.apply(path, 2, 5));
		return path;
	}

	public static List<PathElement> createElements(List<Point2D> points) {
		var list = new ArrayList<PathElement>();
		for (var point : points) {
			if (list.isEmpty())
				list.add(new MoveTo(point.getX(), point.getY()));
			else
				list.add(new LineTo(point.getX(), point.getY()));
		}
		return list;
	}

	public static Point2D getMiddle(Path path) {
		if (path.getElements().isEmpty())
			return null;
		else {
			var points = extractPoints(path);
			var first = points.get(0);
			var last = points.get(points.size() - 1);
			var best = 0;
			var bestDistance = 0.0;
			for (int i = 1; i < points.size() - 1; i++) {
				var point = points.get(i);
				var d = Math.min(point.distance(first), point.distance(last));
				if (d > bestDistance) {
					bestDistance = d;
					best = i;
				}
			}
			return points.get(best);
		}
	}


	public static void reverse(Path path) {
		var points = extractPoints(path);
		points = CollectionUtils.reverse(points);
		path.getElements().setAll(createPath(points, false).getElements());
	}

	/**
	 * split path into two parts
	 *
	 * @param path   path
	 * @param aPoint point along path
	 * @return two paths
	 */
	public static List<List<Point2D>> split(Path path, Point2D aPoint) {
		var points = extractPoints(path);
		var firstIndex = 0;
		var bestFirstDistance = Double.MAX_VALUE;
		for (var i = 0; i < points.size(); i++) {
			var firstDistance = points.get(i).distance(aPoint);
			if (firstDistance < bestFirstDistance) {
				bestFirstDistance = firstDistance;
				firstIndex = i;
			}
		}
		return split(path, true, firstIndex);
	}

	public static Point2D nudgeOntoPath(Path path, Point2D aPoint) {
		var points = extractPoints(path);
		var bestIndex = 0;
		var bestFirstDistance = Double.MAX_VALUE;
		for (var i = 0; i < points.size(); i++) {
			var firstDistance = points.get(i).distance(aPoint);
			if (firstDistance < bestFirstDistance) {
				bestFirstDistance = firstDistance;
				bestIndex = i;
			}
		}
		return points.get(bestIndex);
	}

	/**
	 * split path into three parts
	 *
	 * @param path   the path
	 * @param aPoint first point along path
	 * @param bPoint second point along path
	 * @return three paths
	 */
	public static List<List<Point2D>> split(Path path, Point2D aPoint, Point2D bPoint) {
		var points = extractPoints(path);
		var firstIndex = 0;
		var bestFirstDistance = Double.MAX_VALUE;
		var secondIndex = 0;
		var bestSecondDistance = Double.MAX_VALUE;
		for (var i = 0; i < points.size(); i++) {
			var firstDistance = points.get(i).distance(aPoint);
			if (firstDistance < bestFirstDistance) {
				bestFirstDistance = firstDistance;
				firstIndex = i;
			}
			var secondDistance = points.get(i).distance(bPoint);

			if (secondDistance < bestSecondDistance) {
				bestSecondDistance = secondDistance;
				secondIndex = i;
			}
		}
		return split(path, true, firstIndex, secondIndex);
	}


	public static List<List<Point2D>> split(Path path, boolean normalize, int... elementIndices) {
		var points = extractPoints(path);
		var list = new ArrayList<List<Point2D>>();
		var prev = 0;
		for (int index : elementIndices) {
			list.add(new ArrayList<>(points.subList(prev, index)));
			prev = index;
		}
		list.add(new ArrayList<>(points.subList(prev, points.size())));
		return list;
	}

	public static Path concatenate(Path path1, Path path2, boolean normalize) {
		return createPath(CollectionUtils.concatenate(extractPoints(path1), extractPoints(path2)), normalize);
	}

	public static Point2D getPointAwayFromEnd(Path path, double minDistance) {
		var points = CollectionUtils.reverse(extractPoints(path));
		var start = points.get(0);
		for (var point : points) {
			if (point.distance(start) > minDistance) {
				return point;
			}
		}
		return points.get(points.size() - 1);
	}

	public static Point2D getPointAwayFromStart(Path path, double minDistance) {
		var points = extractPoints(path);
		var start = points.get(0);
		for (var point : points) {
			if (point.distance(start) > minDistance) {
				return point;
			}
		}
		return points.get(points.size() - 1);
	}
}

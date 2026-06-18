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
import javafx.scene.shape.*;
import jloda.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class PathUtils {

	public static ArrayList<Point2D> getPoints(Path path) {
		return new ArrayList<>(path.getElements().stream().map(PathUtils::getCoordinates).toList());
	}

	public static List<PathElement> copy(List<PathElement> elements) {
		var result = new ArrayList<PathElement>();
		for (var element : elements) {
			result.add(copyElement(element));
		}
		return result;
	}

	public static void copy(Path source, Path target) {
		target.getElements().setAll(copy(source.getElements()));
		copyProperties(source, target);
	}

	public static void copyProperties(Path source, Path target) {
		target.setFill(source.getFill());
		target.setStroke(source.getStroke());
		target.setStrokeWidth(source.getStrokeWidth());
		target.getStrokeDashArray().setAll(source.getStrokeDashArray());
		target.setStrokeLineCap(source.getStrokeLineCap());
		target.setStrokeLineJoin(source.getStrokeLineJoin());
		target.setStrokeMiterLimit(source.getStrokeMiterLimit());
		target.setEffect(source.getEffect());
	}

	public static Path copy(Path source) {
		var target = new Path();
		copy(source, target);
		return target;
	}

	public static PathElement copyElement(PathElement element) {
		if (element instanceof MoveTo m) {
			return new MoveTo(m.getX(), m.getY());
		}
		if (element instanceof LineTo l) {
			return new LineTo(l.getX(), l.getY());
		}
		if (element instanceof QuadCurveTo q) {
			return new QuadCurveTo(q.getControlX(), q.getControlY(), q.getX(), q.getY());
		}
		if (element instanceof CubicCurveTo c) {
			return new CubicCurveTo(
					c.getControlX1(), c.getControlY1(),
					c.getControlX2(), c.getControlY2(),
					c.getX(), c.getY());
		}
		if (element instanceof ArcTo a) {
			ArcTo arc = new ArcTo();
			arc.setX(a.getX());
			arc.setY(a.getY());
			arc.setRadiusX(a.getRadiusX());
			arc.setRadiusY(a.getRadiusY());
			arc.setXAxisRotation(a.getXAxisRotation());
			arc.setLargeArcFlag(a.isLargeArcFlag());
			arc.setSweepFlag(a.isSweepFlag());
			return arc;
		}
		if (element instanceof ClosePath) {
			return new ClosePath();
		}

		throw new IllegalArgumentException("Unknown PathElement: " + element);
	}

	public static List<Point2D> extractPoints(Path path) {
		var points = new ArrayList<Point2D>();
		for (var element : path.getElements()) {
			if (element instanceof MoveTo moveTo) {
				points.add(new Point2D(moveTo.getX(), moveTo.getY()));
			} else if (element instanceof LineTo lineTo) {
				points.add(new Point2D(lineTo.getX(), lineTo.getY()));
			} else if (element instanceof QuadCurveTo quadCurveTo) {
				points.add(new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY()));
				points.add(new Point2D(quadCurveTo.getX(), quadCurveTo.getY()));
			} else if (element instanceof CubicCurveTo cubicCurveTo) {
				points.add(new Point2D(cubicCurveTo.getControlX1(), cubicCurveTo.getControlY1()));
				points.add(new Point2D(cubicCurveTo.getControlX2(), cubicCurveTo.getControlY2()));
				points.add(new Point2D(cubicCurveTo.getX(), cubicCurveTo.getY()));
			} else if (element instanceof ArcTo arcTo) {
				points.add(new Point2D(arcTo.getX(), arcTo.getY()));
			}
		}
		return points;
	}

	public static Point2D getCoordinates(PathElement element) {
		if (element instanceof MoveTo moveTo) {
			return new Point2D(moveTo.getX(), moveTo.getY());
		} else if (element instanceof LineTo lineTo) {
			return new Point2D(lineTo.getX(), lineTo.getY());
		} else if (element instanceof QuadCurveTo quadCurveTo) {
			return new Point2D(quadCurveTo.getX(), quadCurveTo.getY());
		} else if (element instanceof CubicCurveTo cubicCurveTo) {
			return new Point2D(cubicCurveTo.getX(), cubicCurveTo.getY());
		} else if (element instanceof ArcTo arcTo) {
			return new Point2D(arcTo.getX(), arcTo.getY());
		} else {
			return new Point2D(0, 0);
		}
	}

	public static Path createPath(List<Point2D> points, boolean normalize) {
		var path = new Path();
		path.getStyleClass().add("graph-edge");
		path.getElements().addAll(createElements(points, normalize));
		return path;
	}

	public static List<PathElement> createElements(List<Point2D> points) {
		return createElements(points, false);
	}

	public static List<PathElement> createElements(List<Point2D> points, boolean normalize) {
		if (normalize)
			points = PathNormalize.apply(points, 2, 5);
		var list = new ArrayList<PathElement>();
		for (var point : points) {
			if (list.isEmpty())
				list.add(new MoveTo(point.getX(), point.getY()));
			else
				list.add(new LineTo(point.getX(), point.getY()));
		}
		return list;
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
		return split(path, firstIndex);
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
		return split(path, firstIndex, secondIndex);
	}


	public static List<List<Point2D>> split(Path path, int... elementIndices) {
		var points = extractPoints(path);
		var list = new ArrayList<List<Point2D>>();
		var prev = 0;
		for (int index : elementIndices) {
			list.add(new ArrayList<>(points.subList(prev, index + 1)));
			prev = index;
		}
		list.add(new ArrayList<>(points.subList(prev, points.size())));
		return list;
	}

	public static Path concatenate(Path path1, Path path2, boolean normalize) {
		return createPath(CollectionUtils.concatenate(extractPoints(path1), extractPoints(path2)), normalize);
	}

	public static Point2D getPointAwayFromEnd(EdgePath path, double minDistance) {
		if (path.getType() == EdgePath.Type.Straight) {
			var points = extractPoints(path);
			return pointAtDistanceFromB(points.get(0), points.get(1), minDistance);
		} else if (path.getType() == EdgePath.Type.Rectangular) {
			var points = extractPoints(path);
			return pointAtDistanceFromB(points.get(1), points.get(2), minDistance);
		} else {
			var workingPath = (path.getType() != EdgePath.Type.Freeform ? path.copyToFreeform() : path);
			var points = extractPoints(workingPath);
			if (points.size() == 1)
				return points.get(0);
			var lastId = points.size() - 1;
			var last = points.get(lastId);
			var firstId = lastId - 1;
			var first = points.get(firstId);
			while (firstId > 0 && first.distance(last) < minDistance) {
				first = points.get(--firstId);
			}
			return first;
		}
	}

	public static Point2D getPointAwayFromStart(EdgePath path, double minDistance) {
		if (path.getType() == EdgePath.Type.Straight) {
			var points = extractPoints(path);
			return pointAtDistanceFromB(points.get(1), points.get(0), minDistance);
		} else if (path.getType() == EdgePath.Type.Rectangular) {
			var points = extractPoints(path);
			return pointAtDistanceFromB(points.get(2), points.get(1), minDistance);
		} else {
			var workingPath = (path.getType() != EdgePath.Type.Freeform ? path.copyToFreeform() : path);
			var points = extractPoints(workingPath);
			if (points.size() == 1)
				return points.get(0);
			var first = points.get(0);
			var lastId = 1;
			var last = points.get(lastId);
			while (lastId < points.size() - 1 && first.distance(last) < minDistance) {
				last = points.get(++lastId);
			}
			return last;
		}
	}

	private static Point2D pointAtDistanceFromB(Point2D a, Point2D b, double d) {
		var v = a.subtract(b);
		var len = a.distance(b);
		return len == 0 ? a : b.add(v.multiply(d / len));
	}
}

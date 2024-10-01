/*
 * PathUtils.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch2.paths;

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
		for (var point : points) {
			if (path.getElements().isEmpty())
				path.getElements().add(new MoveTo(point.getX(), point.getY()));
			else
				path.getElements().add(new LineTo(point.getX(), point.getY()));

		}
		if (normalize)
			path.getElements().setAll(PathNormalize.apply(path, 2, 5));
		return path;
	}

	public static Point2D getMiddle(Path path) {
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


	public static void reverse(Path path) {
		var points = extractPoints(path);
		points = CollectionUtils.reverse(points);
		path.getElements().setAll(createPath(points, false).getElements());
	}
}

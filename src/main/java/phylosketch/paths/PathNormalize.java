/*
 * PathNormalize.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import jloda.graph.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * path refinement
 */
public class PathNormalize {

	/**
	 * normalizes path by ensuring that all consecutive points have distance between dMin and dMax
	 *
	 * @param path the original path
	 * @param dMin min distance between consecutive points
	 * @param dMax max distance between consecutive points
	 * @return normalized points
	 */
	public static List<PathElement> apply(Path path, double dMin, double dMax) {
		if (dMin >= dMax)
			throw new IllegalArgumentException();
		var points = PathUtils.getPoints(path);
		points = refine(points, dMax);
		points = reduce(points, dMin);
		return PathUtils.toPathElements(points);
	}

	/**
	 * adds new points to path to ensure that no two nodes are not too far apart
	 *
	 * @param points the original points
	 * @param dMax   the max distance between points
	 * @return the new points
	 */
	public static ArrayList<Point2D> refine(List<Point2D> points, double dMax) {
		var result = new ArrayList<>(points);
		for (var i = 0; i + 1 < result.size(); i++) {
			var p = result.get(i);
			var q = result.get(i + 1);
			var distance = p.distance(q);
			if (distance > dMax) {
				var center = p.add((q.subtract(p)).multiply(dMax / distance));
				result.add(i + 1, center);
			}
		}
		return result;
	}

	/**
	 * reduces superfluous points from path to ensure that no two nodes are  too isCloseTo together
	 *
	 * @param points the original points
	 * @param dMin   the max distance between points
	 * @return the new points
	 */
	public static ArrayList<Point2D> reduce(List<Point2D> points, double dMin) {
		var newPoints = new ArrayList<>(points);
		for (var i = 0; i < newPoints.size() - 3; i++) {
			var p = newPoints.get(i);
			var q = newPoints.get(i + 1);
			if (p.distance(q) < dMin) {
				newPoints.remove(i + 1);
			}
		}
		return newPoints;
	}

	public static void normalizeEdges(Collection<Edge> edges) {
		for (var e : edges) {
			if (e.getData() instanceof Path path) {
				path.getElements().setAll(apply(path, 2, 5));
			}
		}
	}
}

/*
 * PathSmoother.java Copyright (C) 2024 Daniel H. Huson
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

import java.util.ArrayList;
import java.util.List;

/**
 * path modification algorithms
 */
public class PathSmoother {
	/**
	 * use  Chaikin's algorithm to smooth a path
	 *
	 * @param points     the original points
	 * @param iterations number of iterations
	 * @return new points
	 */
	public static List<Point2D> apply(List<Point2D> points, int iterations) {
		for (var iter = 0; iter < iterations; iter++) {
			var newPoints = new ArrayList<Point2D>();
			if (points.size() < 2) return points;

			newPoints.add(points.get(0));  // Keep the first point

			for (var i = 0; i < points.size() - 1; i++) {
				var P = points.get(i);
				var PNext = points.get(i + 1);
				var Q = P.multiply(0.75).add(PNext.multiply(0.25));
				var R = P.multiply(0.25).add(PNext.multiply(0.75));
				newPoints.add(Q);
				newPoints.add(R);
			}

			newPoints.add(points.get(points.size() - 1));

			points = PathNormalize.reduce(newPoints, 3);
		}
		points = PathNormalize.refine(points, 7);
		return points;
	}

	public static void apply(Path path, int iterations) {
		var points = PathUtils.getPoints(path);
		path.getElements().setAll(PathUtils.toPathElements(apply(points, iterations)));
	}
}
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
	 * computes scaling factors for different indices ranging 1 for the given index and 0 for the left- and rightmost
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

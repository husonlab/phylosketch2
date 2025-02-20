/*
 * ProcessingUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.capture;

import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * processing utils
 * Daniel Huson, 1.2025
 */
public class ProcessingUtils {

	public static List<Point> removeClosePoints(List<Point> points, double minDistance) {
		var list = new ArrayList<Point>();
		for (var point : points) {
			if (list.stream().noneMatch(other -> point.distance(other) < minDistance)) {
				list.add(point);
			}
		}
		return list;
	}

	private static final int ID_OFFSET = 10;

	public static List<Segment> findPaths(ProgressListener progress, int[][] matrix, List<Point> points) throws CanceledException {
		var list = new ArrayList<Segment>();

		var maxD = 5;
		{
			var id = ID_OFFSET;
			for (Point p : points) {
				matrix[p.y()][p.x()] = id;
				for (var d = 1; d < maxD; d++)
					for (var q : neighborsAll(matrix, p, d)) {
						matrix[q.y()][q.x()] = id;
					}
				id++;
			}
		}

		progress.setMaximum(points.size());
		for (var start : points) {
			var startId = matrix[start.y()][start.x()];
			// System.err.println("start " + startId);

			for (var neighbor : neighbors(matrix, start, maxD, a -> a == 1)) {
				// System.err.println("start " + startId + " neighbor: " + neighbor);
					var segment = new Segment();
					segment.points().add(start);
				var endId = getPointsRec(progress, startId, neighbor, matrix, segment.points());
				// System.err.println("end " + startId + ": " + endId);
				if (endId > 0) {
					var endPoint = points.get(endId - ID_OFFSET);
					segment.points().add(endPoint);
					if (list.stream().noneMatch(s -> s.same(segment, 3)))
							list.add(segment);
					}
				}
			progress.incrementProgress();
		}
		System.err.println(points.size() + " -> " + list.size());
		return list;
	}

	private static int getPointsRec(ProgressListener progress, int startId, Point p, int[][] matrix, ArrayList<Point> path) throws CanceledException {
		progress.checkForCancel();
		var x = p.x();
		var y = p.y();

		var m = matrix[y][x];
		if (m == 1) {
			matrix[y][x] = 2;
			path.add(p);
			for (var depth = 1; depth <= 2; depth++) {
				for (var q : neighbors(matrix, p, depth, a -> a > 0 && a != 2 && a != startId)) {
					var endId = getPointsRec(progress, startId, q, matrix, path);
					if (endId > 0)
						return endId;
				}
			}
			path.remove(p);
			if (false)
				matrix[y][x] = 1;
		} else if (m > 2 && m != startId) {
			return m;
		}
		return 0;
	}

	public static List<Point> neighborsAll(int[][] matrix, Point point, int distance) {
		return neighbors(matrix, point, distance, m -> true);
	}


	public static List<Point> neighbors(int[][] matrix, Point point, int distance, Predicate<Integer> acceptValue) {
		var height = matrix.length;
		var width = matrix[0].length;
		var x = point.x();
		var y = point.y();

		var list = new ArrayList<Point>();
		for (var i = -distance; i <= distance; i++) {
			for (var j = -distance; j <= distance; j++) {
				if ((i == -distance || i == distance || j == -distance || j == distance) && (x + i) >= 0 && (x + i) < width && (y + j) >= 0 && (y + j) < height && acceptValue.test(matrix[y + j][x + i]))
					list.add(new Point(x + i, y + j));
			}
		}
		return list;
	}

	public static List<Point> neighborsValue(int[][] matrix, Point point, int distance) {
		return neighbors(matrix, point, distance, m -> m == 1);
	}
}

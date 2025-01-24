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

package phylocap.capture;

import jloda.util.CanceledException;
import jloda.util.CollectionUtils;
import jloda.util.progress.ProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * processing utils
 * Daniel Huson, 1.2025
 */
public class ProcessingUtils {
	public static void tightenRect(int[][] matrix, java.awt.Rectangle awtRect) {
		var width = matrix.length;
		var height = matrix[0].length;

		var left = Math.max(0, awtRect.x - 3);
		var right = Math.min(width - 1, awtRect.x + awtRect.width + 3);
		var bottom = Math.max(0, awtRect.y - 3);
		var top = Math.min(height - 1, awtRect.y + awtRect.height + 3);

		var oleft = left;
		var oright = right;
		var otop = top;
		var obottom = bottom;

		{
			loop:
			while (left < right) {
				var changes = 0;
				var prev = matrix[left][obottom];
				for (var j = obottom; j <= otop; j++) {
					if (matrix[left][j] != prev) {
						if (++changes == 2)
							break loop;
						prev = matrix[left][j];
					}
				}
				left++;
			}
		}
		{
			loop:
			while (left < right) {
				var changes = 0;
				var prev = matrix[right][obottom];
				for (var j = obottom; j <= otop; j++) {
					if (matrix[right][j] != prev) {
						if (++changes == 2)
							break loop;
						prev = matrix[right][j];
					}
				}
				right--;
			}
		}
		{
			loop:
			while (bottom < top) {
				var changes = 0;
				var prev = matrix[oleft][bottom];
				for (var i = oleft; i <= oright; i++) {
					if (matrix[i][bottom] != prev) {
						if (++changes == 2)
							break loop;
						prev = matrix[i][bottom];
					}
				}
				bottom++;
			}
		}
		{
			loop:
			while (bottom < top) {
				var changes = 0;
				var prev = matrix[oleft][top];
				for (var i = oleft; i <= oright; i++) {
					if (matrix[i][top] != prev)
						if (++changes > 1)
							break loop;
					prev = matrix[i][top];
				}
				top--;
			}
		}
		awtRect.x = left;
		awtRect.width = right - left + 1;
		awtRect.y = bottom;
		awtRect.height = top - bottom + 1;
	}

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
				matrix[p.x()][p.y()] = id;
				for (var d = 1; d < maxD; d++)
					for (var q : neighborsAll(matrix, p, d)) {
						matrix[q.x()][q.y()] = id;
					}
				id++;
			}
		}

		progress.setMaximum(points.size());
		for (var start : points) {
			var startId = matrix[start.x()][start.y()];
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

		var m = matrix[x][y];
		if (m == 1) {
			matrix[x][y] = 2;
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
				matrix[x][y] = 1;
		} else if (m > 2 && m != startId) {
			return m;
		}
		return 0;
	}

	public static List<Point> neighbors(int[][] matrix, Point point, int distance, Predicate<Integer> acceptValue) {
		var width = matrix.length;
		var height = matrix[0].length;
		var x = point.x();
		var y = point.y();

		var list = new ArrayList<Point>();
		for (var i = -distance; i <= distance; i++) {
			for (var j = -distance; j <= distance; j++) {
				if ((i == -distance || i == distance || j == -distance || j == distance) && (x + i) >= 0 && (x + i) < width && (y + j) >= 0 && (y + j) < height && acceptValue.test(matrix[x + i][y + j]))
					list.add(new Point(x + i, y + j));
			}
		}
		return list;
	}

	public static List<Point> neighborsValue1(int[][] matrix, Point point, int distance) {
		return neighbors(matrix, point, distance, m -> m == 1);
	}

	public static List<Point> neighborsAll(int[][] matrix, Point point, int distance) {
		return neighbors(matrix, point, distance, m -> true);
	}


	public static List<Point> detectEndPoints(int[][] matrix) {
		int width = matrix.length;
		int height = matrix[0].length;

		var list = new ArrayList<Point>();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (matrix[x][y] == 1) {
					if (neighborsValue1(matrix, new Point(x, y), 1).size() == 1) {
						// printNeighborhood(matrix, i, j,1);
						list.add(new Point(x, y)); // Endpoint
					}
				}
			}
		}

		return list;
	}

	public static List<Point> detectBranchPoints(int[][] matrix) {
		int rows = matrix.length;
		int cols = matrix[0].length;

		var list = new ArrayList<Point>();

		for (int x = 0; x < rows; x++) {
			for (int y = 0; y < cols; y++) {
				if (matrix[x][y] == 1) {
					var neighbors = neighborsValue1(matrix, new Point(x, y), 2);
					if (countNonAdjacent(matrix, neighbors) >= 3) {
						// printNeighborhood(matrix,x,y,2);
						list.add(new Point(x, y));
					}
				}
			}
		}
		return list;
	}

	private static int countNonAdjacent(int[][] matrix, List<Point> points) {
		var count = 0;
		for (var p : points) {
			var neighbors = neighborsValue1(matrix, p, 1);
			if (!CollectionUtils.intersects(points, neighbors))
				count++;
		}
		return count;
	}

	public static void printNeighborhood(int[][] matrix, int x, int y, int d) {
		System.err.println(" " + x + " " + y + ":");
		for (var i = -d; i <= d; i++) {
			for (var j = -d; j <= d; j++) {
				if (x + i >= 0 && x + i < matrix.length && y + j >= 0 && y + j < matrix[0].length)
					System.err.print(matrix[x + i][y + j]);
				else System.err.print("_");
			}
			System.err.println();
		}
	}
}

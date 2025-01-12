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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public static List<Segment> findPaths(int[][] matrix, List<Point> points) {
		var list = new ArrayList<Segment>();

		var endPoints = new HashSet<>(points);

		for (var start : points) {
			matrix[start.x()][start.y()] = 2;
			for (var depth = 1; depth <= 1; depth++) {
				for (var neighbor : neighbors(matrix, start, depth)) {
					var segment = new Segment();
					segment.points().add(start);
					if (getPointsRec(neighbor, matrix, endPoints, segment.points())) {
						if (list.stream().noneMatch(s -> s.similar(segment, 1)))
							list.add(segment);
					}
				}
			}
		}
		System.err.println(points.size() + " -> " + list.size());
		return list;
	}

	private static boolean getPointsRec(Point p, int[][] matrix, Set<Point> terminalPoints, ArrayList<Point> path) {
		var x = p.x();
		var y = p.y();
		if (matrix[x][y] == 1) {
			matrix[x][y] = 2;
			path.add(p);
			if (terminalPoints.contains(p)) // have reached another terminal point
				return true;
			var hasNeighbor = false;
			for (var depth = 1; depth <= 1; depth++) {
				for (var q : neighbors(matrix, p, depth)) {
					if (getPointsRec(q, matrix, terminalPoints, path))
						return true;
					hasNeighbor = true;
				}
			}
			if (!hasNeighbor)
				return true;
			matrix[x][y] = 1;
			path.remove(p);
		}
		return false;
	}

	public static List<Point> neighbors(int[][] matrix, Point point, int distance) {
		var width = matrix.length;
		var height = matrix[0].length;
		var x = point.x();
		var y = point.y();

		var list = new ArrayList<Point>();
		for (var i = -distance; i <= distance; i++) {
			for (var j = -distance; j <= distance; j++) {
				if ((i == -distance || i == distance || j == -distance || j == distance) && (x + i) >= 0 && (x + i) < width && (y + j) >= 0 && (y + j) < height && matrix[x + i][y + j] == 1)
					list.add(new Point(x + i, y + j));
			}
		}
		return list;
	}

	public static List<Point> detectEndPoints(int[][] matrix) {
		int width = matrix.length;
		int height = matrix[0].length;

		var list = new ArrayList<Point>();

		int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
		int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (matrix[i][j] == 1) {
					int neighborCount = 0;

					for (int k = 0; k < dx.length; k++) {
						int ni = i + dx[k];
						int nj = j + dy[k];

						// Check bounds and if the neighbor is part of the line
						if (ni >= 0 && ni < width && nj >= 0 && nj < height && matrix[ni][nj] == 1) {
							neighborCount++;
						}
					}
					if (neighborCount == 1) {
						list.add(new Point(i, j)); // Endpoint
					}
				}
			}
		}

		return list;
	}

	public static List<Point> detectBranchPoints(int[][] binaryImage) {
		int rows = binaryImage.length;
		int cols = binaryImage[0].length;

		var list = new ArrayList<Point>();

		// Neighboring offsets (8-connected neighbors)
		int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
		int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (binaryImage[i][j] == 1) { // Only process line pixels
					int neighborCount = 0;

					for (int k = 0; k < dx.length; k++) {
						int ni = i + dx[k];
						int nj = j + dy[k];

						// Check bounds and if the neighbor is part of the line
						if (ni >= 0 && ni < rows && nj >= 0 && nj < cols && binaryImage[ni][nj] == 1) {
							neighborCount++;
						}
					}

					// Check if the pixel is a branch point
					if (neighborCount >= 3) {
						list.add(new Point(i, j));
					}
				}
			}
		}
		return list;
	}
}

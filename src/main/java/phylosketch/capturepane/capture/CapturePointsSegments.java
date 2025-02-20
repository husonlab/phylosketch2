/*
 * CapturePointsSegments.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.util.CollectionUtils;
import jloda.util.progress.ProgressListener;

import java.util.ArrayList;
import java.util.List;

public class CapturePointsSegments {

	public static void apply(ProgressListener progress, int[][] matrix, int minDistancePoints, List<Point> endPoints, List<Segment> segments) throws CanceledException {
		endPoints.clear();
		segments.clear();

		Skeletonization.apply(matrix);

		var points = detectEndPoints(matrix);
		points.addAll(detectBranchPoints(matrix));

		if (minDistancePoints > 0)
			points = ProcessingUtils.removeClosePoints(points, minDistancePoints);

		var sorted = new ArrayList<>(ProcessingUtils.findPaths(progress, matrix, points));
		sorted.sort((a, b) -> -Double.compare(a.first().distance(a.last()), b.first().distance(b.last())));
		for (var segment : sorted) {
			if (segments.stream().noneMatch(that -> that.contains(segment, 3)))
				segments.add(segment);
		}

		endPoints.addAll(points);
	}


	public static List<Point> detectEndPoints(int[][] matrix) {
		int height = matrix.length;
		int width = matrix[0].length;

		var list = new ArrayList<Point>();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (matrix[y][x] == 1) {
					if (ProcessingUtils.neighborsValue(matrix, new Point(x, y), 1).size() == 1) {
						// printNeighborhood(matrix, i, j,1);
						list.add(new Point(x, y)); // Endpoint
					}
				}
			}
		}

		return list;
	}

	public static List<Point> detectBranchPoints(int[][] matrix) {
		int height = matrix.length;
		int width = matrix[0].length;

		var list = new ArrayList<Point>();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (var distance = 1; distance <= 4; distance++) {
					if (matrix[y][x] == 1) {
						var neighbors = ProcessingUtils.neighborsValue(matrix, new Point(x, y), distance);
						if (countNonAdjacent(matrix, neighbors) >= 3) {
							// printNeighborhood(matrix,x,y,2);
							list.add(new Point(x, y));
							break;
						}
					}
				}
			}
		}
		return list;
	}

	private static int countNonAdjacent(int[][] matrix, List<Point> points) {
		var count = 0;
		for (var p : points) {
			var neighbors = ProcessingUtils.neighborsValue(matrix, p, 1);
			if (!CollectionUtils.intersects(points, neighbors))
				count++;
		}
		return count;
	}
}

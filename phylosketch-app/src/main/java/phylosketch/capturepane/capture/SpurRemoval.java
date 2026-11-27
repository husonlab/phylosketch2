/*
 * SpurRemoval.java Copyright (C) 2026 Daniel H. Huson
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
 */package phylosketch.capturepane.capture;

import java.util.ArrayList;

/**
 * removes from a skeleton the short dead ends that thinning leaves inside solid shapes
 * <p>
 * The skeleton of a line is the line. The skeleton of a solid shape, such as an arrowhead, a node drawn as a disc
 * or a thick corner, has a branch into every corner of the shape, and each such branch would be traced as an
 * edge to a leaf. A dead end is such a branch if it is no longer than two and a half times the half-width of the ink at
 * the point where it branches off: out of a thin line even a short dead end is a real line, out of the middle of
 * an arrowhead a branch of that length does not leave the arrowhead.
 * Daniel Huson, 11.2026
 */
public class SpurRemoval {
	// a dead end is removed if its length is at most this multiple of the half-width of the ink where it branches off, plus a few pixels
	private static final double LENGTH_PER_HALF_WIDTH = 2.5;
	private static final int LENGTH_EXTRA = 2;

	/**
	 * removes the spurs
	 *
	 * @param skeleton the skeleton, indexed [y][x], changed in place
	 * @param ink      the ink that the skeleton was thinned from
	 * @return the number of dead ends removed
	 */
	public static int apply(int[][] skeleton, int[][] ink) {
		var height = skeleton.length;
		if (height < 3)
			return 0;
		var width = skeleton[0].length;
		var halfWidth = distanceToBackground(ink);

		var removed = 0;
		var path = new ArrayList<int[]>();
		for (var round = 0; round < 3; round++) { // removing one dead end may leave another
			var before = removed;
			for (var y0 = 1; y0 < height - 1; y0++) {
				for (var x0 = 1; x0 < width - 1; x0++) {
					if (skeleton[y0][x0] == 0 || countNeighbours(skeleton, x0, y0) != 1)
						continue;
					// walk from the dead end to where it branches off
					path.clear();
					var x = x0;
					var y = y0;
					var px = -1;
					var py = -1;
					var branchX = -1;
					var branchY = -1;
					var limit = 4 * (width + height);
					while (path.size() < limit) {
						if (path.size() > 0 && countArms(skeleton, x, y) >= 3) { // here lines meet
							branchX = x;
							branchY = y;
							break;
						}
						path.add(new int[]{x, y});
						// the next pixel: not the one we came from; where a skeleton turns, a pixel can have a second
						// neighbour that is also a neighbour of the one we came from, and that one is skipped
						var nx = -1;
						var ny = -1;
						var bestRank = -1;
						for (var dy = -1; dy <= 1; dy++) {
							for (var dx = -1; dx <= 1; dx++) {
								var xx = x + dx;
								var yy = y + dy;
								if ((dx == 0 && dy == 0) || yy < 1 || yy >= height - 1 || xx < 1 || xx >= width - 1 || skeleton[yy][xx] == 0 || (xx == px && yy == py))
									continue;
								var nextToPrevious = (px != -1 && Math.abs(xx - px) <= 1 && Math.abs(yy - py) <= 1);
								var rank = (nextToPrevious ? 0 : 2) + (dx == 0 || dy == 0 ? 1 : 0);
								if (rank > bestRank) {
									bestRank = rank;
									nx = xx;
									ny = yy;
								}
							}
						}
						if (nx == -1)
							break; // a line on its own
						px = x;
						py = y;
						x = nx;
						y = ny;
					}
					if (branchX == -1 || path.isEmpty())
						continue;
					// the half-width of the ink at the branching point, taking the largest value around it
					var r = 0;
					for (var dy = -2; dy <= 2; dy++) {
						for (var dx = -2; dx <= 2; dx++) {
							var yy = branchY + dy;
							var xx = branchX + dx;
							if (yy >= 0 && yy < height && xx >= 0 && xx < width)
								r = Math.max(r, halfWidth[yy][xx]);
						}
					}
					if (path.size() <= LENGTH_PER_HALF_WIDTH * r + LENGTH_EXTRA) {
						for (var p : path)
							skeleton[p[1]][p[0]] = 0;
						removed++;
					}
				}
			}
			if (removed == before)
				break;
		}
		return removed;
	}

	/**
	 * for each pixel of ink, its distance to the nearest background pixel, counting steps to any of the eight neighbours
	 */
	static int[][] distanceToBackground(int[][] ink) {
		var height = ink.length;
		var width = ink[0].length;
		var distance = new int[height][width];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				if (ink[y][x] != 0) {
					var d = Integer.MAX_VALUE / 2;
					if (y > 0) {
						d = Math.min(d, distance[y - 1][x] + 1);
						if (x > 0)
							d = Math.min(d, distance[y - 1][x - 1] + 1);
						if (x < width - 1)
							d = Math.min(d, distance[y - 1][x + 1] + 1);
					} else
						d = 1;
					if (x > 0)
						d = Math.min(d, distance[y][x - 1] + 1);
					else
						d = 1;
					distance[y][x] = d;
				}
			}
		}
		for (var y = height - 1; y >= 0; y--) {
			for (var x = width - 1; x >= 0; x--) {
				if (ink[y][x] != 0) {
					var d = distance[y][x];
					if (y < height - 1) {
						d = Math.min(d, distance[y + 1][x] + 1);
						if (x > 0)
							d = Math.min(d, distance[y + 1][x - 1] + 1);
						if (x < width - 1)
							d = Math.min(d, distance[y + 1][x + 1] + 1);
					} else
						d = 1;
					if (x < width - 1)
						d = Math.min(d, distance[y][x + 1] + 1);
					else
						d = 1;
					distance[y][x] = d;
				}
			}
		}
		return distance;
	}

	private static int countNeighbours(int[][] matrix, int x, int y) {
		var count = 0;
		for (var dy = -1; dy <= 1; dy++) {
			for (var dx = -1; dx <= 1; dx++) {
				if ((dx != 0 || dy != 0) && matrix[y + dy][x + dx] != 0)
					count++;
			}
		}
		return count;
	}

	/**
	 * the number of separate runs of ink among the eight neighbours of a pixel, that is, the number of lines that leave it
	 */
	private static int countArms(int[][] matrix, int x, int y) {
		if (y < 1 || y >= matrix.length - 1 || x < 1 || x >= matrix[0].length - 1)
			return 0;
		int[] ring = {matrix[y - 1][x], matrix[y - 1][x + 1], matrix[y][x + 1], matrix[y + 1][x + 1], matrix[y + 1][x], matrix[y + 1][x - 1], matrix[y][x - 1], matrix[y - 1][x - 1]};
		var arms = 0;
		for (var i = 0; i < 8; i++) {
			if (ring[i] == 0 && ring[(i + 1) % 8] != 0)
				arms++;
		}
		return arms;
	}
}

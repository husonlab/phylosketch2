/*
 * Skeletonization.java Copyright (C) 2025 Daniel H. Huson
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

public class Skeletonization {
	public static void apply(int[][] matrix) {
		var pixelsChanged = false;
		var count = 0;
		do {
			pixelsChanged |= thinningIteration(matrix, true);
			pixelsChanged |= thinningIteration(matrix, false);
		} while (pixelsChanged && count++ < 100);
	}

	private static boolean thinningIteration(int[][] matrix, boolean firstSubIteration) {
		var pixelsChanged = false;
		var height = matrix.length;
		var width = matrix[0].length;

		int[][] marker = new int[height][width];

			for (int y = 1; y < height - 1; y++) {
				for (int x = 1; x < width - 1; x++) {
					if (matrix[y][x] == 1 && isRemovable(matrix, x, y, firstSubIteration)) {
						marker[y][x] = 1;
						pixelsChanged = true;
					}
				}
			}

			for (int y = 1; y < height - 1; y++) {
				for (int x = 1; x < width - 1; x++) {
					if (marker[y][x] == 1) {
						matrix[y][x] = 0;
				}
			}
		}

		return pixelsChanged;
	}

	private static boolean isRemovable(int[][] image, int x, int y, boolean firstSubIteration) {
		int[] neighbors = getNeighbors(image, x, y);
		int blackNeighbors = countBlackNeighbors(neighbors);

		if (blackNeighbors < 2 || blackNeighbors > 6) return false;

		if (countTransitions(neighbors) != 1) return false;

		if (firstSubIteration) {
			return !(neighbors[0] * neighbors[2] * neighbors[4] == 1) &&
				   !(neighbors[2] * neighbors[4] * neighbors[6] == 1);
		} else {
			return !(neighbors[0] * neighbors[2] * neighbors[6] == 1) &&
				   !(neighbors[0] * neighbors[4] * neighbors[6] == 1);
		}
	}

	private static int[] getNeighbors(int[][] image, int x, int y) {
		return new int[]{
				image[y][x - 1],     // P2
				image[y + 1][x - 1], // P3
				image[y + 1][x],     // P4
				image[y + 1][x + 1], // P5
				image[y][x + 1],     // P6
				image[y - 1][x + 1], // P7
				image[y - 1][x],     // P8
				image[y - 1][x - 1]  // P9
		};
	}

	private static int countBlackNeighbors(int[] neighbors) {
		int count = 0;
		for (int neighbor : neighbors) {
			if (neighbor == 1) {
				count++;
			}
		}
		return count;
	}

	private static int countTransitions(int[] neighbors) {
		int transitions = 0;
		for (int i = 0; i < neighbors.length; i++) {
			int current = neighbors[i];
			int next = neighbors[(i + 1) % neighbors.length];
			if (current == 0 && next == 1) {
				transitions++;
			}
		}
		return transitions;
	}
}

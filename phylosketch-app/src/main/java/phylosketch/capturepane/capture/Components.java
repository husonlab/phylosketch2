/*
 * Components.java Copyright (C) 2026 Daniel H. Huson
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

import java.util.Arrays;

/**
 * the connected pieces of ink of a binary matrix, each with its size and bounding box. Pixels that touch at a
 * side or at a corner belong to one piece
 * Daniel Huson, 11.2026
 */
public class Components {
	private final int[][] label; // 0 for background, else the number of the piece, from 1
	private int count;
	private int[] area = new int[64];
	private int[] minX = new int[64];
	private int[] maxX = new int[64];
	private int[] minY = new int[64];
	private int[] maxY = new int[64];

	/**
	 * finds the pieces
	 *
	 * @param matrix the binary matrix, indexed [y][x]; any value other than 0 is ink
	 */
	public Components(int[][] matrix) {
		var height = matrix.length;
		var width = (height == 0 ? 0 : matrix[0].length);
		label = new int[height][width];
		var stack = new int[1024];

		for (var y0 = 0; y0 < height; y0++) {
			for (var x0 = 0; x0 < width; x0++) {
				if (matrix[y0][x0] == 0 || label[y0][x0] != 0)
					continue;
				var c = ++count;
				if (c >= area.length) {
					var size = 2 * area.length;
					area = Arrays.copyOf(area, size);
					minX = Arrays.copyOf(minX, size);
					maxX = Arrays.copyOf(maxX, size);
					minY = Arrays.copyOf(minY, size);
					maxY = Arrays.copyOf(maxY, size);
				}
				minX[c] = maxX[c] = x0;
				minY[c] = maxY[c] = y0;
				var top = 0;
				stack[top++] = y0 * width + x0;
				label[y0][x0] = c;
				while (top > 0) {
					var p = stack[--top];
					var x = p % width;
					var y = p / width;
					area[c]++;
					minX[c] = Math.min(minX[c], x);
					maxX[c] = Math.max(maxX[c], x);
					minY[c] = Math.min(minY[c], y);
					maxY[c] = Math.max(maxY[c], y);
					for (var dy = -1; dy <= 1; dy++) {
						var yy = y + dy;
						if (yy < 0 || yy >= height)
							continue;
						for (var dx = -1; dx <= 1; dx++) {
							var xx = x + dx;
							if (xx < 0 || xx >= width || matrix[yy][xx] == 0 || label[yy][xx] != 0)
								continue;
							label[yy][xx] = c;
							if (top == stack.length)
								stack = Arrays.copyOf(stack, 2 * stack.length);
							stack[top++] = yy * width + xx;
						}
					}
				}
			}
		}
	}

	/**
	 * the number of pieces; they are numbered from 1
	 */
	public int count() {
		return count;
	}

	/**
	 * the number of the piece that a pixel belongs to, 0 for background
	 */
	public int get(int x, int y) {
		return label[y][x];
	}

	public int area(int c) {
		return area[c];
	}

	public int minX(int c) {
		return minX[c];
	}

	public int maxX(int c) {
		return maxX[c];
	}

	public int minY(int c) {
		return minY[c];
	}

	public int maxY(int c) {
		return maxY[c];
	}

	public int width(int c) {
		return maxX[c] - minX[c] + 1;
	}

	public int height(int c) {
		return maxY[c] - minY[c] + 1;
	}

	/**
	 * sets all pixels of a piece to background in the given matrix
	 */
	public void erase(int[][] matrix, int c) {
		for (var y = minY[c]; y <= maxY[c]; y++) {
			for (var x = minX[c]; x <= maxX[c]; x++) {
				if (label[y][x] == c)
					matrix[y][x] = 0;
			}
		}
	}
}

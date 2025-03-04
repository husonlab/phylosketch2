/*
 * PiecewiseLinearMap.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.embed;

import java.util.Arrays;

/**
 * compute a piece-wise linear map based on arrays src and tar
 * Daniel Huson, 3.2025
 */
public class PiecewiseLinearMap {
	private final double[] src;
	private final double[] tar;

	/**
	 * constructs a piece-wise linear map based on arrays src and tar
	 *
	 * @param src the source array, of minimum length 2, values strictly increasing
	 * @param tar the target array, of minimum length 2, values strictly increasing
	 * @throws IllegalArgumentException if arrays are not of the same length, do not contain at least two elements, or if are not strictly increasing
	 */
	public PiecewiseLinearMap(double[] src, double[] tar) {
		if (src == null || tar == null || src.length != tar.length || src.length < 2 || !isStrictlyIncreasing(src) || !isStrictlyIncreasing(tar)) {
			throw new IllegalArgumentException("Arrays must be non-null, of the same length, strictly increasing, and have at least two elements.");
		}
		this.src = Arrays.copyOf(src, src.length);
		this.tar = Arrays.copyOf(tar, tar.length);
	}

	/**
	 * applies the piece-wise linear map
	 *
	 * @param x the argument
	 * @return the value
	 */
	public double map(double x) {
		if (x <= src[0])
			return tar[0];
		if (x >= src[src.length - 1])
			return tar[tar.length - 1];

		var i = 0;
		while (i < src.length - 1 && x > src[i + 1]) {
			i++;
		}

		var t = (x - src[i]) / (src[i + 1] - src[i]);
		return tar[i] + t * (tar[i + 1] - tar[i]);
	}

	public static boolean isStrictlyIncreasing(double[] array) {
		for (var i = 1; i < array.length; i++) {
			if (array[i] <= array[i - 1]) {
				return false;
			}
		}
		return true;
	}
}

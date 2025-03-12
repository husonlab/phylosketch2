/*
 * NumberUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.embed.optimize;

import java.util.Arrays;

/**
 * some number utils
 * Daniel Huson, 3.2025
 */
public class NumberUtils {
	/**
	 * count how many array entries lie in the given range
	 *
	 * @param y    the sorted array (ascending)
	 * @param yMin start of range (exclusive)
	 * @param yMax end of range (exclusive)
	 * @return count
	 */
	public static int countInRange(double[] y, double yMin, double yMax) {
		int lowerIndex = Arrays.binarySearch(y, yMin);
		if (lowerIndex < 0) lowerIndex = -lowerIndex - 1;
		else lowerIndex++; // Move to next index to exclude yMin

		int upperIndex = Arrays.binarySearch(y, yMax);
		if (upperIndex < 0) upperIndex = -upperIndex - 1; // Take insertion point directly
		// If upperIndex was found exactly, we do NOT decrement (this is the fix!)

		return Math.max(0, upperIndex - lowerIndex);
	}
}
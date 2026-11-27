/*
 * Binarization.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * turns a picture into ink and background, for line extraction
 * <p>
 * A pixel is ink if it is clearly darker than the background <em>around it</em>, rather than darker than one
 * level for the whole picture: one level cannot be right for a grey picture, for a photograph that is lit
 * unevenly, or for a drawing in which some clades lie on a coloured background, and a level that is wrong turns
 * the whole background into ink. The background around a pixel is the brightness that is left when everything
 * narrower than a window is removed from the picture (a grey-scale closing), so lines, text and node symbols
 * count as drawn on it, whereas a filled area that is wider than the window is background itself.
 * <p>
 * On a white background this is the fixed threshold of 200 that capture has always used.
 * Daniel Huson, 11.2026
 */
public class Binarization {
	// a pixel is ink if its brightness is at most this proportion of that of the background around it. On white
	// (255) this is the threshold of 200 of ImageUtils.convertToBinaryArray
	private static final int INK_NUMERATOR = 200;
	private static final int INK_DENOMINATOR = 255;
	// ... and darker than it by at least this much, of 255: inside a black area nothing is ink
	private static final int MIN_CONTRAST = 25;
	// half the width of the window, as a proportion of the longer side of the picture, and its least value in
	// pixels: anything drawn must be narrower than the window, so this bounds the width of the thickest line
	private static final double RADIUS_PROPORTION = 0.02;
	private static final int MIN_RADIUS = 12;
	// below this median brightness a picture is taken to be light lines on a dark background
	private static final int DARK_PICTURE = 90;

	/**
	 * computes the ink of a picture
	 *
	 * @param image the picture
	 * @return the matrix, indexed [y][x], 1 for ink and 0 for background
	 */
	public static int[][] apply(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var brightness = computeBrightness(image);

		if (median(brightness) < DARK_PICTURE) { // light on dark: work on the negative
			for (var row : brightness) {
				for (var x = 0; x < width; x++)
					row[x] = 255 - row[x];
			}
		}

		var radius = Math.max(MIN_RADIUS, (int) Math.round(RADIUS_PROPORTION * Math.max(width, height)));
		var background = computeBackground(brightness, radius);

		var matrix = new int[height][width];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				matrix[y][x] = (INK_DENOMINATOR * brightness[y][x] <= INK_NUMERATOR * background[y][x] && background[y][x] - brightness[y][x] >= MIN_CONTRAST ? 1 : 0);
			}
		}
		return matrix;
	}

	/**
	 * the brightness of each pixel, 0 to 255, weighted as in ImageUtils.convertToBinaryArray. A transparent pixel
	 * shows the white that the picture is displayed on
	 */
	public static int[][] computeBrightness(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var reader = image.getPixelReader();
		var brightness = new int[height][width];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var argb = reader.getArgb(x, y);
				var a = (argb >>> 24) & 0xFF;
				var r = (argb >> 16) & 0xFF;
				var g = (argb >> 8) & 0xFF;
				var b = argb & 0xFF;
				var gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
				brightness[y][x] = (a == 255 ? gray : (a * gray + (255 - a) * 255) / 255);
			}
		}
		return brightness;
	}

	/**
	 * the brightness of the background around each pixel: the picture without anything darker than its
	 * surroundings that is narrower than the window. The brightest value in the window around each pixel removes
	 * such things, and the darkest value of that, again in the window, puts the edges of larger areas back where
	 * they were
	 *
	 * @param brightness the brightness, indexed [y][x]
	 * @param radius     half the width of the window
	 */
	public static int[][] computeBackground(int[][] brightness, int radius) {
		return slide(slide(brightness, radius, true), radius, false);
	}

	/**
	 * makes a grey picture in which the background is white everywhere: each pixel is as bright as it is in
	 * proportion to the background around it. OCR finds its own threshold for the whole picture, and on a grey
	 * or coloured background, or one that changes across the picture, that threshold loses the text
	 *
	 * @param image the picture
	 * @return the grey picture; on a white background, the brightness as it is
	 */
	public static Image flatten(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var reader = image.getPixelReader();
		var brightness = new int[height][width];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var argb = reader.getArgb(x, y);
				var a = (argb >>> 24) & 0xFF;
				var grey = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3; // as ImageUtils.convertToGrayScale
				brightness[y][x] = (a * grey + (255 - a) * 255) / 255;
			}
		}
		if (median(brightness) < DARK_PICTURE) {
			for (var row : brightness) {
				for (var x = 0; x < width; x++)
					row[x] = 255 - row[x];
			}
		}
		var background = computeBackground(brightness, Math.max(MIN_RADIUS, (int) Math.round(RADIUS_PROPORTION * Math.max(width, height))));
		var result = new WritableImage(width, height);
		var writer = result.getPixelWriter();
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var value = (background[y][x] == 0 ? 255 : Math.min(255, 255 * brightness[y][x] / background[y][x]));
				writer.setArgb(x, y, 0xFF000000 | (value << 16) | (value << 8) | value);
			}
		}
		return result;
	}

	/**
	 * replaces each value by the largest, or smallest, value in the square window around it
	 */
	private static int[][] slide(int[][] values, int radius, boolean max) {
		var height = values.length;
		var width = values[0].length;
		var rows = new int[height][width];
		for (var y = 0; y < height; y++)
			slide(values[y], rows[y], width, radius, max);
		var result = new int[height][width];
		var column = new int[height];
		var slid = new int[height];
		for (var x = 0; x < width; x++) {
			for (var y = 0; y < height; y++)
				column[y] = rows[y][x];
			slide(column, slid, height, radius, max);
			for (var y = 0; y < height; y++)
				result[y][x] = slid[y];
		}
		return result;
	}

	/**
	 * one line of the above, in time linear in its length: the indices of the values that can still become the
	 * extreme of a window are kept in a queue, the extreme at its head
	 */
	private static void slide(int[] in, int[] out, int n, int radius, boolean max) {
		var queue = new int[n];
		var head = 0;
		var tail = 0;
		var next = 0; // next index to enter the window
		for (var i = 0; i < n; i++) {
			var last = Math.min(n - 1, i + radius);
			while (next <= last) {
				while (tail > head && (max ? in[queue[tail - 1]] <= in[next] : in[queue[tail - 1]] >= in[next]))
					tail--;
				queue[tail++] = next++;
			}
			while (queue[head] < i - radius)
				head++;
			out[i] = in[queue[head]];
		}
	}

	private static int median(int[][] values) {
		var counts = new int[256];
		var total = 0L;
		for (var row : values) {
			for (var value : row) {
				counts[value]++;
				total++;
			}
		}
		var seen = 0L;
		for (var value = 0; value < 256; value++) {
			seen += counts[value];
			if (2 * seen >= total)
				return value;
		}
		return 255;
	}

}

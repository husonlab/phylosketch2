/*
 * LineInk.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import phylosketch.ocr.OcrWord;

import java.util.ArrayList;
import java.util.List;

/**
 * reads again, without the lines, each word that OCR read together with a piece of a line
 * <p>
 * A label at the end of a dashed branch comes back as one word with the dashes ("————-Homosapiens"), a dotted
 * line as a row of letters in front of the label ("sesesee Sus scrofa"), and such a word may be several times
 * the height of the text. Once the dashed lines have been joined, what is a line can be told from what is text
 * by size: a line is a piece of ink that is larger than any letter. Those pieces are painted over in a copy of
 * the picture, and the region of each affected word is read again from that copy.
 * Daniel Huson, 11.2026
 */
public class LineInk {
	// a piece of ink is a line if it is longer than this in either direction, as a proportion of the longer side of the picture, or in pixels if that is more
	private static final double MAX_GLYPH_PROPORTION = 0.04;
	private static final int MIN_MAX_GLYPH = 40;
	// a word is read again if at least this proportion of the longer side of its box is line
	private static final double MIN_LINE_PROPORTION = 0.5;
	// a word is dropped if nothing is read the second time and a line runs through this proportion of its box
	private static final double ALL_LINE_PROPORTION = 0.8;
	static final String LINE_CHARACTERS = "-‐‑‒–—―−_=~|";
	// text lower than this is enlarged to twice its size before it is read again
	private static final int SMALL_TEXT = 30;
	private static final int PADDING = 10;

	/**
	 * finds the ink that belongs to lines
	 *
	 * @param ink the binary matrix, indexed [y][x], in which dashed lines have been joined
	 * @return true for each pixel of a piece of ink that is larger than a letter
	 */
	public static boolean[][] compute(int[][] ink) {
		var height = ink.length;
		var width = ink[0].length;
		var maxGlyph = maxGlyph(width, height);
		var components = new Components(ink);
		var lines = new boolean[height][width];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var c = components.get(x, y);
				if (c != 0 && (components.width(c) > maxGlyph || components.height(c) > maxGlyph))
					lines[y][x] = true;
			}
		}
		return lines;
	}

	/**
	 * the size, in pixels, that a letter does not exceed in a picture of the given size
	 */
	public static int maxGlyph(int width, int height) {
		return Math.max(MIN_MAX_GLYPH, (int) Math.round(MAX_GLYPH_PROPORTION * Math.max(width, height)));
	}

	/**
	 * paints over the lines of a picture, and the pixel on either side of them, where a smooth edge leaves grey
	 *
	 * @return a copy of the image without the lines
	 */
	public static Image erase(Image image, boolean[][] lines) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var result = new WritableImage(image.getPixelReader(), width, height);
		var writer = result.getPixelWriter();
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				if (lines[y][x]) {
					for (var yy = Math.max(0, y - 1); yy <= Math.min(height - 1, y + 1); yy++) {
						for (var xx = Math.max(0, x - 1); xx <= Math.min(width - 1, x + 1); xx++)
							writer.setArgb(xx, yy, 0xFFFFFFFF);
					}
				}
			}
		}
		return result;
	}

	/**
	 * reads again each word that has a line in its box
	 *
	 * @param image the image that the words were read from
	 * @param clean the same image without the lines
	 * @param words the words
	 * @return the words, in which each affected one is replaced by what was read the second time, if anything was
	 */
	public static List<OcrWord> readAgain(Image image, Image clean, List<OcrWord> words) {
		var reader = image.getPixelReader();
		var cleanReader = clean.getPixelReader();
		var result = new ArrayList<OcrWord>(words.size());
		for (var word : words) {
			var box = word.boundingBox();
			var x0 = Math.max(0, (int) Math.floor(box.getMinX()));
			var x1 = Math.min((int) image.getWidth(), (int) Math.ceil(box.getMaxX()));
			var y0 = Math.max(0, (int) Math.floor(box.getMinY()));
			var y1 = Math.min((int) image.getHeight(), (int) Math.ceil(box.getMaxY()));
			// how far the lines reach through the box
			var minX = Integer.MAX_VALUE;
			var maxX = Integer.MIN_VALUE;
			var minY = Integer.MAX_VALUE;
			var maxY = Integer.MIN_VALUE;
			for (var y = y0; y < y1; y++) {
				for (var x = x0; x < x1; x++) {
					if (reader.getArgb(x, y) != cleanReader.getArgb(x, y)) {
						minX = Math.min(minX, x);
						maxX = Math.max(maxX, x);
						minY = Math.min(minY, y);
						maxY = Math.max(maxY, y);
					}
				}
			}
			if (maxX >= minX) {
				var text = (word.text() == null ? "" : word.text().strip());
				var letters = text.chars().filter(Character::isLetterOrDigit).count();
				var lineCharacters = text.chars().filter(ch -> LINE_CHARACTERS.indexOf(ch) >= 0).count();
				if (letters == 0)
					continue; // nothing but a piece of line
				var lineProportion = (Math.max(maxX - minX, maxY - minY) + 1.0) / Math.max(x1 - x0, y1 - y0);
				if (lineCharacters > 0 || lineProportion >= MIN_LINE_PROPORTION) {
					var replacement = read(clean, cleanReader, x0, y0, x1 - x0, y1 - y0);
					var found = replacement.stream().mapToLong(w -> w.text().chars().filter(Character::isLetterOrDigit).count()).sum();
					// Where a line runs through most of the box, some of the letters read the first time are pieces
					// of the line (a dotted line comes back as "sesesee"), so anything read now is better. Where
					// only an end of a line is in the box, the second reading has to keep most of the letters
					if (lineProportion >= MIN_LINE_PROPORTION ? found >= Math.min(2, letters) : found >= 0.6 * letters) {
						result.addAll(replacement);
						continue;
					}
					if (found == 0 && lineProportion >= ALL_LINE_PROPORTION)
						continue; // nothing but line
				}
			}
			result.add(word);
		}
		return result;
	}

	/**
	 * runs OCR on a region of an image, enlarged if the text is small
	 *
	 * @return the words, in the coordinates of the image
	 */
	private static ArrayList<OcrWord> read(Image image, PixelReader reader, int x0, int y0, int width, int height) {
		var words = new ArrayList<OcrWord>();
		if (width <= 0 || height <= 0)
			return words;
		var scale = (height < SMALL_TEXT ? 2 : 1);
		var region = new WritableImage(scale * width + 2 * PADDING, scale * height + 2 * PADDING);
		var writer = region.getPixelWriter();
		for (var y = 0; y < region.getHeight(); y++) {
			for (var x = 0; x < region.getWidth(); x++)
				writer.setArgb(x, y, 0xFFFFFFFF);
		}
		for (var y = 0; y < scale * height; y++) {
			for (var x = 0; x < scale * width; x++) {
				if (scale == 1)
					writer.setArgb(PADDING + x, PADDING + y, reader.getArgb(x0 + x, y0 + y));
				else { // between four pixels
					var fx = Math.max(0, Math.min(width - 1.0, (x + 0.5) / scale - 0.5));
					var fy = Math.max(0, Math.min(height - 1.0, (y + 0.5) / scale - 0.5));
					var xa = (int) fx;
					var ya = (int) fy;
					var xb = Math.min(width - 1, xa + 1);
					var yb = Math.min(height - 1, ya + 1);
					var wx = fx - xa;
					var wy = fy - ya;
					var value = (1 - wy) * ((1 - wx) * grey(reader, x0 + xa, y0 + ya) + wx * grey(reader, x0 + xb, y0 + ya))
								+ wy * ((1 - wx) * grey(reader, x0 + xa, y0 + yb) + wx * grey(reader, x0 + xb, y0 + yb));
					var v = (int) Math.round(value);
					writer.setArgb(PADDING + x, PADDING + y, 0xFF000000 | (v << 16) | (v << 8) | v);
				}
			}
		}
		try {
			for (var word : OCR.getWords(region)) {
				var text = (word.text() == null ? "" : word.text().strip());
				if (!text.isEmpty()) {
					var b = word.boundingBox();
					words.add(new OcrWord(text, word.confidence(), new Rectangle2D((b.getMinX() - PADDING) / scale + x0, (b.getMinY() - PADDING) / scale + y0, b.getWidth() / scale, b.getHeight() / scale)));
				}
			}
		} catch (Exception ignored) {
		}
		return words;
	}

	private static int grey(PixelReader reader, int x, int y) {
		var argb = reader.getArgb(x, y);
		var a = (argb >>> 24) & 0xFF;
		var value = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
		return (a * value + (255 - a) * 255) / 255;
	}
}

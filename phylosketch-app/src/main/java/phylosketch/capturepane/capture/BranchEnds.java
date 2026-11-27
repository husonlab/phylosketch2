/*
 * BranchEnds.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.scene.paint.Color;
import phylosketch.ocr.OcrWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a branch runs right up to its label, OCR reads the end of the branch as part of the label:
 * "unsampled taxon" comes back as "—unsampled" + "tazon". The dash makes the label fail the
 * must-start-alphanumeric test, and it also pulls the confidence of the word down (to 27 in that case),
 * so removing the character alone is not enough. This finds the piece of branch in the image, and reads
 * the label again without it.
 * Daniel Huson, 11.2026
 */
public class BranchEnds {
	private static final String DASHES = "-‐‑‒–—―−_=~";
	// a column of the word box belongs to a branch if its dark pixels span at most this proportion of the box height...
	private static final double MAX_THICKNESS = 0.15;
	// ... and lie this far away from the top and the bottom of the box (unlike the bar of a T or the foot of an A)
	private static final double MIN_MARGIN = 0.25;
	// never cut away more than this proportion of a word
	private static final double MAX_CUT = 0.5;
	// a pixel is dark if it lies in this lower part of the range of brightness found in the box
	private static final double DARK_PROPORTION = 0.6;
	private static final int PADDING = 10;

	/**
	 * reads again, without the branch, each label that starts or ends with a dash
	 *
	 * @param image the image that the words were read from
	 * @param words the words
	 * @return the words, in which each affected label is replaced by what was read the second time
	 */
	public static List<OcrWord> removeFromWords(Image image, List<OcrWord> words) {
		var reader = image.getPixelReader();
		if (reader == null)
			return words;
		var result = new ArrayList<>(words);

		for (var word : words) {
			if (!result.contains(word))
				continue; // was read again together with an earlier word
			var text = (word.text() == null ? "" : word.text().strip());
			if (text.chars().noneMatch(Character::isLetterOrDigit))
				continue;
			var leading = DASHES.indexOf(text.charAt(0)) >= 0;
			var trailing = DASHES.indexOf(text.charAt(text.length() - 1)) >= 0;
			if (!leading && !trailing)
				continue;

			var box = word.boundingBox();
			var minX = (leading ? findLabelEdge(reader, image, box, true) : box.getMinX());
			var maxX = (trailing ? findLabelEdge(reader, image, box, false) : box.getMaxX());
			if (minX == box.getMinX() && maxX == box.getMaxX())
				continue; // no branch found, leave the word as it is

			// the label may go on in further words on the side that was not cut
			var group = new ArrayList<OcrWord>();
			group.add(word);
			var minY = box.getMinY();
			var maxY = box.getMaxY();
			var maxGap = 2.0 * box.getHeight();
			var changed = true;
			while (changed) {
				changed = false;
				for (var other : result) {
					if (group.contains(other))
						continue;
					var b = other.boundingBox();
					var centerY = 0.5 * (b.getMinY() + b.getMaxY());
					if (centerY < box.getMinY() || centerY > box.getMaxY())
						continue;
					// the boxes of neighbouring words may overlap a little
					var onRight = !trailing && b.getMaxX() > maxX && b.getMinX() - maxX <= maxGap;
					var onLeft = !leading && b.getMinX() < minX && minX - b.getMaxX() <= maxGap;
					if (onRight || onLeft) {
						group.add(other);
						minX = Math.min(minX, b.getMinX());
						maxX = Math.max(maxX, b.getMaxX());
						minY = Math.min(minY, b.getMinY());
						maxY = Math.max(maxY, b.getMaxY());
						changed = true;
					}
				}
			}

			var replacement = readAgain(image, reader, minX, minY, maxX, maxY);
			// the second reading may come back without a short word that the first had, such as the "N." of
			// "N. confucianus": then the first is kept, without its dashes
			if (!replacement.isEmpty() && countLettersAndDigits(replacement) < countLettersAndDigits(group)) {
				var pos = result.indexOf(word);
				result.remove(word);
				result.add(Math.min(pos, result.size()), new OcrWord(stripDashes(text), word.confidence(), box));
				continue;
			}
			if (replacement.isEmpty()) {
				// could not be read again: at least remove the dash from the text and from the box
				group.clear();
				group.add(word);
				var cutMinX = (leading ? findLabelEdge(reader, image, box, true) : box.getMinX());
				var cutMaxX = (trailing ? findLabelEdge(reader, image, box, false) : box.getMaxX());
				replacement.add(new OcrWord(stripDashes(text), word.confidence(), new Rectangle2D(cutMinX, box.getMinY(), cutMaxX - cutMinX, box.getHeight())));
			}
			var pos = result.indexOf(word);
			result.removeAll(group);
			result.addAll(Math.min(pos, result.size()), replacement);
		}
		return result;
	}

	/**
	 * removes the dashes at the start and end of the text of each word that also has a letter or digit. Where a
	 * label stands in a dashed line, or a branch runs up to it and was not found in the image above, the dash is
	 * not part of the label, and a label that starts or ends with one is rejected as a whole
	 *
	 * @param words the words
	 * @return the words, those with such dashes replaced
	 */
	public static List<OcrWord> stripDashes(List<OcrWord> words) {
		var result = new ArrayList<OcrWord>(words.size());
		for (var word : words) {
			var text = word.text();
			if (text != null && text.chars().anyMatch(Character::isLetterOrDigit)) {
				var stripped = stripDashes(text.strip());
				if (!stripped.equals(text))
					word = new OcrWord(stripped, word.confidence(), word.boundingBox());
			}
			result.add(word);
		}
		return result;
	}

	/**
	 * finds where the label begins (or ends) in the box of a word, by skipping the columns that hold a piece of branch
	 *
	 * @return the x-coordinate, or that of the box, if no branch is found
	 */
	private static double findLabelEdge(PixelReader reader, Image image, Rectangle2D box, boolean fromLeft) {
		var x0 = Math.max(0, (int) Math.floor(box.getMinX()));
		var x1 = Math.min((int) image.getWidth(), (int) Math.ceil(box.getMaxX()));
		var y0 = Math.max(0, (int) Math.floor(box.getMinY()));
		var y1 = Math.min((int) image.getHeight(), (int) Math.ceil(box.getMaxY()));
		var height = y1 - y0;
		var unchanged = (fromLeft ? box.getMinX() : box.getMaxX());
		if (height <= 0 || x1 <= x0)
			return unchanged;
		var maxThickness = Math.max(2, (int) Math.round(MAX_THICKNESS * height));

		// a thin line is often grey rather than black, so what counts as dark depends on the box
		var darkest = 1.0;
		var lightest = 0.0;
		for (var y = y0; y < y1; y++) {
			for (var x = x0; x < x1; x++) {
				var brightness = brightness(reader, x, y);
				darkest = Math.min(darkest, brightness);
				lightest = Math.max(lightest, brightness);
			}
		}
		var threshold = darkest + DARK_PROPORTION * (lightest - darkest);

		var branchTop = -1;
		var branchBottom = -1;
		for (var i = 0; i < x1 - x0; i++) {
			var x = (fromLeft ? x0 + i : x1 - 1 - i);
			var top = -1;
			var bottom = -1;
			for (var y = y0; y < y1; y++) {
				if (brightness(reader, x, y) < threshold) {
					if (top == -1)
						top = y;
					bottom = y;
				}
			}
			if (top == -1)
				continue; // empty column
			var isBranch = (bottom - top + 1 <= maxThickness && top - y0 >= MIN_MARGIN * height && y1 - 1 - bottom >= MIN_MARGIN * height);
			// a branch is a straight line: the stroke of a letter that follows it rises or falls
			if (isBranch && branchTop != -1 && (Math.abs(top - branchTop) > 1 || Math.abs(bottom - branchBottom) > 1))
				isBranch = false;
			if (isBranch) {
				if (branchTop == -1) {
					branchTop = top;
					branchBottom = bottom;
				}
			} else {
				if (branchTop == -1 || i > MAX_CUT * (x1 - x0))
					return unchanged;
				return (fromLeft ? x : x + 1);
			}
		}
		return unchanged;
	}

	/**
	 * runs OCR on a region of the image
	 *
	 * @return the words, in the coordinates of the image; empty, if nothing with a letter or digit was read
	 */
	private static ArrayList<OcrWord> readAgain(Image image, PixelReader reader, double minX, double minY, double maxX, double maxY) {
		var words = new ArrayList<OcrWord>();
		var x0 = Math.max(0, (int) Math.floor(minX));
		var y0 = Math.max(0, (int) Math.floor(minY));
		var width = Math.min((int) image.getWidth(), (int) Math.ceil(maxX)) - x0;
		var height = Math.min((int) image.getHeight(), (int) Math.ceil(maxY)) - y0;
		if (width <= 0 || height <= 0)
			return words;

		var region = new WritableImage(width + 2 * PADDING, height + 2 * PADDING);
		var writer = region.getPixelWriter();
		for (var y = 0; y < region.getHeight(); y++) {
			for (var x = 0; x < region.getWidth(); x++) {
				writer.setColor(x, y, Color.WHITE);
			}
		}
		writer.setPixels(PADDING, PADDING, width, height, reader, x0, y0);

		try {
			for (var word : OCR.getWords(region)) {
				var text = (word.text() == null ? "" : stripDashes(word.text().strip()));
				if (!text.isEmpty()) {
					var b = word.boundingBox();
					words.add(new OcrWord(text, word.confidence(), new Rectangle2D(b.getMinX() - PADDING + x0, b.getMinY() - PADDING + y0, b.getWidth(), b.getHeight())));
				}
			}
		} catch (Exception ignored) {
		}
		if (words.stream().noneMatch(w -> w.text().chars().anyMatch(Character::isLetterOrDigit)))
			words.clear();
		return words;
	}

	private static long countLettersAndDigits(List<OcrWord> words) {
		return words.stream().mapToLong(w -> w.text() == null ? 0 : w.text().chars().filter(Character::isLetterOrDigit).count()).sum();
	}

	private static String stripDashes(String text) {
		var start = 0;
		var end = text.length();
		while (start < end && (DASHES.indexOf(text.charAt(start)) >= 0 || Character.isWhitespace(text.charAt(start))))
			start++;
		while (end > start && (DASHES.indexOf(text.charAt(end - 1)) >= 0 || Character.isWhitespace(text.charAt(end - 1))))
			end--;
		return text.substring(start, end);
	}

	private static double brightness(PixelReader reader, int x, int y) {
		var color = reader.getColor(x, y);
		return color.getOpacity() * color.getBrightness() + (1.0 - color.getOpacity());
	}
}

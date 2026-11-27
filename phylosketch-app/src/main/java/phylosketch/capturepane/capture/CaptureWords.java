/*
 * CaptureWords.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.fx.util.ProgramProperties;
import phylosketch.ocr.OcrWord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * word capture
 * Daniel Huson, 1.2025
 */
public class CaptureWords {
	/**
	 * filter words
	 *
	 * @param words         input words
	 * @param minTextHeight the min word bounding box height
	 * @param maxTextHeight the max word bounding box height
	 * @return the list of filtered words
	 */
	public static List<OcrWord> filter(Collection<OcrWord> words, double minWordConfidence, int minWordLength, double minTextHeight, double maxTextHeight) {
		var list = new ArrayList<OcrWord>();

		for (var word : words) {
			var box = word.boundingBox();
			if (ProgramProperties.isIOS() || word.text().length() >= minWordLength && word.confidence() >= minWordConfidence && box.getHeight() >= minTextHeight && box.getHeight() <= maxTextHeight && word.text().matches(".*[a-zA-Z0-9].*")) {
				list.add(word);
			}
		}
		return list;
	}


	/**
	 * Join consecutive words on the same line into multi-word labels, merging across small
	 * horizontal gaps (same label) but not across large gaps or different lines (different nodes).
	 *
	 * @param words input words
	 * @return joined words in reading order (top-to-bottom, getLeft-to-getRight)
	 */
	public static List<OcrWord> joinConsecutiveWords(List<OcrWord> words, boolean mustStartWithAlphaNumeric,
													 boolean mustEndWithAlphaNumeric, boolean mustContainLetter) {
		return joinConsecutiveWords(words, mustStartWithAlphaNumeric, mustEndWithAlphaNumeric, mustContainLetter, 2.0);
	}

	/**
	 * Join consecutive words on the same line into multi-word labels, merging across small
	 * horizontal gaps (same label) but not across large gaps or different lines (different nodes).
	 *
	 * @param words     input words
	 * @param gapFactor largest horizontal gap (as a multiple of text height) still treated as one label. Generous
	 *                  (2) where a line is normally a single label: we only want to split genuinely far-apart words
	 *                  (e.g. two leaves that happen to share a row), not the ordinary space before a specimen code or
	 *                  after a genus abbreviation. Less where the labels of neighbouring leaves stand side by side
	 *                  in one line, as below a tree with the root at the top
	 * @return joined words in reading order (top-to-bottom, getLeft-to-getRight)
	 */
	public static List<OcrWord> joinConsecutiveWords(List<OcrWord> words, boolean mustStartWithAlphaNumeric,
													 boolean mustEndWithAlphaNumeric, boolean mustContainLetter, double gapFactor) {
		if (words == null || words.isEmpty())
			return new ArrayList<>();

		final double GAP_FACTOR = gapFactor;

		// 1. clean: strip surrounding whitespace / control chars, drop blanks  <-- fixes the empty-with-Vision case
		var cleaned = new ArrayList<OcrWord>();
		for (var w : words) {
			var text = (w.text() == null ? "" : w.text().strip());
			if (!text.isEmpty())
				cleaned.add(text.equals(w.text()) ? w : new OcrWord(text, w.confidence(), w.boundingBox()));
		}

		// 2. group into lines by y-CENTER proximity, processing top-to-bottom. Using each word's center
		//    (rather than the growing union of the line's box extents) keeps densely-stacked label lines from
		//    absorbing one another's words: even when neighbouring lines' boxes touch, their centers are a
		//    full line apart. This is what lets large, tightly-spaced top/bottom captures group correctly.
		var heights = cleaned.stream().mapToDouble(w -> w.boundingBox().getHeight()).sorted().toArray();
		var medianHeight = (heights.length == 0 ? 1.0 : heights[heights.length / 2]);
		var lineTolerance = 0.6 * medianHeight; // a word joins the current line if its center is within this
		cleaned.sort(Comparator.comparingDouble(CaptureWords::centerY));
		var lines = new ArrayList<List<OcrWord>>();
		var lineCenterY = 0.0;
		for (var w : cleaned) {
			var yc = centerY(w);
			if (lines.isEmpty() || Math.abs(yc - lineCenterY) > lineTolerance) {
				lines.add(new ArrayList<>());
			}
			var line = lines.get(lines.size() - 1);
			line.add(w);
			lineCenterY = line.stream().mapToDouble(CaptureWords::centerY).average().orElse(yc);
		}

		// 3. within each line, sort getLeft-to-getRight and merge across small gaps
		var result = new ArrayList<OcrWord>();
		for (var line : lines) {
			line.sort(Comparator.comparingDouble(w -> w.boundingBox().getMinX()));
			var group = new ArrayList<OcrWord>();
			for (var w : line) {
				if (!group.isEmpty()) {
					var prev = group.get(group.size() - 1).boundingBox();
					var cur = w.boundingBox();
					var gap = cur.getMinX() - prev.getMaxX();
					// scale by the TALLER of the two boxes: a short token such as the genus abbreviation "V."
					// otherwise shrinks the threshold so its trailing space looks like a label break and it
					// gets severed (then discarded by the trailing-"." rule).
					var height = Math.max(prev.getHeight(), cur.getHeight());
					if (gap > GAP_FACTOR * height) {   // big gap -> different label / node
						result.add(merge(group));
						group = new ArrayList<>();
					}
				}
				group.add(w);
			}
			if (!group.isEmpty())
				result.add(merge(group));
		}

		// 4. same filter as before (now seeing cleaned text, so it no longer drops everything)
		return new ArrayList<>(result.stream().filter(w -> !(mustContainLetter && !containsLetter(w)
															 || mustStartWithAlphaNumeric && !startsAlphaNumeric(w)
															 || mustEndWithAlphaNumeric && !endsAlphaNumeric(w))).toList());
	}

	private static OcrWord merge(List<OcrWord> group) {
		if (group.size() == 1)
			return group.get(0);
		var buf = new StringBuilder();
		var minX = Double.POSITIVE_INFINITY;
		var minY = Double.POSITIVE_INFINITY;
		var maxX = Double.NEGATIVE_INFINITY;
		var maxY = Double.NEGATIVE_INFINITY;
		var confidence = 0.0;
		for (var w : group) {
			if (!buf.isEmpty())
				buf.append(' ');
			buf.append(w.text());
			var b = w.boundingBox();
			minX = Math.min(minX, b.getMinX());
			minY = Math.min(minY, b.getMinY());
			maxX = Math.max(maxX, b.getMaxX());
			maxY = Math.max(maxY, b.getMaxY());
			confidence += w.confidence();
		}
		return new OcrWord(buf.toString(), (float) (confidence / group.size()),
				new Rectangle2D(minX, minY, maxX - minX, maxY - minY));
	}

	private static double centerY(OcrWord word) {
		var b = word.boundingBox();
		return 0.5 * (b.getMinY() + b.getMaxY());
	}

	private static boolean containsLetter(OcrWord word) {
		return word.text().chars().anyMatch(Character::isLetter);
	}

	private static final String OPENING = "([{'\"‘“";
	private static final String CLOSING = ")]}'\"’”";

	/**
	 * does the label start with a letter or digit, or with a bracket or quotation mark that is closed later in it,
	 * as in "(Homo) sapiens" or "'himalayanus'"?
	 */
	private static boolean startsAlphaNumeric(OcrWord word) {
		var text = word.text();
		if (text.isEmpty())
			return false;
		if (Character.isLetterOrDigit(text.charAt(0)))
			return true;
		var which = OPENING.indexOf(text.charAt(0));
		return which >= 0 && text.indexOf(CLOSING.charAt(which), 1) > 0 && text.chars().anyMatch(Character::isLetterOrDigit);
	}

	/**
	 * does the label end with a letter or digit, or with a bracket or quotation mark that was opened earlier in it,
	 * as in "Ocelot (Brazil)", or with the full stop of an abbreviation, as in "sp. nov."? Rejecting those loses
	 * the whole label
	 */
	private static boolean endsAlphaNumeric(OcrWord word) {
		var text = word.text();
		if (text.isEmpty())
			return false;
		var last = text.charAt(text.length() - 1);
		if (Character.isLetterOrDigit(last))
			return true;
		if (last == '.')
			return text.length() >= 3 && Character.isLetter(text.charAt(text.length() - 2)) && Character.isLetter(text.charAt(text.length() - 3));
		var which = CLOSING.indexOf(last);
		return which >= 0 && text.lastIndexOf(OPENING.charAt(which), text.length() - 2) >= 0 && text.chars().anyMatch(Character::isLetterOrDigit);
	}

	private static boolean intersect(double minI, double maxI, double minJ, double maxJ) {
		return minI <= minJ && maxI >= minJ || minI >= minJ && minI <= maxJ;
	}
}

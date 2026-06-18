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
import jloda.graph.Graph;
import jloda.graph.Node;
import phylosketch.ocr.OcrWord;

import java.util.ArrayList;
import java.util.Collection;
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
	public static List<OcrWord> filter(Collection<OcrWord> words, int minWordLength, double minTextHeight, double maxTextHeight) {
		var list = new ArrayList<OcrWord>();

		for (var word : words) {
			var box = word.boundingBox();
			if (word.text().length() >= minWordLength && word.confidence() >= minTextHeight && box.getHeight() <= maxTextHeight && word.text().matches(".*[a-zA-Z0-9].*")) {
				list.add(word);
			}
		}
		return list;
	}


	/**
	 * join pairs of consecutive words
	 *
	 * @param words input words
	 * @return joined pairs
	 * todo: join more than two words
	 */
	public static List<OcrWord> joinConsecutiveWords(List<OcrWord> words, boolean mustStartWithAlphaNumeric, boolean mustEndWithAlphaNumeric, boolean mustContainLetter) {
		var graph = new Graph();

		for (var word : words) {
			graph.newNode(word);
		}
		for (var v : graph.nodes()) {
			var vWord = (OcrWord) v.getInfo();
			var bboxI = vWord.boundingBox();
			for (var w : graph.nodes(v)) {
				var wWord = (OcrWord) w.getInfo();
				var bboxJ = wWord.boundingBox();
				var dx = bboxJ.getMinX() - (bboxI.getMaxX());
				if (intersect(bboxI.getMinY(), bboxI.getMaxY(), bboxJ.getMinY(), bboxJ.getMaxY()) && dx >= 0 && dx <= 20) {
					graph.newEdge(v, w, dx);
				}
			}
		}

		var list = new ArrayList<OcrWord>();

		if (true) {
			var changed = true;
			while (changed) {
				changed = false;
				var start = graph.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
				if (start.isPresent()) {
					var v = start.get();
					var path = new ArrayList<Node>();
					path.add(v);
					while (v.getOutDegree() == 1) {
						v = v.getFirstOutEdge().getTarget();
						path.add(v);
					}

					if (path.size() == 1) {
						list.add((OcrWord) path.get(0).getInfo());
					} else {
						var buf = new StringBuilder();
						for (var w : path) {
							if (!buf.isEmpty()) {
								buf.append(' ');
							}
							buf.append(((OcrWord) w.getInfo()).text());
						}
						var minX = Double.MAX_VALUE;
						var maxX = Double.MIN_VALUE;
						var minY = Double.MAX_VALUE;
						var maxY = Double.MIN_VALUE;
						for (var w : path) {
							var bbox = ((OcrWord) w.getInfo()).boundingBox();
							minX = Math.min(minX, bbox.getMinX());
							maxX = Math.max(maxX, bbox.getMaxX());
							minY = Math.min(minY, bbox.getMinY());
							maxY = Math.max(maxY, bbox.getMaxY());
						}
						var confidence = (float) path.stream().mapToDouble(w -> ((OcrWord) w.getInfo()).confidence()).average().orElse(0.0);
						var rect = new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
						list.add(new OcrWord(buf.toString(), confidence, rect));
					}
					for (var w : path) {
						graph.deleteNode(w);
					}
					changed = true;
				}
			}
		}

		// add all remaining words
		for (var w : graph.nodes()) {
			list.add((OcrWord) w.getInfo());
		}

		return new ArrayList<>(list.stream().filter(w -> !(mustContainLetter && !containsLetter(w)
														   || mustStartWithAlphaNumeric && !startsAlphaNumeric(w)
														   || mustEndWithAlphaNumeric && !endsAlphaNumeric(w))).toList());
	}

	private static boolean containsLetter(OcrWord word) {
		return word.text().chars().anyMatch(Character::isLetter);
	}

	private static boolean startsAlphaNumeric(OcrWord word) {
		return !word.text().isEmpty() && Character.isLetterOrDigit(word.text().charAt(0));
	}

	private static boolean endsAlphaNumeric(OcrWord word) {
		return !word.text().isEmpty() && Character.isLetterOrDigit(word.text().charAt(word.text().length() - 1));
	}

	private static boolean intersect(double minI, double maxI, double minJ, double maxJ) {
		return minI <= minJ && maxI >= minJ || minI >= minJ && minI <= maxJ;
	}
}

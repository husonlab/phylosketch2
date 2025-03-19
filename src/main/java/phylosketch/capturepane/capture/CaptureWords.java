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

import jloda.graph.Graph;
import jloda.graph.Node;

import java.awt.*;
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
	public static List<Word> filter(Collection<Word> words, int minWordLength, double minTextHeight, double maxTextHeight,
									boolean mustStartWithAlphaNumeric, boolean mustEndWithAlphaNumeric, boolean mustContainLetter) {
		var list = new ArrayList<Word>();

		for (var word : words) {
			var awtRect = word.boundingBox();
			if (word.text().length() >= minWordLength && word.confidence() >= minTextHeight && awtRect.height <= maxTextHeight && word.text().matches(".*[a-zA-Z0-9].*")) {
				// trim leading non letter or digit characters
				var text = word.text();

				if (mustContainLetter && text.chars().noneMatch(Character::isLetter)) {
					continue;
				}

				var a = 0;
				if (mustStartWithAlphaNumeric) {
					while (a < text.length() && !Character.isLetterOrDigit(text.charAt(a))) {
						a++;
					}
				}
				var b = text.length();
				if (mustEndWithAlphaNumeric) {
					while (b > 0) {
						char c = text.charAt(b - 1);
						if (Character.isLetterOrDigit(c) || c == ')' || c == ']' || c == '}')
							break;
						b--;
					}
				}

				if (a < b) {
					word = new Word(text.substring(a, b).trim(), word.confidence(), awtRect);
					System.err.println("Extracted word:" + word);
					list.add(new Word(word.text(), word.confidence(), awtRect));
				}
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
	public static List<Word> joinConsecutiveWords(List<Word> words) {
		var graph = new Graph();

		for (var word : words) {
			graph.newNode(word);
		}
		for (var v : graph.nodes()) {
			var vWord = (Word) v.getInfo();
			var bboxI = vWord.boundingBox();
			for (var w : graph.nodes(v)) {
				var wWord = (Word) w.getInfo();
				var bboxJ = wWord.boundingBox();
				var dx = bboxJ.x - (bboxI.x + bboxI.width);
				if (intersect(bboxI.y, bboxI.y + bboxI.height, bboxJ.y, bboxJ.y + bboxJ.height) && dx >= 0 && dx <= 20) {
					graph.newEdge(v, w, dx);
				}
			}
		}

		var list = new ArrayList<Word>();

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
						list.add((Word) path.get(0).getInfo());
					} else {
						var buf = new StringBuilder();
						for (var w : path) {
							if (!buf.isEmpty()) {
								buf.append(' ');
							}
							buf.append(((Word) w.getInfo()).text());
						}
						var minX = Integer.MAX_VALUE;
						var maxX = Integer.MIN_VALUE;
						var minY = Integer.MAX_VALUE;
						var maxY = Integer.MIN_VALUE;
						for (var w : path) {
							var bbox = ((Word) w.getInfo()).boundingBox();
							minX = Math.min(minX, bbox.x);
							maxX = Math.max(maxX, bbox.x + bbox.width);
							minY = Math.min(minY, bbox.y);
							maxY = Math.max(maxY, bbox.y + bbox.height);
						}
						var confidence = (float) path.stream().mapToDouble(w -> ((Word) w.getInfo()).confidence()).average().orElse(0.0);
						var rect = new Rectangle(minX, minY, maxX - minX, maxY - minY);
						list.add(new Word(buf.toString(), confidence, rect));
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
			list.add((Word) w.getInfo());
		}
		return list;
	}

	private static boolean intersect(int minI, int maxI, int minJ, int maxJ) {
		return minI <= minJ && maxI >= minJ || minI >= minJ && minI <= maxJ;
	}
}

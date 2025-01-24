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

package phylocap.capture;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CaptureWords {
	public static void apply(Tesseract tesseract, int[][] binaryMatrix, BufferedImage awtBufferedImage, boolean tighten, double maxTextHeight, List<Word> words) {
		words.clear();
		for (var word : tesseract.getWords(awtBufferedImage, ITessAPI.TessPageIteratorLevel.RIL_WORD)) {
			var awtRect = word.getBoundingBox();
			if (word.getConfidence() >= 20 && awtRect.height <= maxTextHeight) {
				System.err.println("Extracted word:" + word);
				words.add(new Word(word.getText(), word.getConfidence(), awtRect));
			}
		}
		var joinedWords = new ArrayList<Word>();
		var used = new BitSet();
		for (var i = 0; i < words.size(); i++) {
			if (!used.get(i)) {
				var wordI = words.get(i);
				var bboxI = wordI.getBoundingBox();
				for (var j = 0; j < words.size(); j++) {
					if (!used.get(j)) {
						var wordJ = words.get(j);
						var bboxJ = wordJ.getBoundingBox();
						var dx = bboxJ.x - (bboxI.x + bboxI.width);
						if (intersect(bboxI.y, bboxI.y + bboxI.height, bboxJ.y, bboxJ.y + bboxJ.height) && dx >= 0 && dx <= 20) {
							used.set(i);
							used.set(j);
							joinedWords.add(new Word(wordI.getText() + " " + wordJ.getText(), 0.5f * (wordI.getConfidence() + wordJ.getConfidence()),
									new Rectangle(wordI.getBoundingBox().x, wordI.getBoundingBox().y, wordI.getBoundingBox().width + bboxJ.width + dx, Math.max(bboxI.height, bboxJ.height))));
							break;
						}
					}
				}
			}
		}
		for (var i = 0; i < words.size(); i++) {
			if (!used.get(i)) {
				joinedWords.add(words.get(i));
			}
		}
		words.clear();

		for (var word : joinedWords) {
			var awtRect = word.getBoundingBox();
			if (tighten) {
				ProcessingUtils.tightenRect(binaryMatrix, awtRect);
			}
			words.add(new Word(word.getText(), word.getConfidence(), awtRect));
		}
	}

	private static boolean intersect(int minI, int maxI, int minJ, int maxJ) {
		return minI <= minJ && maxI >= minJ || minI >= minJ && minI <= maxJ;
	}
}

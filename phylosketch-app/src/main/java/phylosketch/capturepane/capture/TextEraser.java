/*
 * TextEraser.java Copyright (C) 2026 Daniel H. Huson
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

import java.util.Collection;

/**
 * removes the ink of text from a binary matrix, so that it is not traced as nodes and edges
 * <p>
 * Only pieces of ink that lie completely inside the box of a word are removed. A line that passes through the
 * box, or ends in it, is a piece that reaches beyond the box and stays as it is: the box of a support value next
 * to a branch usually overlaps the branch, and a box that OCR reports where there is no text at all may cover
 * half the tree. Erasing the whole box, as was done before, cut the branch in the first case and removed that
 * part of the tree in the second.
 * Daniel Huson, 11.2026
 */
public class TextEraser {
	/**
	 * erases the text
	 *
	 * @param matrix the binary matrix, indexed [y][x]
	 * @param boxes  the boxes of the words, in pixels
	 * @param pad    number of pixels by which a piece of ink may stick out of a box and still be text
	 * @return the number of pieces erased
	 */
	public static int apply(int[][] matrix, Collection<Rectangle2D> boxes, int pad) {
		if (boxes.isEmpty() || matrix.length == 0)
			return 0;
		var components = new Components(matrix);
		var erased = 0;
		for (var c = 1; c <= components.count(); c++) {
			for (var box : boxes) {
				if (components.minX(c) >= Math.floor(box.getMinX()) - pad && components.maxX(c) <= Math.ceil(box.getMaxX()) + pad
					&& components.minY(c) >= Math.floor(box.getMinY()) - pad && components.maxY(c) <= Math.ceil(box.getMaxY()) + pad) {
					components.erase(matrix, c);
					erased++;
					break;
				}
			}
		}
		return erased;
	}

	/**
	 * erases every piece of ink that is no larger than a letter. Once dashed lines have been joined and the words
	 * that OCR found have been erased, such a piece is connected to nothing and is not part of the drawing: it is a
	 * letter that OCR did not find, a speck, or a symbol
	 *
	 * @param matrix the binary matrix, indexed [y][x]
	 * @param size   the largest width and height of a piece that is erased
	 * @return the number of pieces erased
	 */
	public static int eraseSmallPieces(int[][] matrix, int size) {
		if (matrix.length == 0)
			return 0;
		var components = new Components(matrix);
		var erased = 0;
		for (var c = 1; c <= components.count(); c++) {
			if (components.width(c) <= size && components.height(c) <= size) {
				components.erase(matrix, c);
				erased++;
			}
		}
		return erased;
	}
}

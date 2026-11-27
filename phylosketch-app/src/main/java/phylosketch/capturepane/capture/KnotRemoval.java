/*
 * KnotRemoval.java Copyright (C) 2026 Daniel H. Huson
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
 */package phylosketch.capturepane.capture;

import javafx.geometry.Rectangle2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * removes small knots of segments that hang on the drawing at one point
 * <p>
 * A letter or digit that touches a line cannot be erased as text, because it is one piece of ink with the line.
 * It is traced as a few short segments that hang on the line where the two touch. Such a knot is removed if it
 * is no larger than a letter and either lies in the box of a word that OCR found, or has a loop or at least four
 * segments. Two short branches at the end of a branch, as of taxa that do not differ, are neither. A loop that
 * hangs on the drawing at one point and has a word inside it is the frame of a label, and is removed as well:
 * the branch then ends where it met the frame.
 * Daniel Huson, 11.2026
 */
public class KnotRemoval {
	// ends of segments within this distance of each other are one point
	private static final double SAME_POINT = 3;
	// a knot that is not in the box of a word has at least this many segments, or a loop
	private static final int MIN_SEGMENTS = 4;
	// a loop around a word is the frame of that word if it is at most this many times as large as the word
	private static final double FRAME_PER_WORD = 3;

	/**
	 * removes the knots
	 *
	 * @param segments  the segments
	 * @param textBoxes the boxes of the words that OCR found
	 * @param maxSize   the largest width and height of a knot
	 * @return the segments that are left
	 */
	public static ArrayList<Segment> apply(List<Segment> segments, Collection<Rectangle2D> textBoxes, double maxSize) {
		var n = segments.size();
		// the points at which segments end
		var points = new ArrayList<Point>();
		var from = new int[n];
		var to = new int[n];
		for (var i = 0; i < n; i++) {
			from[i] = pointOf(points, segments.get(i).first());
			to[i] = pointOf(points, segments.get(i).last());
		}
		var m = points.size();
		var remove = new boolean[n];

		var component = new int[m];
		var stack = new int[m];
		for (var cut = 0; cut < m; cut++) {
			// the parts that the drawing falls into without this point
			java.util.Arrays.fill(component, -1);
			var count = 0;
			for (var start = 0; start < m; start++) {
				if (start == cut || component[start] != -1)
					continue;
				var top = 0;
				stack[top++] = start;
				component[start] = count;
				while (top > 0) {
					var p = stack[--top];
					for (var i = 0; i < n; i++) {
						if (remove[i])
							continue;
						var q = (from[i] == p ? to[i] : (to[i] == p ? from[i] : -1));
						if (q != -1 && q != cut && component[q] == -1) {
							component[q] = count;
							stack[top++] = q;
						}
					}
				}
				count++;
			}
			if (count < 2)
				continue;
			// the size of each part: its segments, including those that join it to the point
			var size = new int[count];
			var nodes = new int[count];
			var minX = new double[count];
			var minY = new double[count];
			var maxX = new double[count];
			var maxY = new double[count];
			java.util.Arrays.fill(minX, Double.MAX_VALUE);
			java.util.Arrays.fill(minY, Double.MAX_VALUE);
			java.util.Arrays.fill(maxX, -Double.MAX_VALUE);
			java.util.Arrays.fill(maxY, -Double.MAX_VALUE);
			for (var p = 0; p < m; p++) {
				if (p != cut)
					nodes[component[p]]++;
			}
			for (var i = 0; i < n; i++) {
				if (remove[i])
					continue;
				var c = (from[i] != cut ? component[from[i]] : (to[i] != cut ? component[to[i]] : -1));
				if (c == -1)
					continue; // a loop at the point itself
				size[c]++;
				var box = segments.get(i).computeBoundingBox();
				minX[c] = Math.min(minX[c], box.getMinX());
				minY[c] = Math.min(minY[c], box.getMinY());
				maxX[c] = Math.max(maxX[c], box.getMaxX());
				maxY[c] = Math.max(maxY[c], box.getMaxY());
			}
			var largest = 0;
			for (var c = 1; c < count; c++) {
				if (size[c] > size[largest])
					largest = c;
			}
			for (var c = 0; c < count; c++) {
				if (c == largest || size[c] == 0)
					continue;
				var loop = (size[c] > nodes[c]); // more segments than points
				if (loop && isFrame(minX[c], minY[c], maxX[c], maxY[c], textBoxes)) {
					for (var i = 0; i < n; i++) {
						if (!remove[i] && ((from[i] != cut && component[from[i]] == c) || (to[i] != cut && component[to[i]] == c)))
							remove[i] = true;
					}
					continue;
				}
				if (maxX[c] - minX[c] > maxSize || maxY[c] - minY[c] > maxSize)
					continue;
				var knot = (size[c] >= MIN_SEGMENTS || loop);
				if (!knot) {
					for (var box : textBoxes) {
						if (minX[c] >= box.getMinX() - 2 && maxX[c] <= box.getMaxX() + 2 && minY[c] >= box.getMinY() - 2 && maxY[c] <= box.getMaxY() + 2) {
							knot = true;
							break;
						}
					}
				}
				if (knot) {
					for (var i = 0; i < n; i++) {
						if (!remove[i] && ((from[i] != cut && component[from[i]] == c) || (to[i] != cut && component[to[i]] == c)))
							remove[i] = true;
					}
				}
			}
		}
		var result = new ArrayList<Segment>();
		for (var i = 0; i < n; i++) {
			if (!remove[i])
				result.add(segments.get(i));
		}
		return result;
	}

	/**
	 * is this the frame of a label: does the box lie around a word, and is it not much larger than the word?
	 */
	private static boolean isFrame(double minX, double minY, double maxX, double maxY, Collection<Rectangle2D> textBoxes) {
		for (var box : textBoxes) {
			var x = 0.5 * (box.getMinX() + box.getMaxX());
			var y = 0.5 * (box.getMinY() + box.getMaxY());
			var size = Math.max(box.getWidth(), box.getHeight());
			if (x > minX && x < maxX && y > minY && y < maxY && maxX - minX <= FRAME_PER_WORD * size && maxY - minY <= FRAME_PER_WORD * size)
				return true;
		}
		return false;
	}

	private static int pointOf(ArrayList<Point> points, Point point) {
		for (var i = 0; i < points.size(); i++) {
			if (points.get(i).distance(point) <= SAME_POINT)
				return i;
		}
		points.add(point);
		return points.size() - 1;
	}
}

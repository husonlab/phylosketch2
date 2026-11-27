/*
 * DashConnector.java Copyright (C) 2026 Daniel H. Huson
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * joins the dashes and dots of dashed and dotted lines, so that such a line is traced as one line
 * <p>
 * Each small piece of ink is thinned to its skeleton: a dash becomes a short path with two ends, a dot a pixel
 * or two. Two dashes are linked if an end of the one and an end of the other point at each other across a gap
 * that is small for their length, or, at a corner of the line, if the two ends point at the same place. Dots are
 * linked to their nearest neighbours on either side. A run of linked dashes or dots is taken to be a dashed line
 * only if it has at least three members (four dots), or fewer and continues into a line at one end: a hyphen,
 * the stem of a letter or a tick on its own is left alone, and so is any dash or dot that has a letter right
 * next to it, or lies in a word that OCR read with confidence. The gaps of a run are then drawn into the ink,
 * and each open end of the run is extended straight on, by no more than about one gap, to the line that it
 * starts from or runs into.
 * <p>
 * This has to be done before text is erased: OCR reports dashes and dots as words, some of them with letters,
 * and a dash lies completely inside the box of such a word.
 * Daniel Huson, 11.2026
 */
public class DashConnector {
	// a piece of skeleton can be a dash if it is at most this long, as a proportion of the longer side of the picture, or in pixels if that is more
	private static final double MAX_LENGTH_PROPORTION = 0.04;
	private static final int MIN_MAX_LENGTH = 40;
	// the largest gap between two dashes: this multiple of the longer of the two, plus a few pixels, since thinning shortens a dash at both ends
	private static final double GAP_PER_LENGTH = 2.5;
	private static final int GAP_EXTRA = 5;
	// the largest gap between two dots: this multiple of the diameter of the larger one, or a few pixels if that is more
	private static final double GAP_PER_DIAMETER = 4.5;
	private static final int MIN_DOT_GAP = 12;
	// a piece of ink is a dot if it is about as wide as high and at most this large, as a proportion of the longer side of the picture, or in pixels if that is more
	private static final double MAX_DOT_PROPORTION = 0.006;
	private static final int MIN_MAX_DOT = 6;
	// two ends point at each other if each lies within this angle of the direction of the other
	private static final double COS_FACING = Math.cos(Math.toRadians(32));
	// the pixels at the end of a dash that give its direction there
	private static final int TANGENT_LENGTH = 6;
	// a piece is a dash only if its ends are this far apart, as a proportion of its length: this lets a dash bend round a corner, but excludes letters such as c and s
	private static final double MIN_CHORD_PROPORTION = 0.6;
	// a dash is the stroke of a letter if a branching or curved piece of ink lies within this proportion of its length from it (and a dot, of its diameter)
	private static final double LETTER_GAP_PER_LENGTH = 0.3;
	private static final double MIN_LETTER_GAP = 1.5;
	private static final double MIN_ARM_GAP = 2.5;
	private static final double LETTER_GAP_PER_DIAMETER = 1.0;
	// an open end of a run is extended by at most this multiple of the median gap of the run, plus a few pixels
	private static final double EXTENSION_PER_GAP = 1.15;
	private static final int EXTENSION_EXTRA = 4;
	// two open ends that run into a line from opposite sides are one dashed line that crosses it if they meet it this close to each other: this multiple of the length of a dash, or this many pixels if that is more
	private static final double CROSSING_PER_LENGTH = 1.5;
	private static final int MIN_CROSSING_WIDTH = 10;
	// a small piece of ink is a symbol, such as an arrowhead, if it is at least this wide and high and mostly filled
	private static final int MIN_SYMBOL_SIZE = 4;

	private static final class Element {
		boolean dot;
		int component; // the piece of ink
		int length; // number of pixels of its skeleton
		double diameter; // of the ink that a dot was thinned from
		final double[] x = new double[2]; // the two ends; for a dot, twice its centre
		final double[] y = new double[2];
		final double[] tx = new double[2]; // direction out of each end, of unit length
		final double[] ty = new double[2];
		// what each end is linked to: element and end, or -1; for a dot, its (up to) two neighbours
		final int[] linkElement = {-1, -1};
		final int[] linkEnd = {-1, -1};
		final double[] linkGap = {0, 0};
		final double[][] corner = {null, null}; // the corner that the bridge goes through, if any
		final boolean[] attached = {false, false}; // this end is where the piece was taken off a branching piece of ink
		int chain = -1;
	}

	/**
	 * joins dashed and dotted lines
	 *
	 * @param ink  the ink, indexed [y][x]; the gaps are drawn into it
	 * @param text the boxes of the words that OCR read with confidence: the ink in them is text, whatever its shape
	 * @return the places where a dashed line crosses another line
	 */
	public static List<Point> apply(int[][] ink, Collection<Rectangle2D> text) {
		var crossings = new ArrayList<Point>();
		var height = ink.length;
		if (height == 0)
			return crossings;
		var width = ink[0].length;
		var maxLength = Math.max(MIN_MAX_LENGTH, (int) Math.round(MAX_LENGTH_PROPORTION * Math.max(width, height)));

		// only a piece of ink that is small in both directions can be a dash or a dot; only those are thinned
		var inkComponents = new Components(ink);
		var small = new boolean[inkComponents.count() + 1];
		var matrix = new int[height][width];
		var any = false;
		var elements = new ArrayList<Element>();
		var maxDot = Math.max(MIN_MAX_DOT, (int) Math.round(MAX_DOT_PROPORTION * Math.max(width, height)));
		var isText = new boolean[inkComponents.count() + 1];
		for (var c = 1; c <= inkComponents.count(); c++) {
			if (inkComponents.width(c) <= maxLength && inkComponents.height(c) <= maxLength) {
				small[c] = true;
				for (var box : text) {
					if (inkComponents.minX(c) >= box.getMinX() - 1 && inkComponents.maxX(c) <= box.getMaxX() + 1 && inkComponents.minY(c) >= box.getMinY() - 1 && inkComponents.maxY(c) <= box.getMaxY() + 1) {
						isText[c] = true;
						break;
					}
				}
				if (isText[c])
					continue;
				any = true;
				var w = inkComponents.width(c);
				var h = inkComponents.height(c);
				if (Math.max(w, h) <= maxDot && 10 * Math.max(w, h) <= 16 * Math.min(w, h) && (Math.max(w, h) <= 4 || 100 * inkComponents.area(c) >= 65 * w * h)) {
					// a dot is taken as it is: thinning removes some dots of this size altogether
					var dot = new Element();
					dot.dot = true;
					dot.component = c;
					dot.length = 1;
					dot.diameter = 2 * Math.sqrt(inkComponents.area(c) / Math.PI);
					var sumX = 0.0;
					var sumY = 0.0;
					for (var y = inkComponents.minY(c); y <= inkComponents.maxY(c); y++) {
						for (var x = inkComponents.minX(c); x <= inkComponents.maxX(c); x++) {
							if (inkComponents.get(x, y) == c) {
								sumX += x;
								sumY += y;
							}
						}
					}
					dot.x[0] = dot.x[1] = sumX / inkComponents.area(c);
					dot.y[0] = dot.y[1] = sumY / inkComponents.area(c);
					elements.add(dot);
					continue;
				}
				for (var y = inkComponents.minY(c); y <= inkComponents.maxY(c); y++) {
					for (var x = inkComponents.minX(c); x <= inkComponents.maxX(c); x++) {
						if (inkComponents.get(x, y) == c)
							matrix[y][x] = 1;
					}
				}
			}
		}
		if (!any)
			return crossings;
		Skeletonization.apply(matrix);

		// a piece that branches, as where dashed lines meet, is taken apart into its arms: each arm is a dash of
		// its own, and the arms are connected in the ink anyway
		var branch = new boolean[height][width];
		for (var y = 1; y < height - 1; y++) {
			for (var x = 1; x < width - 1; x++) {
				if (matrix[y][x] != 0 && countArms(matrix, x, y) >= 3)
					branch[y][x] = true;
			}
		}
		for (var y = 1; y < height - 1; y++) {
			for (var x = 1; x < width - 1; x++) {
				if (branch[y][x])
					matrix[y][x] = 0;
			}
		}
		var components = new Components(matrix);

		// small pieces of ink that are neither a dot nor one dash: they branch, or are curved, as most letters are
		var complex = isText;
		for (var y = 1; y < height - 1; y++) {
			for (var x = 1; x < width - 1; x++) {
				if (branch[y][x])
					complex[inkComponents.get(x, y)] = true;
			}
		}
		for (var c = 1; c <= components.count(); c++) {
			var element = createElement(components, c, inkComponents, branch);
			if (element != null)
				elements.add(element);
			else if (components.area(c) > 3)
				complex[inkComponentOf(components, c, inkComponents)] = true;
		}

		// A dash or dot is part of a word, not of a dashed line, if it has such a piece right next to it: the stem of
		// an l or an i, a hyphen, a full stop. Next to a dash of a dashed line there is nothing nearer than the next
		// gap. An arm of a branching piece, and a dot, is part of a word if it has any small piece next to it that is
		// not a dot: letters stand closer to each other than the dashes of a line do
		var isDot = new boolean[inkComponents.count() + 1];
		for (var e : elements) {
			if (e.dot)
				isDot[e.component] = true;
		}
		var smallPieces = new ArrayList<Integer>();
		for (var c = 1; c <= inkComponents.count(); c++) {
			if (small[c] && !isDot[c])
				smallPieces.add(c);
		}
		// what turns out to be part of a word is a letter for its neighbours in turn, until nothing changes
		var changed = true;
		while (changed) {
			var complexPieces = new ArrayList<Integer>();
			for (var c = 1; c <= inkComponents.count(); c++) {
				if (complex[c])
					complexPieces.add(c);
			}
			var letters = new ArrayList<Element>();
			for (var e : elements) {
				var reach = (e.dot ? LETTER_GAP_PER_DIAMETER * e.diameter : Math.max(MIN_LETTER_GAP, LETTER_GAP_PER_LENGTH * e.length));
				var letter = false;
				for (var c : complexPieces) {
					if (c != e.component && Math.max(gapX(inkComponents, c, e.component), gapY(inkComponents, c, e.component)) <= reach) {
						letter = true;
						break;
					}
				}
				if (!letter && (e.dot || complex[e.component])) {
					if (!e.dot)
						reach = Math.max(reach, MIN_ARM_GAP);
					for (var c : smallPieces) {
						if (c != e.component && Math.max(gapX(inkComponents, c, e.component), gapY(inkComponents, c, e.component)) <= reach) {
							letter = true;
							break;
						}
					}
				}
				if (letter)
					letters.add(e);
			}
			changed = false;
			for (var e : letters) {
				if (!complex[e.component]) {
					complex[e.component] = true;
					changed = true;
				}
			}
			elements.removeAll(letters);
		}
		if (elements.size() < 2)
			return crossings;
		var isElement = new boolean[inkComponents.count() + 1];
		for (var e : elements)
			isElement[e.component] = true;

		link(elements);

		// runs of linked elements
		var chains = new ArrayList<List<Element>>();
		for (var element : elements) {
			if (element.chain == -1) {
				var chain = new ArrayList<Element>();
				var queue = new ArrayDeque<Element>();
				element.chain = chains.size();
				queue.add(element);
				while (!queue.isEmpty()) {
					var e = queue.poll();
					chain.add(e);
					for (var end = 0; end < 2; end++) {
						if (e.linkElement[end] != -1) {
							var other = elements.get(e.linkElement[end]);
							if (other.chain == -1) {
								other.chain = element.chain;
								queue.add(other);
							}
						}
					}
				}
				chains.add(chain);
			}
		}

		// which runs are dashed lines by their number of members; a run of two dashes may still become one below
		var accepted = new boolean[chains.size()];
		var lineOfComponent = new int[inkComponents.count() + 1]; // the run that a piece of ink belongs to, plus 1, if that run is a dashed line
		for (var i = 0; i < chains.size(); i++) {
			var chain = chains.get(i);
			accepted[i] = (chain.size() >= (chain.stream().allMatch(e -> e.dot) ? 4 : 3));
			if (accepted[i]) {
				for (var e : chain)
					lineOfComponent[e.component] = i + 1;
			}
		}

		for (var i = 0; i < chains.size(); i++) {
			if (accepted[i])
				findCrossings(ink, inkComponents, small, lineOfComponent, i + 1, elements, chains.get(i), crossings);
		}
		for (var i = 0; i < chains.size(); i++) {
			if (accepted[i])
				drawGaps(ink, elements, chains.get(i));
		}
		var straight = new ArrayList<double[]>(); // where an open end runs straight into a line: place, direction, length of a dash

		// each open end of a run is extended straight on to the ink that it runs into: a line, or another dashed line.
		// A run that is too short to be a dashed line by itself is one if it starts from, or runs into, a line: two
		// dashes, or two or three dots, at any line; a single dash only at a dashed line
		for (var i = 0; i < chains.size(); i++) {
			var chain = chains.get(i);
			var dots = chain.stream().allMatch(e -> e.dot);
			if (!accepted[i] && dots && chain.size() < 2)
				continue;
			var gaps = new ArrayList<Double>();
			for (var e : chain) {
				for (var end = 0; end < 2; end++) {
					if (e.linkElement[end] != -1)
						gaps.add(e.linkGap[end]);
				}
			}
			gaps.sort(Double::compare);
			var medianGap = (gaps.isEmpty() ? chain.get(0).length : gaps.get(gaps.size() / 2));
			// a dot may be missing at the end of a dotted line as anywhere else in it
			var reach = (dots ? 2.2 : EXTENSION_PER_GAP) * medianGap + EXTENSION_EXTRA;

			var extensions = new ArrayList<double[]>();
			var hits = new ArrayList<double[]>();
			var lengths = chain.stream().mapToDouble(e -> e.length).sorted().toArray();
			var medianLength = lengths[lengths.length / 2];
			for (var e : chain) {
				for (var end = 0; end < 2; end++) {
					if (e.linkElement[end] != -1 || e.attached[end])
						continue;
					double dx, dy;
					if (e.dot) { // straight on from the neighbouring dot
						if (e.linkElement[1 - end] == -1)
							continue;
						var other = elements.get(e.linkElement[1 - end]);
						var otherEnd = Math.max(0, e.linkEnd[1 - end]);
						dx = e.x[0] - other.x[otherEnd];
						dy = e.y[0] - other.y[otherEnd];
						var norm = Math.hypot(dx, dy);
						if (norm == 0)
							continue;
						dx /= norm;
						dy /= norm;
					} else {
						dx = e.tx[end];
						dy = e.ty[end];
					}
					var dashedOnly = (!accepted[i] && chain.size() == 1);
					var hit = findInk(ink, inkComponents, small, lineOfComponent, i + 1, e.x[end], e.y[end], dx, dy, reach, dashedOnly);
					if (hit != null) {
						extensions.add(new double[]{e.x[end], e.y[end], hit[0], hit[1]});
						hits.add(new double[]{hit[0], hit[1], dx, dy, medianLength});
					} else if (!e.dot) {
						// an arrowhead, or another solid symbol at the end of the line: the line goes on through it
						var symbol = (dashedOnly ? null : findSymbol(ink, inkComponents, small, isText, isElement, e.x[end], e.y[end], dx, dy, reach));
						if (symbol != null) {
							extensions.add(new double[]{e.x[end], e.y[end], symbol[0], symbol[1]});
							lineOfComponent[(int) symbol[4]] = i + 1;
							var beyond = findInk(ink, inkComponents, small, lineOfComponent, i + 1, symbol[2], symbol[3], dx, dy, reach, false);
							if (beyond != null)
								extensions.add(new double[]{symbol[2], symbol[3], beyond[0], beyond[1]});
							continue;
						}
						var corner = findEndRoundCorner(ink, inkComponents, small, lineOfComponent, i + 1, e.x[end], e.y[end], dx, dy, reach, dashedOnly);
						if (corner != null) {
							extensions.add(new double[]{e.x[end], e.y[end], corner[0], corner[1]});
							extensions.add(new double[]{corner[0], corner[1], corner[2], corner[3]});
						}
					}
				}
			}
			if (!accepted[i]) {
				if (extensions.isEmpty())
					continue;
				findCrossings(ink, inkComponents, small, lineOfComponent, i + 1, elements, chain, crossings);
				drawGaps(ink, elements, chain);
			}
			for (var extension : extensions)
				drawLine(ink, extension[0], extension[1], extension[2], extension[3]);
			straight.addAll(hits);
		}

		// where two open ends run into the same line from opposite sides, the dashed line crosses that line
		for (var i = 0; i < straight.size(); i++) {
			for (var j = i + 1; j < straight.size(); j++) {
				var a = straight.get(i);
				var b = straight.get(j);
				if (a[2] * b[2] + a[3] * b[3] <= -0.8 && Math.hypot(a[0] - b[0], a[1] - b[1]) <= Math.max(MIN_CROSSING_WIDTH, CROSSING_PER_LENGTH * Math.max(a[4], b[4])))
					crossings.add(new Point((int) Math.round(0.5 * (a[0] + b[0])), (int) Math.round(0.5 * (a[1] + b[1]))));
			}
		}
		return crossings;
	}

	/**
	 * finds where the gaps of a run, once drawn in, pass over a line that is not part of the run
	 */
	private static void findCrossings(int[][] ink, Components components, boolean[] small, int[] lineOfComponent, int run, List<Element> elements, List<Element> chain, List<Point> crossings) {
		for (var e : chain) {
			for (var end = 0; end < 2; end++) {
				if (e.linkElement[end] == -1 || e.corner[end] != null || elements.indexOf(e) > e.linkElement[end])
					continue;
				var other = elements.get(e.linkElement[end]);
				var otherEnd = Math.max(0, e.linkEnd[end]);
				var length = Math.hypot(other.x[otherEnd] - e.x[end], other.y[otherEnd] - e.y[end]);
				for (var t = 2; t <= length - 2; t++) {
					var x = (int) Math.round(e.x[end] + t * (other.x[otherEnd] - e.x[end]) / length);
					var y = (int) Math.round(e.y[end] + t * (other.y[otherEnd] - e.y[end]) / length);
					if (y < 0 || y >= ink.length || x < 0 || x >= ink[0].length || ink[y][x] == 0)
						continue;
					var c = components.get(x, y);
					if (c != 0 && c != e.component && c != other.component && (!small[c] || (lineOfComponent[c] != 0 && lineOfComponent[c] != run))) {
						crossings.add(new Point(x, y));
						break;
					}
				}
			}
		}
	}

	/**
	 * walks straight on from an open end of a run, and returns the first small solid piece of ink that is not a
	 * letter, a dash or a dot: an arrowhead, or a node symbol
	 *
	 * @return where the piece is met, where it ends in the direction of the walk, and its number, or null
	 */
	private static double[] findSymbol(int[][] ink, Components components, boolean[] small, boolean[] isText, boolean[] isElement, double x, double y, double dx, double dy, double reach) {
		var height = ink.length;
		var width = ink[0].length;
		for (var s = 2; s <= reach; s++) {
			var xx = (int) Math.round(x + s * dx);
			var yy = (int) Math.round(y + s * dy);
			if (xx < 0 || xx >= width || yy < 0 || yy >= height || ink[yy][xx] == 0)
				continue;
			var c = components.get(xx, yy);
			if (c == 0 || !small[c] || isText[c] || isElement[c])
				return null;
			var w = components.width(c);
			var h = components.height(c);
			if (Math.min(w, h) < MIN_SYMBOL_SIZE || 100 * components.area(c) < 35 * w * h)
				return null;
			// the far end of the piece, along the walk
			var best = Double.NEGATIVE_INFINITY;
			var farX = xx;
			var farY = yy;
			for (var py = components.minY(c); py <= components.maxY(c); py++) {
				for (var px = components.minX(c); px <= components.maxX(c); px++) {
					if (components.get(px, py) == c) {
						var along = (px - x) * dx + (py - y) * dy;
						var across = Math.abs((px - x) * dy - (py - y) * dx);
						if (along - across > best) { // the farthest pixel that is near the line of the walk
							best = along - across;
							farX = px;
							farY = py;
						}
					}
				}
			}
			return new double[]{xx, yy, farX, farY, c};
		}
		return null;
	}
	/**
	 * draws in the gaps of a run
	 */
	private static int drawGaps(int[][] matrix, List<Element> elements, List<Element> chain) {
		var count = 0;
		for (var e : chain) {
			for (var end = 0; end < 2; end++) {
				if (e.linkElement[end] != -1) { // seen from both sides, drawn twice
					var other = elements.get(e.linkElement[end]);
					var otherEnd = Math.max(0, e.linkEnd[end]);
					if (e.corner[end] != null) {
						drawLine(matrix, e.x[end], e.y[end], e.corner[end][0], e.corner[end][1]);
						drawLine(matrix, e.corner[end][0], e.corner[end][1], other.x[otherEnd], other.y[otherEnd]);
					} else
						drawLine(matrix, e.x[end], e.y[end], other.x[otherEnd], other.y[otherEnd]);
					count++;
				}
			}
		}
		return count / 2;
	}

	/**
	 * describes a small piece of skeleton as a dash or a dot, or returns null if it is neither
	 */
	private static Element createElement(Components components, int c, Components inkComponents, boolean[][] branch) {
		var n = components.area(c);
		var w = components.width(c);
		var h = components.height(c);
		var x0 = components.minX(c);
		var y0 = components.minY(c);

		// the pixels of the piece, and a grid to look them up in
		var xs = new int[n];
		var ys = new int[n];
		var grid = new int[h][w];
		for (var row : grid)
			Arrays.fill(row, -1);
		var k = 0;
		for (var y = 0; y < h; y++) {
			for (var x = 0; x < w; x++) {
				if (components.get(x0 + x, y0 + y) == c) {
					xs[k] = x;
					ys[k] = y;
					grid[y][x] = k++;
				}
			}
		}

		var element = new Element();
		element.component = inkComponents.get(x0 + xs[0], y0 + ys[0]);
		element.length = n;
		element.diameter = 2 * Math.sqrt(inkComponents.area(element.component) / Math.PI);

		if (n <= 3 && w <= 3 && h <= 3) {
			for (var i = 0; i < n; i++) {
				if (nextToBranch(branch, x0 + xs[i], y0 + ys[i]))
					return null; // what is left of an arm, not a dot
			}
			element.dot = true;
			element.x[0] = element.x[1] = x0 + Arrays.stream(xs).average().orElse(0);
			element.y[0] = element.y[1] = y0 + Arrays.stream(ys).average().orElse(0);
			return element;
		}

		// the longest path through the piece: from any pixel to the one farthest from it, and from there to the one farthest from that
		var previous = new int[n];
		var a = farthest(grid, xs, ys, 0, previous);
		var b = farthest(grid, xs, ys, a, previous);
		var path = new ArrayList<Integer>();
		for (var p = b; p != -1; p = previous[p])
			path.add(p);
		if (path.size() < 0.8 * n)
			return null; // branches, or is a loop
		var first = path.get(0);
		var last = path.get(path.size() - 1);
		if (Math.hypot(xs[first] - xs[last], ys[first] - ys[last]) < MIN_CHORD_PROPORTION * path.size())
			return null; // too curly

		// a short piece may bend round a corner, so its direction at an end is taken from a third of it at most
		var back = Math.min(path.size() - 1, Math.max(2, Math.min(TANGENT_LENGTH, path.size() / 3)));
		for (var end = 0; end < 2; end++) {
			var tip = (end == 0 ? first : last);
			var inner = path.get(end == 0 ? back : path.size() - 1 - back);
			element.x[end] = x0 + xs[tip];
			element.y[end] = y0 + ys[tip];
			var dx = xs[tip] - xs[inner];
			var dy = ys[tip] - ys[inner];
			var norm = Math.hypot(dx, dy);
			element.tx[end] = dx / norm;
			element.ty[end] = dy / norm;
			element.attached[end] = nextToBranch(branch, x0 + xs[tip], y0 + ys[tip]);
		}
		return element;
	}

	/**
	 * the gap between the bounding boxes of two pieces of ink along x; negative if they overlap
	 */
	private static int gapX(Components components, int a, int b) {
		return Math.max(components.minX(a), components.minX(b)) - Math.min(components.maxX(a), components.maxX(b)) - 1;
	}

	private static int gapY(Components components, int a, int b) {
		return Math.max(components.minY(a), components.minY(b)) - Math.min(components.maxY(a), components.maxY(b)) - 1;
	}

	/**
	 * the piece of ink that a piece of skeleton was thinned from
	 */
	private static int inkComponentOf(Components components, int c, Components inkComponents) {
		for (var y = components.minY(c); y <= components.maxY(c); y++) {
			for (var x = components.minX(c); x <= components.maxX(c); x++) {
				if (components.get(x, y) == c)
					return inkComponents.get(x, y);
			}
		}
		return 0;
	}

	private static boolean nextToBranch(boolean[][] branch, int x, int y) {
		for (var dy = -1; dy <= 1; dy++) {
			for (var dx = -1; dx <= 1; dx++) {
				if (y + dy >= 0 && y + dy < branch.length && x + dx >= 0 && x + dx < branch[0].length && branch[y + dy][x + dx])
					return true;
			}
		}
		return false;
	}

	/**
	 * the number of separate runs of ink among the eight neighbours of a pixel, that is, the number of lines that leave it
	 */
	private static int countArms(int[][] matrix, int x, int y) {
		int[] ring = {matrix[y - 1][x], matrix[y - 1][x + 1], matrix[y][x + 1], matrix[y + 1][x + 1], matrix[y + 1][x], matrix[y + 1][x - 1], matrix[y][x - 1], matrix[y - 1][x - 1]};
		var arms = 0;
		for (var i = 0; i < 8; i++) {
			if (ring[i] == 0 && ring[(i + 1) % 8] != 0)
				arms++;
		}
		return arms;
	}

	/**
	 * breadth-first search over the pixels of a piece
	 *
	 * @return the pixel reached last; previous then holds the way back to the start
	 */
	private static int farthest(int[][] grid, int[] xs, int[] ys, int start, int[] previous) {
		var h = grid.length;
		var w = grid[0].length;
		var seen = new boolean[xs.length];
		var queue = new int[xs.length];
		var head = 0;
		var tail = 0;
		queue[tail++] = start;
		seen[start] = true;
		previous[start] = -1;
		var last = start;
		while (head < tail) {
			var p = queue[head++];
			last = p;
			// neighbours at a side before those at a corner, so that the path does not cut corners
			for (var round = 0; round < 2; round++) {
				for (var dy = -1; dy <= 1; dy++) {
					for (var dx = -1; dx <= 1; dx++) {
						if ((dx == 0 && dy == 0) || ((dx == 0 || dy == 0) != (round == 0)))
							continue;
						var x = xs[p] + dx;
						var y = ys[p] + dy;
						if (x >= 0 && x < w && y >= 0 && y < h && grid[y][x] != -1 && !seen[grid[y][x]]) {
							var q = grid[y][x];
							seen[q] = true;
							previous[q] = p;
							queue[tail++] = q;
						}
					}
				}
			}
		}
		return last;
	}

	/**
	 * links the ends of dashes that point at each other or at a common corner, and dots to their neighbours
	 */
	private static void link(List<Element> elements) {
		var n = elements.size();
		// for each end of each dash: the best partner
		var bestElement = new int[n][2];
		var bestEnd = new int[n][2];
		var bestScore = new double[n][2];
		var bestGap = new double[n][2];
		var bestCorner = new double[n][2][];
		for (var i = 0; i < n; i++) {
			Arrays.fill(bestElement[i], -1);
			Arrays.fill(bestScore[i], Double.MAX_VALUE);
		}

		for (var i = 0; i < n; i++) {
			var a = elements.get(i);
			if (a.dot)
				continue;
			for (var j = 0; j < n; j++) {
				var b = elements.get(j);
				if (i == j || a.component == b.component)
					continue;
				var maxGap = (b.dot ? Math.max(GAP_PER_LENGTH * a.length + GAP_EXTRA, dotGap(b, b)) : GAP_PER_LENGTH * Math.max(a.length, b.length) + GAP_EXTRA);
				if (Math.abs(a.x[0] - b.x[0]) > maxGap + a.length + b.length || Math.abs(a.y[0] - b.y[0]) > maxGap + a.length + b.length)
					continue;
				for (var ea = 0; ea < 2; ea++) {
					if (a.attached[ea])
						continue;
					for (var eb = 0; eb < (b.dot ? 1 : 2); eb++) {
						if (b.attached[eb])
							continue;
						var gx = b.x[eb] - a.x[ea];
						var gy = b.y[eb] - a.y[ea];
						var gap = Math.hypot(gx, gy);
						if (gap > maxGap || gap == 0)
							continue;
						var towards = (gx * a.tx[ea] + gy * a.ty[ea]) / gap;
						double score;
						double[] corner = null;
						if (b.dot) {
							if (towards < COS_FACING)
								continue;
							score = gap;
						} else {
							var back = -(gx * b.tx[eb] + gy * b.ty[eb]) / gap;
							if (towards >= COS_FACING && back >= COS_FACING) {
								score = gap * (2 - 0.5 * (towards + back)); // straight on is better
							} else {
								// a corner: the two ends point at the same place, at about a right angle
								corner = intersect(a.x[ea], a.y[ea], a.tx[ea], a.ty[ea], b.x[eb], b.y[eb], b.tx[eb], b.ty[eb], maxGap);
								if (corner == null || Math.abs(a.tx[ea] * b.tx[eb] + a.ty[ea] * b.ty[eb]) > 0.5)
									continue;
								gap = Math.hypot(corner[0] - a.x[ea], corner[1] - a.y[ea]) + Math.hypot(corner[0] - b.x[eb], corner[1] - b.y[eb]);
								if (gap > maxGap)
									continue;
								score = 1.5 * gap;
							}
						}
						if (score < bestScore[i][ea]) {
							bestScore[i][ea] = score;
							bestElement[i][ea] = j;
							bestEnd[i][ea] = (b.dot ? -1 : eb);
							bestGap[i][ea] = gap;
							bestCorner[i][ea] = corner;
						}
					}
				}
			}
		}

		// for each dot: its nearest neighbour, and the nearest one on the other side
		var dotNeighbours = new int[n][2];
		var dotGaps = new double[n][2];
		for (var i = 0; i < n; i++) {
			Arrays.fill(dotNeighbours[i], -1);
			var a = elements.get(i);
			if (!a.dot)
				continue;
			var candidates = new ArrayList<double[]>(); // element, distance, x, y
			for (var j = 0; j < n; j++) {
				if (i == j)
					continue;
				var b = elements.get(j);
				if (b.dot) {
					var d = Math.hypot(b.x[0] - a.x[0], b.y[0] - a.y[0]);
					if (d <= dotGap(a, b))
						candidates.add(new double[]{j, d, b.x[0], b.y[0]});
				} else {
					for (var eb = 0; eb < 2; eb++) {
						if (bestElement[j][eb] == i)
							candidates.add(new double[]{j, bestGap[j][eb], b.x[eb], b.y[eb]});
					}
				}
			}
			candidates.sort((p, q) -> Double.compare(p[1], q[1]));
			if (!candidates.isEmpty()) {
				var first = candidates.get(0);
				dotNeighbours[i][0] = (int) first[0];
				dotGaps[i][0] = first[1];
				// the second neighbour: the one most nearly opposite the first, among those about as near. A dotted line may
				// turn a corner at a dot, so opposite need not mean straight on
				var bestCos = 0.2;
				for (var k = 1; k < candidates.size(); k++) {
					var second = candidates.get(k);
					if (second[1] > 2.2 * first[1] + 2) // a dot may be missing on that side
						break;
					var cos = ((first[2] - a.x[0]) * (second[2] - a.x[0]) + (first[3] - a.y[0]) * (second[3] - a.y[0])) / (first[1] * second[1]);
					if (cos < bestCos && (int) second[0] != (int) first[0]) {
						bestCos = cos;
						dotNeighbours[i][1] = (int) second[0];
						dotGaps[i][1] = second[1];
					}
				}
			}
		}

		// keep the links that both sides chose
		for (var i = 0; i < n; i++) {
			var a = elements.get(i);
			if (a.dot) {
				for (var k = 0; k < 2; k++) {
					var j = dotNeighbours[i][k];
					if (j == -1)
						continue;
					var b = elements.get(j);
					var mutual = (b.dot ? (dotNeighbours[j][0] == i || dotNeighbours[j][1] == i) : (bestElement[j][0] == i || bestElement[j][1] == i));
					if (mutual) {
						a.linkElement[k] = j;
						a.linkEnd[k] = (b.dot ? -1 : (bestElement[j][0] == i ? 0 : 1));
						a.linkGap[k] = dotGaps[i][k];
					}
				}
			} else {
				for (var ea = 0; ea < 2; ea++) {
					var j = bestElement[i][ea];
					if (j == -1)
						continue;
					var b = elements.get(j);
					var mutual = (b.dot ? (dotNeighbours[j][0] == i || dotNeighbours[j][1] == i) : (bestElement[j][bestEnd[i][ea]] == i && bestEnd[j][bestEnd[i][ea]] == ea));
					if (mutual) {
						a.linkElement[ea] = j;
						a.linkEnd[ea] = bestEnd[i][ea];
						a.linkGap[ea] = bestGap[i][ea];
						a.corner[ea] = bestCorner[i][ea];
					}
				}
			}
		}
	}

	private static double dotGap(Element a, Element b) {
		return Math.max(MIN_DOT_GAP, GAP_PER_DIAMETER * Math.max(a.diameter, b.diameter));
	}

	/**
	 * the point at which two rays meet, if both reach it within the given distance
	 */
	private static double[] intersect(double ax, double ay, double adx, double ady, double bx, double by, double bdx, double bdy, double maxDistance) {
		var det = adx * (-bdy) - (-bdx) * ady;
		if (Math.abs(det) < 1e-6)
			return null;
		var s = ((bx - ax) * (-bdy) - (-bdx) * (by - ay)) / det;
		var r = (adx * (by - ay) - ady * (bx - ax)) / det;
		if (s <= 0 || r <= 0 || s > maxDistance || r > maxDistance)
			return null;
		return new double[]{ax + s * adx, ay + s * ady};
	}

	/**
	 * walks straight on from an open end of a run, and returns the first ink that belongs to a line: ink that is
	 * neither part of this run nor another small piece on its own. The search widens a little with the distance
	 */
	private static double[] findInk(int[][] matrix, Components components, boolean[] small, int[] lineOfComponent, int run, double x, double y, double dx, double dy, double reach, boolean dashedOnly) {
		var height = matrix.length;
		var width = matrix[0].length;
		for (var s = 2; s <= reach; s++) {
			var cx = x + s * dx;
			var cy = y + s * dy;
			var radius = (int) Math.round(1 + 0.12 * s);
			double[] best = null;
			var bestDistance = Double.MAX_VALUE;
			for (var yy = (int) Math.round(cy) - radius; yy <= Math.round(cy) + radius; yy++) {
				for (var xx = (int) Math.round(cx) - radius; xx <= Math.round(cx) + radius; xx++) {
					if (xx < 0 || xx >= width || yy < 0 || yy >= height || matrix[yy][xx] == 0)
						continue;
					var c = components.get(xx, yy);
					// a gap that was drawn in has no number: it belongs to a dashed line
					if (c != 0 && (lineOfComponent[c] == run || (lineOfComponent[c] == 0 && (small[c] || dashedOnly))))
						continue;
					var distance = Math.hypot(xx - x, yy - y);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = new double[]{xx, yy};
					}
				}
			}
			if (best != null)
				return best;
		}
		return null;
	}

	/**
	 * looks for the end of a line that an open end of a run reaches round a corner: where a dashed line leaves a
	 * node and turns within its first gap, the run ends short of the corner, and what it has to be joined to is
	 * not straight on but to the side. The lines around the open end are thinned, and the end of a line is taken
	 * if it and the open end point at the same place, at about a right angle
	 *
	 * @return the corner and the end of the line, as x, y, x, y, or null
	 */
	private static double[] findEndRoundCorner(int[][] ink, Components components, boolean[] small, int[] lineOfComponent, int run, double x, double y, double dx, double dy, double reach, boolean dashedOnly) {
		var height = ink.length;
		var width = ink[0].length;
		var radius = (int) Math.ceil(reach) + 8;
		var x0 = (int) Math.round(x) - radius;
		var y0 = (int) Math.round(y) - radius;
		var size = 2 * radius + 1;
		var local = new int[size][size];
		var any = false;
		for (var yy = 0; yy < size; yy++) {
			for (var xx = 0; xx < size; xx++) {
				var px = x0 + xx;
				var py = y0 + yy;
				if (px < 0 || px >= width || py < 0 || py >= height || ink[py][px] == 0)
					continue;
				var c = components.get(px, py);
				if (c != 0 && (lineOfComponent[c] == run || (lineOfComponent[c] == 0 && (small[c] || dashedOnly))))
					continue;
				local[yy][xx] = 1;
				any = true;
			}
		}
		if (!any)
			return null;
		Skeletonization.apply(local);

		double[] best = null;
		var bestScore = Double.MAX_VALUE;
		for (var yy = 3; yy < size - 3; yy++) { // a line that leaves the window does not end at its border
			for (var xx = 3; xx < size - 3; xx++) {
				if (local[yy][xx] == 0 || countNeighbours(local, xx, yy) != 1)
					continue;
				// walk back along the line for its direction at the end
				var cx = xx;
				var cy = yy;
				var px = -1;
				var py = -1;
				for (var step = 0; step < TANGENT_LENGTH; step++) {
					var nx = -1;
					var ny = -1;
					var count = 0;
					for (var ddy = -1; ddy <= 1; ddy++) {
						for (var ddx = -1; ddx <= 1; ddx++) {
							if ((ddx != 0 || ddy != 0) && local[cy + ddy][cx + ddx] != 0 && !(cx + ddx == px && cy + ddy == py)) {
								nx = cx + ddx;
								ny = cy + ddy;
								count++;
							}
						}
					}
					if (count != 1 || nx < 1 || ny < 1 || nx >= size - 1 || ny >= size - 1)
						break;
					px = cx;
					py = cy;
					cx = nx;
					cy = ny;
				}
				var tx = xx - cx;
				var ty = yy - cy;
				var norm = Math.hypot(tx, ty);
				if (norm < 2)
					continue;
				tx /= norm;
				ty /= norm;
				if (Math.abs(tx * dx + ty * dy) > 0.5)
					continue;
				var corner = intersect(x, y, dx, dy, x0 + xx, y0 + yy, tx, ty, reach);
				if (corner == null)
					continue;
				var score = Math.hypot(corner[0] - x, corner[1] - y) + Math.hypot(corner[0] - (x0 + xx), corner[1] - (y0 + yy));
				if (score < bestScore) {
					bestScore = score;
					best = new double[]{corner[0], corner[1], x0 + xx, y0 + yy};
				}
			}
		}
		return best;
	}

	private static int countNeighbours(int[][] matrix, int x, int y) {
		var count = 0;
		for (var dy = -1; dy <= 1; dy++) {
			for (var dx = -1; dx <= 1; dx++) {
				if ((dx != 0 || dy != 0) && matrix[y + dy][x + dx] != 0)
					count++;
			}
		}
		return count;
	}

	private static void drawLine(int[][] matrix, double ax, double ay, double bx, double by) {
		var height = matrix.length;
		var width = matrix[0].length;
		var x = (int) Math.round(ax);
		var y = (int) Math.round(ay);
		var x1 = (int) Math.round(bx);
		var y1 = (int) Math.round(by);
		var dx = Math.abs(x1 - x);
		var dy = Math.abs(y1 - y);
		var sx = Integer.compare(x1, x);
		var sy = Integer.compare(y1, y);
		var err = dx - dy;
		while (true) {
			if (x >= 0 && x < width && y >= 0 && y < height)
				matrix[y][x] = 1;
			if (x == x1 && y == y1)
				break;
			var e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
	}
}

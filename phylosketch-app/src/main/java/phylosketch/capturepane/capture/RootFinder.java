/*
 * RootFinder.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import phylosketch.view.RootPosition;

/**
 * finds, from the ink of a picture alone, where the root of the drawing is likely to be and which way the drawing
 * grows, and finds the line that is nearest to a point
 * <p>
 * The root marker of the capture pane starts where this says and takes the line that it is dropped near, because
 * capture needs the root on a line: a few pixels beside it, the root is left without an edge. No text is read, so
 * the answer is there as soon as the picture is.
 * <p>
 * The drawing is taken to be the piece of line ink whose lines are longest in total, other than a frame. Its leaves
 * are the ends of straight lines that nothing is attached to; they point the way the drawing grows, and the root
 * lies between the outermost of them, at the end of the drawing that they point away from. Where a line is dashed
 * the drawing is in several pieces, and only the longest is looked at: joining dashes takes seven times as long as
 * all the rest.
 * Daniel Huson, 11.2026
 */
public class RootFinder {
	// a run of ink along a row or column is part of a straight line if it is at least this long, in pixels and as a multiple of the thickness of the line
	private static final int MIN_RUN = 8;
	private static final int MIN_RUN_PER_THICKNESS = 4;
	// the end of such a run is free if the ink is at most this much thicker there than in the middle of the run
	private static final int FREE_END_EXTRA = 2;
	// the ends say which way the drawing grows if this many more point one way than the opposite way, and that is this many times what is left over across
	private static final double MIN_ENDS = 3;
	private static final double MIN_ENDS_RATIO = 2;
	// otherwise the lines do, if in the outer third at one side this many cross a straight scan on average, and that is this many times the number at any other side
	private static final double MIN_CROSSINGS = 3;
	private static final double MIN_CROSSINGS_RATIO = 1.6;
	// a piece is a frame if this proportion of its ink lies along the sides of its box, and it is not the drawing if the box is larger than this proportion of the picture both ways
	private static final double FRAME_PROPORTION = 0.9;
	private static final double FRAME_SIZE = 0.2;
	// how far, in pixels, the end of the drawing is looked at to find the longest stretch of it
	private static final int END_DEPTH = 3;
	// how far from a point of a line the middle of that line is looked for
	private static final int MIDDLE_REACH = 25;

	private final int width;
	private final int height;
	// for each pixel of ink, its distance to the background, at most 127: positive in a line, negative in a smaller piece
	// of ink, such as a dash or a letter; 0 for the background
	private final byte[][] halfWidth;
	private final Point2D[] guesses = new Point2D[RootPosition.Side.values().length];
	private RootPosition.Side side;

	/**
	 * analyses a picture
	 *
	 * @param image the picture
	 */
	public RootFinder(Image image) {
		var ink = Binarization.apply(image);
		height = ink.length;
		width = (height == 0 ? 0 : ink[0].length);
		halfWidth = new byte[height][width];
		if (width == 0)
			return;

		var components = new Components(ink);
		var count = components.count();
		var maxGlyph = LineInk.maxGlyph(width, height);
		var distance = SpurRemoval.distanceToBackground(ink);
		var sumDistance = new long[count + 1];
		var alongSides = new int[count + 1];
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var c = components.get(x, y);
				if (c != 0) {
					if (components.width(c) > maxGlyph || components.height(c) > maxGlyph) {
						halfWidth[y][x] = (byte) Math.min(127, distance[y][x]);
						sumDistance[c] += distance[y][x];
					} else
						halfWidth[y][x] = (byte) -Math.min(127, distance[y][x]);
				}
			}
		}
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var c = components.get(x, y);
				if (sumDistance[c] > 0) {
					var band = Math.max(4.0 * sumDistance[c] / components.area(c), 0.04 * Math.min(components.width(c), components.height(c)));
					if (x - components.minX(c) < band || components.maxX(c) - x < band || y - components.minY(c) < band || components.maxY(c) - y < band)
						alongSides[c]++;
				}
			}
		}

		// the drawing: the piece with the longest lines. The area of a piece over its mean half width is their length, but for a factor
		var drawing = 0;
		var longest = 0.0;
		for (var c = 1; c <= count; c++) {
			if (sumDistance[c] == 0)
				continue;
			var meanHalfWidth = (double) sumDistance[c] / components.area(c);
			var frame = alongSides[c] >= FRAME_PROPORTION * components.area(c) && Math.min(components.width(c), components.height(c)) > 16 * meanHalfWidth;
			if (frame && components.width(c) > FRAME_SIZE * width && components.height(c) > FRAME_SIZE * height)
				continue;
			var length = components.area(c) / meanHalfWidth;
			if (length > longest) {
				longest = length;
				drawing = c;
			}
		}
		if (drawing == 0)
			return;

		var ends = new Ends(components, drawing);
		side = determineSide(components, drawing, ends);
		for (var s : RootPosition.Side.values()) {
			guesses[s.ordinal()] = determineRoot(components, drawing, ends, s);
		}
	}

	/**
	 * the likely place of the root of a drawing that grows from the given side
	 *
	 * @param side the side that the root is on, or the center
	 * @return the place in the pixels of the picture, on a line, or null if the picture has no lines
	 */
	public Point2D getRoot(RootPosition.Side side) {
		return guesses[side.ordinal()];
	}

	/**
	 * the side that the root is likely to be on
	 *
	 * @return the side, or null if the picture does not say. The center is never returned
	 */
	public RootPosition.Side getSide() {
		return side;
	}

	/**
	 * finds the middle of the line that is nearest to a point. A smaller piece of ink is taken only if there is no line,
	 * and only half as far away: it may be a dash of a dashed line, but it may also be a letter
	 *
	 * @param x      the point, in the pixels of the picture
	 * @param y      the point
	 * @param radius how far to look
	 * @return the nearest point in the middle of a line, or null if there is nothing within the radius
	 */
	public Point2D nearestLine(double x, double y, double radius) {
		var result = nearest(x, y, radius, true);
		return result != null ? result : nearest(x, y, 0.5 * radius, false);
	}

	private Point2D nearest(double x, double y, double radius, boolean line) {
		var px = (int) Math.round(x);
		var py = (int) Math.round(y);
		var reach = (int) Math.ceil(radius);
		var best = Double.MAX_VALUE;
		Point2D result = null;
		for (var yy = Math.max(0, py - reach); yy <= Math.min(height - 1, py + reach); yy++) {
			for (var xx = Math.max(0, px - reach); xx <= Math.min(width - 1, px + reach); xx++) {
				if (line ? halfWidth[yy][xx] > 0 : halfWidth[yy][xx] < 0) {
					var d = (xx - x) * (xx - x) + (yy - y) * (yy - y);
					if (d < best && d <= radius * radius && isMiddle(xx, yy)) {
						best = d;
						result = new Point2D(xx, yy);
					}
				}
			}
		}
		return result;
	}

	/**
	 * is this point on a line, or on any other ink?
	 *
	 * @param x the point, in the pixels of the picture
	 * @param y the point
	 */
	public boolean isOnLine(double x, double y) {
		var px = (int) Math.round(x);
		var py = (int) Math.round(y);
		for (var yy = Math.max(0, py - 2); yy <= Math.min(height - 1, py + 2); yy++) {
			for (var xx = Math.max(0, px - 2); xx <= Math.min(width - 1, px + 2); xx++) {
				if (halfWidth[yy][xx] != 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * is this pixel in the middle of its line, that is, at least as far from the background as each of its neighbours?
	 */
	private boolean isMiddle(int x, int y) {
		var d = Math.abs(halfWidth[y][x]);
		for (var yy = Math.max(0, y - 1); yy <= Math.min(height - 1, y + 1); yy++) {
			for (var xx = Math.max(0, x - 1); xx <= Math.min(width - 1, x + 1); xx++) {
				if (Math.abs(halfWidth[yy][xx]) > d) // a neighbour is ink of the same piece, or background
					return false;
			}
		}
		return true;
	}

	/**
	 * which way the drawing grows: the way the free ends of its lines point, or else the side at which most lines run
	 */
	private static RootPosition.Side determineSide(Components components, int c, Ends ends) {
		var across = ends.count[Ends.RIGHT] - ends.count[Ends.LEFT];
		var down = ends.count[Ends.DOWN] - ends.count[Ends.UP];
		if (Math.abs(across) >= MIN_ENDS && Math.abs(across) >= MIN_ENDS_RATIO * Math.abs(down))
			return across > 0 ? RootPosition.Side.Left : RootPosition.Side.Right;
		if (Math.abs(down) >= MIN_ENDS && Math.abs(down) >= MIN_ENDS_RATIO * Math.abs(across))
			return down > 0 ? RootPosition.Side.Top : RootPosition.Side.Bottom;

		var x0 = components.minX(c);
		var x1 = components.maxX(c);
		var y0 = components.minY(c);
		var y1 = components.maxY(c);
		// the mean number of lines that a straight scan crosses, in the outer third of the drawing at each side
		var crossings = new double[4];
		var third = Math.max(1, components.width(c) / 3);
		for (var i = 0; i < third; i += 2) {
			crossings[Ends.LEFT] += countCrossings(components, c, x0 + i, y0, y1, true) / Math.ceil(third / 2.0);
			crossings[Ends.RIGHT] += countCrossings(components, c, x1 - i, y0, y1, true) / Math.ceil(third / 2.0);
		}
		third = Math.max(1, components.height(c) / 3);
		for (var i = 0; i < third; i += 2) {
			crossings[Ends.UP] += countCrossings(components, c, y0 + i, x0, x1, false) / Math.ceil(third / 2.0);
			crossings[Ends.DOWN] += countCrossings(components, c, y1 - i, x0, x1, false) / Math.ceil(third / 2.0);
		}
		var most = 0;
		for (var i = 1; i < 4; i++) {
			if (crossings[i] > crossings[most])
				most = i;
		}
		for (var i = 0; i < 4; i++) {
			if (i != most && crossings[most] < MIN_CROSSINGS_RATIO * Math.max(0.5, crossings[i]))
				return null;
		}
		if (crossings[most] < MIN_CROSSINGS)
			return null;
		return switch (most) { // the leaves are where most lines are, the root is at the other side
			case Ends.RIGHT -> RootPosition.Side.Left;
			case Ends.LEFT -> RootPosition.Side.Right;
			case Ends.DOWN -> RootPosition.Side.Top;
			default -> RootPosition.Side.Bottom;
		};
	}

	/**
	 * the number of times that a straight scan across a piece enters it
	 */
	private static int countCrossings(Components components, int c, int at, int from, int to, boolean vertical) {
		var count = 0;
		var inside = false;
		for (var i = from; i <= to; i++) {
			var here = (vertical ? components.get(at, i) : components.get(i, at)) == c;
			if (here && !inside)
				count++;
			inside = here;
		}
		return count;
	}

	/**
	 * the root of the drawing, if it grows from the given side: the middle of the longest stretch of the drawing that
	 * reaches that side, between the outermost leaves
	 */
	private Point2D determineRoot(Components components, int c, Ends ends, RootPosition.Side side) {
		var x0 = components.minX(c);
		var x1 = components.maxX(c);
		var y0 = components.minY(c);
		var y1 = components.maxY(c);
		if (side == RootPosition.Side.Center) // the line nearest to the middle of the drawing
			return nearest(0.5 * (x0 + x1), 0.5 * (y0 + y1), Math.max(x1 - x0, y1 - y0), true);

		var vertical = (side == RootPosition.Side.Left || side == RootPosition.Side.Right); // the side is a vertical one
		var near = (side == RootPosition.Side.Left || side == RootPosition.Side.Top); // and it is where the coordinates are small
		// the leaves point away from the root
		var leaves = switch (side) {
			case Left -> Ends.RIGHT;
			case Right -> Ends.LEFT;
			case Top -> Ends.DOWN;
			default -> Ends.UP;
		};
		var from = (vertical ? y0 : x0);
		var to = (vertical ? y1 : x1);
		if (ends.count[leaves] >= 2) {
			from = Math.max(from, ends.first[leaves]);
			to = Math.min(to, ends.last[leaves]);
		}

		// how far the drawing reaches towards the side, between the outermost leaves
		var reach = (near ? Integer.MAX_VALUE : Integer.MIN_VALUE);
		for (var i = from; i <= to; i++) {
			if (near) {
				for (var j = (vertical ? x0 : y0); j < reach && j <= (vertical ? x1 : y1); j++) {
					if ((vertical ? components.get(j, i) : components.get(i, j)) == c) {
						reach = j;
						break;
					}
				}
			} else {
				for (var j = (vertical ? x1 : y1); j > reach && j >= (vertical ? x0 : y0); j--) {
					if ((vertical ? components.get(j, i) : components.get(i, j)) == c) {
						reach = j;
						break;
					}
				}
			}
		}
		if (reach == Integer.MAX_VALUE || reach == Integer.MIN_VALUE)
			return null;

		// the longest stretch of it there
		var bestStart = -1;
		var bestLength = 0;
		var start = -1;
		for (var i = from; i <= to + 1; i++) {
			var here = false;
			if (i <= to) {
				for (var d = 0; d < END_DEPTH && !here; d++) {
					var j = (near ? reach + d : reach - d);
					if (j >= (vertical ? x0 : y0) && j <= (vertical ? x1 : y1))
						here = (vertical ? components.get(j, i) : components.get(i, j)) == c;
				}
			}
			if (here && start < 0)
				start = i;
			else if (!here && start >= 0) {
				if (i - start > bestLength) {
					bestLength = i - start;
					bestStart = start;
				}
				start = -1;
			}
		}
		if (bestStart < 0)
			return null;
		var middle = bestStart + bestLength / 2;
		var x = (vertical ? reach : middle);
		var y = (vertical ? middle : reach);
		var result = nearest(x, y, MIDDLE_REACH, true);
		return result != null ? result : new Point2D(x, y);
	}

	/**
	 * the free ends of the straight lines of a piece, by the way they point
	 * <p>
	 * A run of ink along a row is part of a horizontal line if it is long for its thickness. Its end is free if no
	 * other line is attached there, that is, if the ink is no thicker at the end than in the middle of the run. A line
	 * is seen in as many rows as it is thick, so each row counts for one over the thickness. A line that is not quite
	 * straight has such ends at each step, as many one way as the other, which is why only the difference between
	 * opposite ways is used.
	 */
	private static class Ends {
		static final int LEFT = 0;
		static final int UP = 1;
		static final int RIGHT = 2;
		static final int DOWN = 3;

		final double[] count = new double[4]; // how many ends point each way
		final int[] first = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}; // the first row, or column, that has such an end
		final int[] last = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}; // and the last

		Ends(Components components, int c) {
			for (var pass = 0; pass < 2; pass++) {
				var horizontal = (pass == 0);
				var a0 = (horizontal ? components.minY(c) : components.minX(c));
				var a1 = (horizontal ? components.maxY(c) : components.maxX(c));
				var b0 = (horizontal ? components.minX(c) : components.minY(c));
				var b1 = (horizontal ? components.maxX(c) : components.maxY(c));
				for (var a = a0; a <= a1; a++) {
					var start = -1;
					for (var b = b0; b <= b1 + 1; b++) {
						var here = b <= b1 && (horizontal ? components.get(b, a) : components.get(a, b)) == c;
						if (here && start < 0)
							start = b;
						else if (!here && start >= 0) {
							var end = b - 1;
							var length = end - start + 1;
							if (length >= MIN_RUN) {
								var thickness = across(components, c, (start + end) / 2, a, horizontal, length);
								if (length >= MIN_RUN_PER_THICKNESS * thickness) {
									if (across(components, c, start, a, horizontal, length) <= thickness + FREE_END_EXTRA)
										add(horizontal ? LEFT : UP, a, 1.0 / thickness);
									if (across(components, c, end, a, horizontal, length) <= thickness + FREE_END_EXTRA)
										add(horizontal ? RIGHT : DOWN, a, 1.0 / thickness);
								}
							}
							start = -1;
						}
					}
				}
			}
		}

		private void add(int way, int at, double weight) {
			count[way] += weight;
			first[way] = Math.min(first[way], at);
			last[way] = Math.max(last[way], at);
		}

		/**
		 * the extent of the ink across a run, through one position of it, as far as it is looked at
		 */
		private static int across(Components components, int c, int b, int a, boolean horizontal, int limit) {
			var count = 1;
			for (var step = -1; step <= 1; step += 2) {
				for (var i = 1; i <= limit; i++) {
					var x = (horizontal ? b : a + step * i);
					var y = (horizontal ? a + step * i : b);
					if (x < components.minX(c) || x > components.maxX(c) || y < components.minY(c) || y > components.maxY(c) || components.get(x, y) != c)
						break;
					count++;
				}
			}
			return count;
		}
	}
}

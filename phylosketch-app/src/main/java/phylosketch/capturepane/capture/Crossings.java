/*
 * Crossings.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * lets a dashed line that crosses another line run through
 * <p>
 * Where two lines cross, tracing finds a branching point and four segments that end there, and each of them
 * would become an edge that ends in a node at the crossing. Where a dashed line passes over another line this
 * is known to be a crossing and not a node: the two segments that lie opposite each other are made one segment
 * again, both pairs of them, so that no segment ends there any more. The same is done where two lines of
 * different colour pass straight through each other, as a coloured reticulate edge and a black tree edge do.
 * Four lines of one colour that meet in a point are left as they are: that is also what a node with three
 * children looks like in a rectangular drawing.
 * Daniel Huson, 11.2026
 */
public class Crossings {
	// a segment ends at a crossing if it ends within this distance of it
	private static final int RADIUS = 9;
	// the direction of a segment at its end is taken over this many of its points
	private static final int DIRECTION_POINTS = 12;
	// two segments lie opposite each other if the cosine of the angle between their directions is at most this
	private static final double MAX_COS = -0.7;
	// ends of segments within this distance of each other are one point
	private static final double SAME_POINT = 3;
	// for a crossing by colour both lines have to pass straight through: the cosine of the angle is at most this
	private static final double STRAIGHT_COS = -0.85;
	// the colour of a line is taken over its points from this to this, counted from the crossing
	private static final int COLOUR_FROM = 5;
	private static final int COLOUR_TO = 22;
	// two colours are the same if they differ by at most this, and different if by at least that, as distances in red, green and blue
	// a pixel brighter than this is background
	private static final double LIGHT = 200;
	private static final double SAME_COLOUR = 60;
	private static final double OTHER_COLOUR = 90;

	/**
	 * joins the segments that run through each crossing
	 *
	 * @param segments  the segments, changed in place
	 * @param crossings the places where a dashed line crosses another line
	 * @return the number of crossings at which segments were joined
	 */
	public static int apply(List<Segment> segments, Collection<Point> crossings) {
		var count = 0;
		for (var crossing : crossings) {
			// the ends of segments at the crossing: segment, and 0 for its first point or 1 for its last
			var ends = new ArrayList<int[]>();
			for (var i = 0; i < segments.size(); i++) {
				var segment = segments.get(i);
				if (segment.points().size() < 2)
					continue;
				if (segment.first().distance(crossing) <= RADIUS)
					ends.add(new int[]{i, 0});
				if (segment.last().distance(crossing) <= RADIUS)
					ends.add(new int[]{i, 1});
			}
			if (ends.size() < 4)
				continue; // not traced as a crossing
			ends.sort((a, b) -> Double.compare(end(segments, a).distance(crossing), end(segments, b).distance(crossing)));

			var directions = new double[4][];
			for (var k = 0; k < 4; k++) {
				var points = segments.get(ends.get(k)[0]).points();
				var steps = Math.min(DIRECTION_POINTS, points.size() - 1);
				var from = (ends.get(k)[1] == 0 ? points.get(0) : points.get(points.size() - 1));
				var to = (ends.get(k)[1] == 0 ? points.get(steps) : points.get(points.size() - 1 - steps));
				var norm = Math.max(1e-9, from.distance(to));
				directions[k] = new double[]{(to.x() - from.x()) / norm, (to.y() - from.y()) / norm};
			}

			// the pairing of the four ends in which the pairs lie most nearly opposite each other
			int[][][] pairings = {{{0, 1}, {2, 3}}, {{0, 2}, {1, 3}}, {{0, 3}, {1, 2}}};
			int[][] best = null;
			var bestSum = Double.MAX_VALUE;
			for (var pairing : pairings) {
				var sum = cos(directions, pairing[0]) + cos(directions, pairing[1]);
				if (sum < bestSum) {
					bestSum = sum;
					best = pairing;
				}
			}
			var joined = new ArrayList<Segment>();
			var used = new ArrayList<Integer>();
			for (var pair : best) {
				var a = ends.get(pair[0]);
				var b = ends.get(pair[1]);
				if (cos(directions, pair) > MAX_COS || a[0] == b[0])
					continue;
				// from the far end of the one, through the crossing, to the far end of the other
				var points = new ArrayList<Point>();
				var first = segments.get(a[0]).points();
				for (var i = 0; i < first.size(); i++)
					points.add(first.get(a[1] == 1 ? i : first.size() - 1 - i));
				var second = segments.get(b[0]).points();
				for (var i = 0; i < second.size(); i++)
					points.add(second.get(b[1] == 0 ? i : second.size() - 1 - i));
				joined.add(new Segment(points));
				used.add(a[0]);
				used.add(b[0]);
			}
			if (!joined.isEmpty()) {
				used.sort((a, b) -> Integer.compare(b, a));
				for (int i : used)
					segments.remove(i);
				segments.addAll(joined);
				count++;
			}
		}
		return count;
	}

	/**
	 * joins the segments that run through each point at which two lines of different colour cross
	 *
	 * @param segments the segments, changed in place
	 * @param image    the picture that they were traced from
	 * @return the number of crossings at which segments were joined
	 */
	public static int apply(List<Segment> segments, Image image) {
		var reader = image.getPixelReader();
		if (reader == null)
			return 0;
		// the points at which exactly four segments end
		var points = new ArrayList<Point>();
		var counts = new ArrayList<Integer>();
		for (var segment : segments) {
			if (segment.points().size() < 2)
				continue;
			for (var end : List.of(segment.first(), segment.last())) {
				var found = false;
				for (var i = 0; i < points.size() && !found; i++) {
					if (points.get(i).distance(end) <= SAME_POINT) {
						counts.set(i, counts.get(i) + 1);
						found = true;
					}
				}
				if (!found) {
					points.add(end);
					counts.add(1);
				}
			}
		}
		var crossings = new ArrayList<Point>();
		for (var k = 0; k < points.size(); k++) {
			if (counts.get(k) != 4)
				continue;
			var crossing = points.get(k);
			var ends = new ArrayList<int[]>();
			for (var i = 0; i < segments.size(); i++) {
				var segment = segments.get(i);
				if (segment.points().size() < 2)
					continue;
				if (segment.first().distance(crossing) <= SAME_POINT)
					ends.add(new int[]{i, 0});
				if (segment.last().distance(crossing) <= SAME_POINT)
					ends.add(new int[]{i, 1});
			}
			if (ends.size() != 4)
				continue;
			var directions = new double[4][];
			var colours = new double[4][];
			var ok = true;
			for (var e = 0; e < 4 && ok; e++) {
				var list = segments.get(ends.get(e)[0]).points();
				if (list.size() <= COLOUR_FROM + 2) {
					ok = false;
					break;
				}
				var atFirst = (ends.get(e)[1] == 0);
				var steps = Math.min(DIRECTION_POINTS, list.size() - 1);
				var from = (atFirst ? list.get(0) : list.get(list.size() - 1));
				var to = (atFirst ? list.get(steps) : list.get(list.size() - 1 - steps));
				var norm = Math.max(1e-9, from.distance(to));
				directions[e] = new double[]{(to.x() - from.x()) / norm, (to.y() - from.y()) / norm};
				// the colour of the line, a little away from the crossing
				var sum = new double[3];
				var n = 0;
				for (var i = COLOUR_FROM; i < Math.min(COLOUR_TO, list.size()); i++) {
					var p = (atFirst ? list.get(i) : list.get(list.size() - 1 - i));
					if (p.x() < 0 || p.y() < 0 || p.x() >= image.getWidth() || p.y() >= image.getHeight())
						continue;
					var argb = reader.getArgb(p.x(), p.y());
					var r = (argb >> 16) & 0xFF;
					var g = (argb >> 8) & 0xFF;
					var b = argb & 0xFF;
					if (0.3 * r + 0.59 * g + 0.11 * b > LIGHT)
						continue; // a gap of a dashed line that was drawn in, not the line
					sum[0] += r;
					sum[1] += g;
					sum[2] += b;
					n++;
				}
				if (n == 0)
					ok = false;
				else
					colours[e] = new double[]{sum[0] / n, sum[1] / n, sum[2] / n};
			}
			if (!ok)
				continue;
			int[][][] pairings = {{{0, 1}, {2, 3}}, {{0, 2}, {1, 3}}, {{0, 3}, {1, 2}}};
			for (var pairing : pairings) {
				if (cos(directions, pairing[0]) <= STRAIGHT_COS && cos(directions, pairing[1]) <= STRAIGHT_COS
					&& difference(colours[pairing[0][0]], colours[pairing[0][1]]) <= SAME_COLOUR && difference(colours[pairing[1][0]], colours[pairing[1][1]]) <= SAME_COLOUR
					&& difference(mean(colours[pairing[0][0]], colours[pairing[0][1]]), mean(colours[pairing[1][0]], colours[pairing[1][1]])) >= OTHER_COLOUR) {
					crossings.add(crossing);
					break;
				}
			}
		}
		return apply(segments, crossings);
	}

	/**
	 * makes one segment of two segments that end in the same point, if no other segment ends there: such a point
	 * is not a branching point, it is where a branch that was pruned, or a line that now runs through, used to
	 * end. As two segments they would become two edges and a node between them
	 *
	 * @param segments the segments, changed in place
	 * @return the number of points removed
	 */
	public static int joinThrough(List<Segment> segments) {
		var count = 0;
		var changed = true;
		while (changed) {
			changed = false;
			for (var i = 0; i < segments.size() && !changed; i++) {
				var a = segments.get(i);
				if (a.points().size() < 2)
					continue;
				for (var endA = 0; endA < 2 && !changed; endA++) {
					var point = (endA == 0 ? a.first() : a.last());
					var other = -1;
					var otherEnd = -1;
					var ends = 0;
					for (var j = 0; j < segments.size(); j++) {
						var b = segments.get(j);
						if (b.points().size() < 2)
							continue;
						if (b.first().distance(point) <= SAME_POINT) {
							ends++;
							if (j != i) {
								other = j;
								otherEnd = 0;
							}
						}
						if (b.last().distance(point) <= SAME_POINT) {
							ends++;
							if (j != i) {
								other = j;
								otherEnd = 1;
							}
						}
					}
					if (ends == 2 && other != -1) {
						var b = segments.get(other);
						var points = new ArrayList<Point>();
						for (var k = 0; k < a.points().size(); k++)
							points.add(a.points().get(endA == 1 ? k : a.points().size() - 1 - k));
						for (var k = 0; k < b.points().size(); k++)
							points.add(b.points().get(otherEnd == 0 ? k : b.points().size() - 1 - k));
						segments.remove(Math.max(i, other));
						segments.remove(Math.min(i, other));
						segments.add(new Segment(points));
						count++;
						changed = true;
					}
				}
			}
		}
		return count;
	}

	private static double difference(double[] a, double[] b) {
		return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
	}

	private static double[] mean(double[] a, double[] b) {
		return new double[]{0.5 * (a[0] + b[0]), 0.5 * (a[1] + b[1]), 0.5 * (a[2] + b[2])};
	}

	private static Point end(List<Segment> segments, int[] end) {
		return (end[1] == 0 ? segments.get(end[0]).first() : segments.get(end[0]).last());
	}

	private static double cos(double[][] directions, int[] pair) {
		return directions[pair[0]][0] * directions[pair[1]][0] + directions[pair[0]][1] * directions[pair[1]][1];
	}
}

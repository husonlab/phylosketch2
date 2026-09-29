/*
 * NodeGrid.java Copyright (C) 2026 Daniel H. Huson
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

package phylosketch.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import jloda.graph.Graph;
import jloda.graph.Node;

import java.util.*;
import java.util.function.Function;

/**
 * the grid that the nodes snap to, so that a phylogeny can be pulled into a rectilinear drawing by hand
 * <p>
 * The grid runs through the origin of the drawing. Its spacing is half the median distance between the two nodes of
 * an edge, so that two nodes joined by such an edge are two grid steps apart once aligned. If the nodes already lie
 * on a common grid, because they were snapped before the file was saved, say, that grid is kept, so that switching
 * the grid on again does not disturb them. Zooming scales the view, not the node coordinates, so the spacing does
 * not change with it.
 * <p>
 * Ported from splitstree6.view.network.NetworkGrid, where a layout gives each edge a length; here every edge counts
 * as one unit of length.
 * <p>
 * Daniel Huson, 9.2026
 */
public class NodeGrid {
	/**
	 * a coordinate this close to a multiple of the spacing counts as on the grid. Saved node positions are rounded to
	 * one decimal, so they are off by up to 0.05
	 */
	private static final double TOLERANCE = 0.1;

	private final BooleanProperty snap = new SimpleBooleanProperty(this, "snap", false);
	private double spacing;

	public boolean isSnap() {
		return snap.get();
	}

	public BooleanProperty snapProperty() {
		return snap;
	}

	public void setSnap(boolean snap) {
		this.snap.set(snap);
	}

	public double getSpacing() {
		return spacing;
	}

	public void setSpacing(double spacing) {
		this.spacing = spacing;
	}

	/**
	 * the grid point nearest to the given point
	 */
	public Point2D snap(Point2D point) {
		return snap(point, spacing);
	}

	/**
	 * the point of the grid of the given spacing that is nearest to the given point
	 */
	public static Point2D snap(Point2D point, double spacing) {
		if (spacing <= 0)
			return point;
		return new Point2D(spacing * Math.round(point.getX() / spacing), spacing * Math.round(point.getY() / spacing));
	}

	/**
	 * determines the grid spacing for a drawing: half the median distance between the two nodes of an edge, unless
	 * the nodes already lie on a common grid. Then it is the step of that grid, or, as nodes snapped earlier often
	 * use only every second grid line, the whole fraction of that step that comes nearest to the half distance
	 *
	 * @param graph    the phylogeny
	 * @param position the drawn position of a node, or null
	 * @return the spacing, positive
	 */
	public static double computeSpacing(Graph graph, Function<Node, Point2D> position) {
		var points = new ArrayList<Point2D>();
		for (var v : graph.nodes()) {
			if (position.apply(v) != null)
				points.add(position.apply(v));
		}
		var spacing = 0.5 * computeUnitLength(graph, position, points);
		var existing = detectSpacing(points);
		if (existing >= 0.25 * spacing)
			return existing / Math.max(1, Math.round(existing / spacing));
		else
			return spacing;
	}

	/**
	 * the median distance between the two nodes of an edge
	 */
	private static double computeUnitLength(Graph graph, Function<Node, Point2D> position, List<Point2D> points) {
		var lengths = new ArrayList<Double>();
		for (var e : graph.edges()) {
			var p = position.apply(e.getSource());
			var q = position.apply(e.getTarget());
			if (p != null && q != null && p.distance(q) > 0)
				lengths.add(p.distance(q));
		}
		if (!lengths.isEmpty()) {
			Collections.sort(lengths);
			return lengths.get(lengths.size() / 2);
		}
		// no edges, as for a set of points: a twentieth of the extent of the drawing
		var extent = 0.0;
		if (!points.isEmpty()) {
			var minX = points.stream().mapToDouble(Point2D::getX).min().orElse(0);
			var maxX = points.stream().mapToDouble(Point2D::getX).max().orElse(0);
			var minY = points.stream().mapToDouble(Point2D::getY).min().orElse(0);
			var maxY = points.stream().mapToDouble(Point2D::getY).max().orElse(0);
			extent = Math.max(maxX - minX, maxY - minY);
		}
		return extent > 0 ? extent / 20 : 10;
	}

	/**
	 * the spacing of a grid through the origin that all points lie on, or 0 if there is none
	 * <p>
	 * The smallest gap between two distinct coordinates is one grid step, or a whole number of them. That estimate
	 * is refined by least squares over all coordinates, and then every coordinate must be a multiple of the result.
	 */
	public static double detectSpacing(Collection<Point2D> points) {
		if (points.size() < 3)
			return 0;
		var values = new TreeSet<Double>();
		values.add(0.0);
		for (var p : points) {
			values.add(p.getX());
			values.add(p.getY());
		}
		var gap = Double.MAX_VALUE;
		Double previous = null;
		for (var value : values) {
			if (previous != null && value - previous > TOLERANCE)
				gap = Math.min(gap, value - previous);
			previous = value;
		}
		// a step not well above the tolerance would make any coordinates look like multiples of it
		if (gap < 10 * TOLERANCE)
			return 0;

		var sumKV = 0.0;
		var sumKK = 0.0;
		for (var value : values) {
			var k = (double) Math.round(value / gap);
			sumKV += k * value;
			sumKK += k * k;
		}
		if (sumKK == 0)
			return 0;
		var spacing = sumKV / sumKK;
		for (var value : values) {
			if (Math.abs(value - spacing * Math.round(value / spacing)) > TOLERANCE)
				return 0;
		}
		return spacing;
	}

	/**
	 * snaps all points to the grid, no two onto the same grid point. The points nearest to a grid point are placed
	 * first, and a point whose grid point is already taken goes to the nearest free one
	 *
	 * @param points  the points, in an order that breaks ties
	 * @param spacing the grid spacing, positive
	 * @return the snapped points
	 */
	public static <T> Map<T, Point2D> snapAll(Map<T, Point2D> points, double spacing) {
		record Cell(long i, long j) {
		}

		var order = new ArrayList<>(points.keySet());
		order.sort(Comparator.comparingDouble(key -> points.get(key).distance(snap(points.get(key), spacing)))); // stable

		var taken = new HashSet<Cell>();
		var result = new HashMap<T, Point2D>();
		for (var key : order) {
			var point = points.get(key);
			var i0 = Math.round(point.getX() / spacing);
			var j0 = Math.round(point.getY() / spacing);
			Cell best = null;
			var bestDistance = Double.MAX_VALUE;
			// search rings of cells around the nearest one. A cell in ring r is at least r - 1/2 grid steps from the
			// point, so once that exceeds the best distance found, no further ring can hold a nearer free cell
			for (var r = 0; best == null || (r - 0.5) * spacing < bestDistance; r++) {
				for (var i = i0 - r; i <= i0 + r; i++) {
					for (var j = j0 - r; j <= j0 + r; j++) {
						if (Math.max(Math.abs(i - i0), Math.abs(j - j0)) == r) {
							var cell = new Cell(i, j);
							var distance = point.distance(i * spacing, j * spacing);
							if (!taken.contains(cell) && distance < bestDistance) {
								best = cell;
								bestDistance = distance;
							}
						}
					}
				}
			}
			taken.add(best);
			result.put(key, new Point2D(best.i() * spacing, best.j() * spacing));
		}
		return result;
	}
}

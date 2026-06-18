/*
 * Segment.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.geometry.Rectangle2D;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathNormalize;

import java.util.ArrayList;
import java.util.List;

/**
 * line segment with integer points
 * Daniel Huson 1.2025
 *
 * @param points
 */
public record Segment(ArrayList<Point> points) {
	public Segment() {
		this(new ArrayList<>());
	}

	public boolean same(Segment that, double epsilon) {
		return this.contains(that, epsilon) && that.contains(this, epsilon);
	}

	public boolean contains(Segment that, double epsilon) {
		for (var p : that.points) {
			if (this.points.stream().noneMatch(q -> p.distance(q) <= epsilon))
				return false;
		}
		return true;
	}

	public boolean proximal(Segment that, double maxDistance) {
		for (var p : this.points) {
			if (that.points.stream().anyMatch(q -> p.distance(q) <= maxDistance))
				return true;
		}
		return false;
	}

	public boolean proximal(Point2D point, double maxDistance) {
		return points.stream().anyMatch(p -> point.distance(p.point2D()) <= maxDistance);
	}

	public List<Point2D> point2Ds() {
		return new ArrayList<>(points.stream().map(Point::point2D).toList());
	}

	public Segment reverse() {
		return new Segment(CollectionUtils.reverse(points));
	}

	public Point first() {
		return points.get(0);
	}

	public Point last() {
		return points.get(points.size() - 1);
	}

	public List<Segment> split(Point2D point) {
		var best = 0;
		var bestDistance = Double.MAX_VALUE;
		for (var i = 0; i < points.size(); i++) {
			var distance = point.distance(points.get(i).point2D());
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}
		if (best > 0 && best < points.size() - 1) {
			var part1 = CollectionUtils.reverse(new ArrayList<>(points.subList(0, best + 1)));
			var part2 = new ArrayList<>(points.subList(best, points.size()));
			return List.of(new Segment(part1), new Segment(part2));
		} else
			return List.of(this);
	}

	public Segment refine(int gap) {
		var points2D = PathNormalize.refine(point2Ds(), gap);
		var points = new ArrayList<Point>();
		for (var p : points2D) {
			points.add(new Point((int) Math.round(p.getX()), (int) Math.round(p.getY())));
		}
		return new Segment(points);
	}

	public Rectangle2D computeBoundingBox() {
		var xmin = Integer.MAX_VALUE;
		var xmax = Integer.MIN_VALUE;
		var ymin = Integer.MAX_VALUE;
		var ymax = Integer.MIN_VALUE;
		for (var p : points) {
			xmin = Math.min(xmin, p.x());
			xmax = Math.max(xmax, p.x());
			ymin = Math.min(ymin, p.y());
			ymax = Math.max(ymax, p.y());
		}
		return new Rectangle2D(xmin, ymin, xmax - xmin, ymax - ymin);
	}
}

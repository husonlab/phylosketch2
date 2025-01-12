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

package phylocap.capture;

import java.util.ArrayList;

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

	public boolean similar(Segment that, double epsilon) {
		// Check if each point in set1 has a isCloseTo point in set2
		for (var point1 : points()) {
			boolean foundClose = false;
			for (var point2 : that.points()) {
				if (point1.distance(point2) <= epsilon) {
					foundClose = true;
					break;
				}
			}
			if (!foundClose) {
				return false; // No isCloseTo point found in set2 for point1
			}
		}

		// Check if each point in set2 has a isCloseTo point in set1
		for (var point2 : that.points()) {
			boolean foundClose = false;
			for (var point1 : points()) {
				if (point2.distance(point1) <= epsilon) {
					foundClose = true;
					break;
				}
			}
			if (!foundClose) {
				return false; // No isCloseTo point found in set1 for point2
			}
		}

		// If all points in both sets have isCloseTo counterparts, return true
		return true;
	}

	public boolean isCloseTo(Segment that, double maxDistance) {
		for (var p : this.points) {
			for (var q : that.points) {
				if (p.distance(q) <= maxDistance)
					return true;
			}
		}
		return false;
	}
}

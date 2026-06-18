/*
 * GraphRelaxation.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.utils;

import javafx.geometry.Point2D;
import jloda.graph.Node;

import java.util.List;
import java.util.Map;

public class GraphRelaxation {
	public static double stepSize = 0.1;    // Step size for adjustments
	public static double damping = 0.85;    // Damping factor to prevent oscillations

	public static void apply(List<Node> component, Map<Node, Point2D> points, int iterations) {
		var averageDistance = 0.0;
		var count = 0;
		for (var v : component) {
			for (var e : v.outEdges()) {
				averageDistance += points.get(v).distance(points.get(e.getTarget()));
				count++;
			}
		}
		averageDistance /= count;
		apply(component, points, averageDistance, iterations);
	}

	public static void apply(List<Node> component, Map<Node, Point2D> points, double idealDistance, int iterations) {
		for (var i = 0; i < iterations; i++) {
			for (var v : component) {
				for (var e : v.outEdges()) {
					var w = e.getTarget();

					var pv = points.get(v);
					var pw = points.get(w);

					if (pv == null || pw == null) continue; // Skip if missing positions

					var currentDistance = pv.distance(pw);
					var delta = (currentDistance - idealDistance) * stepSize;

					if (Math.abs(delta) < 1e-3) continue; // Ignore very small adjustments

					var direction = pw.subtract(pv).normalize();
					var adjustment = direction.multiply(delta);

					// Apply adjustments with damping
					points.put(v, pv.add(adjustment.multiply(damping)));
					points.put(w, pw.subtract(adjustment.multiply(damping)));
				}
			}
		}
	}
}

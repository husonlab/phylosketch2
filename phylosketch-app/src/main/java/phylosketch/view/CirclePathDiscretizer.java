/*
 * CirclePathDiscretizer.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.shape.*;

public final class CirclePathDiscretizer {
	/**
	 * Discretize a Path that consists of MoveTo, LineTo, ArcTo into
	 * a new Path with one MoveTo followed by many LineTo segments,
	 * with points spaced roughly 'delta' pixels apart along the path.
	 * <p>
	 * The result has no ArcTo/ClosePath, just MoveTo + LineTo.
	 */
	public static Path discretize(Path src, double delta) {
		Path dst = new Path();

		// (Optional) copy some stroke properties:
		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(null);

		double cx = 0, cy = 0;   // current point
		boolean hasCurrent = false;

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				cx = m.getX();
				cy = m.getY();
				hasCurrent = true;

				dst.getElements().add(new MoveTo(cx, cy));

			} else if (e instanceof LineTo l) {
				if (!hasCurrent) continue;
				double x1 = l.getX();
				double y1 = l.getY();

				addDiscretizedLine(dst, cx, cy, x1, y1, delta);
				cx = x1;
				cy = y1;

			} else if (e instanceof ArcTo a) {
				if (!hasCurrent) continue;
				addDiscretizedArc(dst, cx, cy, a, delta);
				cx = a.getX();
				cy = a.getY();
			}
			// (If you ever add ClosePath etc., you can handle them here;
			// for this question we ignore them.)
		}

		return dst;
	}

	// --- helpers -----------------------------------------------------------

	private static void addDiscretizedLine(Path dst,
										   double x0, double y0,
										   double x1, double y1,
										   double delta) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double len = Math.hypot(dx, dy);
		if (len == 0) return;

		int segments = Math.max(1, (int) Math.ceil(len / delta));
		for (int i = 1; i <= segments; i++) {
			double t = (double) i / segments;
			double x = x0 + dx * t;
			double y = y0 + dy * t;
			dst.getElements().add(new LineTo(x, y));
		}
	}

	private static void addDiscretizedArc(Path dst,
										  double x0, double y0,
										  ArcTo arc,
										  double delta) {

		double x1 = arc.getX();
		double y1 = arc.getY();

		double rx = Math.abs(arc.getRadiusX());
		double ry = Math.abs(arc.getRadiusY());

		// Degenerate → treat as straight line
		if (rx == 0 || ry == 0 || (x0 == x1 && y0 == y1)) {
			addDiscretizedLine(dst, x0, y0, x1, y1, delta);
			return;
		}

		double phi = Math.toRadians(arc.getXAxisRotation());
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);

		// Step 1: compute (x1', y1') in the rotated/translated coord system
		double dx2 = (x0 - x1) / 2.0;
		double dy2 = (y0 - y1) / 2.0;

		double x1p = cosPhi * dx2 + sinPhi * dy2;
		double y1p = -sinPhi * dx2 + cosPhi * dy2;

		double rx2 = rx * rx;
		double ry2 = ry * ry;
		double x1p2 = x1p * x1p;
		double y1p2 = y1p * y1p;

		// Step 1.5: ensure radii are large enough
		double lambda = x1p2 / rx2 + y1p2 / ry2;
		if (lambda > 1.0) {
			double scale = Math.sqrt(lambda);
			rx *= scale;
			ry *= scale;
			rx2 = rx * rx;
			ry2 = ry * ry;
		}

		// Step 2: compute center in the prime coordinate system
		double num = rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2;
		double den = rx2 * y1p2 + ry2 * x1p2;
		if (den == 0 || num < 0) {
			// Fallback: treat as line if something is off
			addDiscretizedLine(dst, x0, y0, x1, y1, delta);
			return;
		}

		double factor = Math.sqrt(num / den);
		// choose sign based on flags
		double sign = (arc.isLargeArcFlag() == arc.isSweepFlag()) ? -1.0 : 1.0;
		factor *= sign;

		double cxp = factor * (rx * y1p / ry);
		double cyp = factor * (-ry * x1p / rx);

		// Step 3: center in original coordinates
		double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x1) / 2.0;
		double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y1) / 2.0;

		// Step 4: compute start/end angles
		double ux = (x1p - cxp) / rx;
		double uy = (y1p - cyp) / ry;
		double vx = (-x1p - cxp) / rx;
		double vy = (-y1p - cyp) / ry;

		double theta1 = Math.atan2(uy, ux);
		double dtheta = angleBetween(ux, uy, vx, vy);

		if (!arc.isSweepFlag() && dtheta > 0) {
			dtheta -= 2 * Math.PI;
		} else if (arc.isSweepFlag() && dtheta < 0) {
			dtheta += 2 * Math.PI;
		}

		// Approximate arc length and choose number of segments
		double rEq = Math.sqrt((rx2 + ry2) / 2.0);  // equivalent radius
		double arcLen = Math.abs(dtheta) * rEq;
		int segments = Math.max(1, (int) Math.ceil(arcLen / delta));

		// Step 5: sample along the arc
		for (int i = 1; i <= segments; i++) {
			double t = (double) i / segments;
			double angle = theta1 + dtheta * t;

			double cosA = Math.cos(angle);
			double sinA = Math.sin(angle);

			double xep = rx * cosA;
			double yep = ry * sinA;

			double xt = cosPhi * xep - sinPhi * yep + cx;
			double yt = sinPhi * xep + cosPhi * yep + cy;

			dst.getElements().add(new LineTo(xt, yt));
		}
	}

	private static double angleBetween(double ux, double uy,
									   double vx, double vy) {
		double dot = ux * vx + uy * vy;
		double det = ux * vy - uy * vx;
		return Math.atan2(det, dot);
	}
}

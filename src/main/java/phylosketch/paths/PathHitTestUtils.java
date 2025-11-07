/*
 * PathHitTestUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.paths;

import javafx.scene.shape.*;

import java.awt.geom.Point2D;

/**
 * call this when you need to know that the stroke of a path has be hit
 * Daniel Huson, 11.2025
 */
public final class PathHitTestUtils {
	public static final double DEFAULT_TOLERANCE = 3;

	/**
	 * does local point line on stroke of path, using default tolerance
	 *
	 * @param path   path
	 * @param localX local x
	 * @param localY local y
	 * @return true, if on stroke
	 */
	public static boolean isPointOnStroke(Path path, double localX, double localY) {
		return isPointOnStroke(path, localX, localY, DEFAULT_TOLERANCE);
	}

	/**
	 * Returns true if the point (localX, localY) lies within 'delta' of the
	 * stroked centerline of the given Path.
	 * <p>
	 * Supports: MoveTo, LineTo, HLineTo, VLineTo, QuadCurveTo, ArcTo, ClosePath.
	 * <p>
	 * localX/localY must be in the Path's local coordinate system.
	 */
	public static boolean isPointOnStroke(Path path,
										  double localX,
										  double localY,
										  double delta) {
		if (path.getStroke() == null || path.getStrokeWidth() <= 0) {
			return false;
		}

		// Effective tolerance: half stroke width + extra picking tolerance.
		double tol = path.getStrokeWidth() / 2.0 + delta;

		double cx = 0, cy = 0;   // current point
		double sx = 0, sy = 0;   // start point of current subpath (for ClosePath)
		boolean hasCurrent = false;
		boolean hasStart = false;

		for (PathElement e : path.getElements()) {
			if (e instanceof MoveTo m) {
				cx = m.getX();
				cy = m.getY();
				sx = cx;
				sy = cy;
				hasCurrent = true;
				hasStart = true;

			} else if (e instanceof LineTo l) {
				if (hasCurrent) {
					double x2 = l.getX();
					double y2 = l.getY();
					if (pointNearSegment(localX, localY, cx, cy, x2, y2, tol)) {
						return true;
					}
					cx = x2;
					cy = y2;
				}

			} else if (e instanceof HLineTo h) {
				if (hasCurrent) {
					double x2 = h.getX();
					double y2 = cy;
					if (pointNearSegment(localX, localY, cx, cy, x2, y2, tol)) {
						return true;
					}
					cx = x2;
					cy = y2;
				}

			} else if (e instanceof VLineTo v) {
				if (hasCurrent) {
					double x2 = cx;
					double y2 = v.getY();
					if (pointNearSegment(localX, localY, cx, cy, x2, y2, tol)) {
						return true;
					}
					cx = x2;
					cy = y2;
				}

			} else if (e instanceof QuadCurveTo q) {
				if (hasCurrent) {
					double x0 = cx;
					double y0 = cy;
					double cpx = q.getControlX();
					double cpy = q.getControlY();
					double x1 = q.getX();
					double y1 = q.getY();

					if (pointNearQuadCurve(localX, localY, x0, y0, cpx, cpy, x1, y1, tol)) {
						return true;
					}
					cx = x1;
					cy = y1;
				}

			} else if (e instanceof ArcTo a) {
				if (hasCurrent) {
					double x0 = cx;
					double y0 = cy;
					if (pointNearArc(localX, localY, x0, y0, a, tol)) {
						return true;
					}
					cx = a.getX();
					cy = a.getY();
				}

			} else if (e instanceof ClosePath) {
				if (hasCurrent && hasStart) {
					if (pointNearSegment(localX, localY, cx, cy, sx, sy, tol)) {
						return true;
					}
					cx = sx;
					cy = sy;
				}
			}
		}

		return false;
	}

	/**
	 * Distance from point P(px,py) to segment AB(x1,y1)-(x2,y2) <= tolerance?
	 */
	private static boolean pointNearSegment(double px, double py,
											double x1, double y1,
											double x2, double y2,
											double tolerance) {

		double dx = x2 - x1;
		double dy = y2 - y1;

		// Degenerate segment (both endpoints same)
		if (dx == 0 && dy == 0) {
			return Point2D.distance(px, py, x1, y1) <= tolerance;
		}

		double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
		if (t < 0) t = 0;
		else if (t > 1) t = 1;

		double projX = x1 + t * dx;
		double projY = y1 + t * dy;

		double dist = Point2D.distance(px, py, projX, projY);
		return dist <= tolerance;
	}

	/**
	 * Approximate hit test against a quadratic Bézier curve by subdividing into
	 * line segments and reusing pointNearSegment.
	 */
	private static boolean pointNearQuadCurve(double px, double py,
											  double x0, double y0,
											  double cpx, double cpy,
											  double x1, double y1,
											  double tolerance) {

		double lenApprox =
				Point2D.distance(x0, y0, cpx, cpy) +
				Point2D.distance(cpx, cpy, x1, y1);

		int segments = (int) Math.max(8, Math.min(64, Math.round(lenApprox / 10.0)));

		double prevX = x0;
		double prevY = y0;

		for (int i = 1; i <= segments; i++) {
			double t = (double) i / segments;
			double xt = quadPoint(x0, cpx, x1, t);
			double yt = quadPoint(y0, cpy, y1, t);

			if (pointNearSegment(px, py, prevX, prevY, xt, yt, tolerance)) {
				return true;
			}

			prevX = xt;
			prevY = yt;
		}
		return false;
	}

	/**
	 * 1D quadratic Bézier: B(t) = (1-t)^2 * p0 + 2(1-t)t * p1 + t^2 * p2
	 */
	private static double quadPoint(double p0, double p1, double p2, double t) {
		double u = 1 - t;
		return u * u * p0 + 2 * u * t * p1 + t * t * p2;
	}

	/**
	 * Approximate hit test against an ArcTo elliptical arc segment by
	 * subdividing the arc into line segments.
	 */
	private static boolean pointNearArc(double px, double py,
										double x0, double y0,
										ArcTo arc,
										double tolerance) {

		double x1 = arc.getX();
		double y1 = arc.getY();
		double rx = Math.abs(arc.getRadiusX());
		double ry = Math.abs(arc.getRadiusY());

		// Degenerate → treat as line
		if (rx == 0 || ry == 0 || (x0 == x1 && y0 == y1)) {
			return pointNearSegment(px, py, x0, y0, x1, y1, tolerance);
		}

		// Convert to SVG arc parameters and use standard algorithm.
		double phi = Math.toRadians(arc.getXAxisRotation());
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);

		// Step 1: transform to "prime" coordinates
		double dx2 = (x0 - x1) / 2.0;
		double dy2 = (y0 - y1) / 2.0;

		double x1p = cosPhi * dx2 + sinPhi * dy2;
		double y1p = -sinPhi * dx2 + cosPhi * dy2;

		double rx2 = rx * rx;
		double ry2 = ry * ry;
		double x1p2 = x1p * x1p;
		double y1p2 = y1p * y1p;

		// Ensure radii are large enough
		double lambda = x1p2 / rx2 + y1p2 / ry2;
		if (lambda > 1.0) {
			double scale = Math.sqrt(lambda);
			rx *= scale;
			ry *= scale;
			rx2 = rx * rx;
			ry2 = ry * ry;
		}

		// Step 2: compute center in prime coords
		double num = rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2;
		double den = rx2 * y1p2 + ry2 * x1p2;
		double factor = 0.0;
		if (den != 0) {
			factor = Math.sqrt(Math.max(0.0, num / den));
		}

		double sign = (arc.isLargeArcFlag() == arc.isSweepFlag()) ? -1.0 : 1.0;
		factor *= sign;

		double cxp = factor * (rx * y1p / ry);
		double cyp = factor * (-ry * x1p / rx);

		// Step 3: center in original coords
		double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x1) / 2.0;
		double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y1) / 2.0;

		// Step 4: compute start and end angles
		double sxp = (x1p - cxp) / rx;
		double syp = (y1p - cyp) / ry;

		// end point prime (symmetric)
		double x2p = -x1p;
		double y2p = -y1p;
		double exp = (x2p - cxp) / rx;
		double eyp = (y2p - cyp) / ry;

		double theta1 = Math.atan2(syp, sxp);
		double theta2 = Math.atan2(eyp, exp);

		double dtheta = theta2 - theta1;
		if (!arc.isSweepFlag() && dtheta > 0) {
			dtheta -= 2 * Math.PI;
		} else if (arc.isSweepFlag() && dtheta < 0) {
			dtheta += 2 * Math.PI;
		}

		// Approximate arc length and choose number of segments
		double rEq = Math.sqrt((rx2 + ry2) / 2.0); // equivalent radius
		double arcLenApprox = Math.abs(dtheta) * rEq;
		int segments = (int) Math.max(8, Math.min(64, Math.round(arcLenApprox / 10.0)));

		double prevX = x0;
		double prevY = y0;

		for (int i = 1; i <= segments; i++) {
			double t = (double) i / segments;
			double angle = theta1 + dtheta * t;

			double xep = rx * Math.cos(angle);
			double yep = ry * Math.sin(angle);

			double xt = cosPhi * xep - sinPhi * yep + cx;
			double yt = sinPhi * xep + cosPhi * yep + cy;

			if (pointNearSegment(px, py, prevX, prevY, xt, yt, tolerance)) {
				return true;
			}

			prevX = xt;
			prevY = yt;
		}

		return false;
	}
}
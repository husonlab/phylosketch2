/*
 * PathTransforms.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.shape.*;

public final class PathTransforms {

	private PathTransforms() {
	}

	/**
	 * Return a new Path that is the original 'src' rotated around (cx, cy)
	 * by 'angleDeg' degrees. Supports MoveTo, LineTo, QuadCurveTo, ArcTo.
	 */
	public static Path rotate(Path src, double cx, double cy, double angleDeg) {
		Path dst = new Path();
		// copy basic visual properties if you like
		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(src.getFill());

		double rad = Math.toRadians(angleDeg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				Point2D p = rotatePoint(m.getX(), m.getY(), cx, cy, cos, sin);
				dst.getElements().add(new MoveTo(p.getX(), p.getY()));

			} else if (e instanceof LineTo l) {
				Point2D p = rotatePoint(l.getX(), l.getY(), cx, cy, cos, sin);
				dst.getElements().add(new LineTo(p.getX(), p.getY()));

			} else if (e instanceof QuadCurveTo q) {
				Point2D c = rotatePoint(q.getControlX(), q.getControlY(), cx, cy, cos, sin);
				Point2D p = rotatePoint(q.getX(), q.getY(), cx, cy, cos, sin);
				dst.getElements().add(new QuadCurveTo(
						c.getX(), c.getY(),
						p.getX(), p.getY()
				));

			} else if (e instanceof ArcTo a) {
				Point2D p = rotatePoint(a.getX(), a.getY(), cx, cy, cos, sin);
				ArcTo arc2 = new ArcTo();
				arc2.setRadiusX(a.getRadiusX());
				arc2.setRadiusY(a.getRadiusY());
				// rotate the ellipse orientation as well
				arc2.setXAxisRotation(a.getXAxisRotation() + angleDeg);
				arc2.setLargeArcFlag(a.isLargeArcFlag());
				arc2.setSweepFlag(a.isSweepFlag());
				arc2.setX(p.getX());
				arc2.setY(p.getY());
				dst.getElements().add(arc2);

			} else if (e instanceof ClosePath) {
				dst.getElements().add(new ClosePath());
			} else {
				throw new IllegalArgumentException("Unsupported PathElement type: " + e.getClass());
			}
		}
		return dst;
	}

	/**
	 * Flip a path horizontally across the vertical line x = cx.
	 * (i.e. mirror left/right around cx)
	 */
	public static Path flipHorizontal(Path src, double cx) {
		Path dst = new Path();

		// copy basic visual properties if you like
		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(src.getFill());

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				double x = mirrorX(m.getX(), cx);
				double y = m.getY();
				dst.getElements().add(new MoveTo(x, y));

			} else if (e instanceof LineTo l) {
				double x = mirrorX(l.getX(), cx);
				double y = l.getY();
				dst.getElements().add(new LineTo(x, y));

			} else if (e instanceof QuadCurveTo q) {
				double cpx = mirrorX(q.getControlX(), cx);
				double cpy = q.getControlY();
				double x = mirrorX(q.getX(), cx);
				double y = q.getY();
				dst.getElements().add(new QuadCurveTo(cpx, cpy, x, y));

			} else if (e instanceof ArcTo a) {
				ArcTo arc2 = new ArcTo();
				arc2.setRadiusX(a.getRadiusX());
				arc2.setRadiusY(a.getRadiusY());

				// horizontal reflection: x-axis rotation θ -> 180° - θ
				double theta = a.getXAxisRotation();
				double newTheta = 180.0 - theta;
				arc2.setXAxisRotation(newTheta);

				// large-arc flag unchanged; sweep flag flips (orientation reversal)
				arc2.setLargeArcFlag(a.isLargeArcFlag());
				arc2.setSweepFlag(!a.isSweepFlag());

				double x = mirrorX(a.getX(), cx);
				double y = a.getY();
				arc2.setX(x);
				arc2.setY(y);

				dst.getElements().add(arc2);

			} else if (e instanceof ClosePath) {
				dst.getElements().add(new ClosePath());
			} else {
				throw new IllegalArgumentException("Unsupported PathElement type: " + e.getClass());
			}
		}

		return dst;
	}

	/**
	 * Return a new Path translated by (dx, dy).
	 * Works for MoveTo, LineTo, QuadCurveTo, ArcTo, and ClosePath.
	 */
	public static Path translate(Path src, double dx, double dy) {
		Path dst = new Path();

		// (optional) copy styling
		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(src.getFill());

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				dst.getElements().add(new MoveTo(
						m.getX() + dx, m.getY() + dy));

			} else if (e instanceof LineTo l) {
				dst.getElements().add(new LineTo(
						l.getX() + dx, l.getY() + dy));

			} else if (e instanceof QuadCurveTo q) {
				dst.getElements().add(new QuadCurveTo(
						q.getControlX() + dx, q.getControlY() + dy,
						q.getX() + dx, q.getY() + dy));

			} else if (e instanceof ArcTo a) {
				ArcTo arc2 = new ArcTo();
				arc2.setRadiusX(a.getRadiusX());
				arc2.setRadiusY(a.getRadiusY());
				arc2.setXAxisRotation(a.getXAxisRotation());
				arc2.setLargeArcFlag(a.isLargeArcFlag());
				arc2.setSweepFlag(a.isSweepFlag());
				arc2.setX(a.getX() + dx);
				arc2.setY(a.getY() + dy);
				dst.getElements().add(arc2);
			} else if (e instanceof ClosePath) {
				dst.getElements().add(new ClosePath());
			} else {
				throw new IllegalArgumentException("Unsupported PathElement type: " + e.getClass());
			}
		}

		return dst;
	}

	/**
	 * Flip a path vertically across the horizontal line y = cy.
	 * (i.e. mirror up/down around cy)
	 */
	public static Path flipVertical(Path src, double cy) {
		Path dst = new Path();

		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(src.getFill());

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				double x = m.getX();
				double y = mirrorY(m.getY(), cy);
				dst.getElements().add(new MoveTo(x, y));

			} else if (e instanceof LineTo l) {
				double x = l.getX();
				double y = mirrorY(l.getY(), cy);
				dst.getElements().add(new LineTo(x, y));

			} else if (e instanceof QuadCurveTo q) {
				double cpx = q.getControlX();
				double cpy = mirrorY(q.getControlY(), cy);
				double x = q.getX();
				double y = mirrorY(q.getY(), cy);
				dst.getElements().add(new QuadCurveTo(cpx, cpy, x, y));

			} else if (e instanceof ArcTo a) {
				ArcTo arc2 = new ArcTo();
				arc2.setRadiusX(a.getRadiusX());
				arc2.setRadiusY(a.getRadiusY());

				// vertical reflection: x-axis rotation θ -> -θ
				double theta = a.getXAxisRotation();
				double newTheta = -theta;
				arc2.setXAxisRotation(newTheta);

				// large-arc flag unchanged; sweep flag flips (orientation reversal)
				arc2.setLargeArcFlag(a.isLargeArcFlag());
				arc2.setSweepFlag(!a.isSweepFlag());

				double x = a.getX();
				double y = mirrorY(a.getY(), cy);
				arc2.setX(x);
				arc2.setY(y);

				dst.getElements().add(arc2);

			} else if (e instanceof ClosePath) {
				dst.getElements().add(new ClosePath());
			} else {
				throw new IllegalArgumentException("Unsupported PathElement type: " + e.getClass());
			}
		}

		return dst;
	}

	/**
	 * Map a path from oldBox to newBox by an affine scale+translate:
	 * x' = newMinX + (x - oldMinX) * (newWidth  / oldWidth)
	 * y' = newMinY + (y - oldMinY) * (newHeight / oldHeight)
	 * <p>
	 * Supports MoveTo, LineTo, QuadCurveTo, ArcTo, ClosePath.
	 */
	public static Path fitToBounds(Path src, BBox oldBox, BBox newBox) {
		double ow = oldBox.width();
		double oh = oldBox.height();

		if (ow == 0 || oh == 0) {
			throw new IllegalArgumentException("oldBox has zero width or height");
		}

		double nw = newBox.width();
		double nh = newBox.height();

		double sx = nw / ow;
		double sy = nh / oh;

		double ox = oldBox.xMin();
		double oy = oldBox.yMin();
		double nx = newBox.xMin();
		double ny = newBox.yMin();

		Path dst = new Path();

		// copy styling if you like
		dst.setStroke(src.getStroke());
		dst.setStrokeWidth(src.getStrokeWidth());
		dst.setFill(src.getFill());

		for (PathElement e : src.getElements()) {
			if (e instanceof MoveTo m) {
				double x = mapX(m.getX(), ox, sx, nx);
				double y = mapY(m.getY(), oy, sy, ny);
				dst.getElements().add(new MoveTo(x, y));

			} else if (e instanceof LineTo l) {
				double x = mapX(l.getX(), ox, sx, nx);
				double y = mapY(l.getY(), oy, sy, ny);
				dst.getElements().add(new LineTo(x, y));

			} else if (e instanceof QuadCurveTo q) {
				double cpx = mapX(q.getControlX(), ox, sx, nx);
				double cpy = mapY(q.getControlY(), oy, sy, ny);
				double x = mapX(q.getX(), ox, sx, nx);
				double y = mapY(q.getY(), oy, sy, ny);
				dst.getElements().add(new QuadCurveTo(cpx, cpy, x, y));

			} else if (e instanceof ArcTo a) {
				ArcTo arc2 = new ArcTo();

				// scale radii; for non-uniform scaling this is an approximation
				arc2.setRadiusX(a.getRadiusX() * sx);
				arc2.setRadiusY(a.getRadiusY() * sy);

				arc2.setXAxisRotation(a.getXAxisRotation());
				arc2.setLargeArcFlag(a.isLargeArcFlag());
				arc2.setSweepFlag(a.isSweepFlag());

				double x = mapX(a.getX(), ox, sx, nx);
				double y = mapY(a.getY(), oy, sy, ny);
				arc2.setX(x);
				arc2.setY(y);

				dst.getElements().add(arc2);

			} else if (e instanceof ClosePath) {
				dst.getElements().add(new ClosePath());
			} else {
				throw new IllegalArgumentException("Unsupported PathElement type: " + e.getClass());
			}
		}

		return dst;
	}

	// --- helpers -----------------------------------------------------------

	private static double mapX(double x, double oldMinX, double sx, double newMinX) {
		return newMinX + (x - oldMinX) * sx;
	}

	private static double mapY(double y, double oldMinY, double sy, double newMinY) {
		return newMinY + (y - oldMinY) * sy;
	}

	private static Point2D rotatePoint(double x, double y,
									   double cx, double cy,
									   double cos, double sin) {
		double dx = x - cx;
		double dy = y - cy;
		double xr = cx + cos * dx - sin * dy;
		double yr = cy + sin * dx + cos * dy;
		return new Point2D(xr, yr);
	}

	private static double mirrorX(double x, double cx) {
		return 2 * cx - x;
	}

	private static double mirrorY(double y, double cy) {
		return 2 * cy - y;
	}


}
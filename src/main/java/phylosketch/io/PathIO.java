/*
 * PathIO.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.io;

import javafx.scene.shape.*;
import jloda.util.StringUtils;

/**
 * path i/o
 * Daniel Huson, 11.2025
 */
public final class PathIO {
	/**
	 * Convert a JavaFX Path into a compact string representation.
	 * Format: SVG-style absolute commands, e.g.:
	 * "M 100 100 L 200 100 A 50 50 0 0 1 250 150 Z"
	 */
	public static String toString(Path path) {
		var sb = new StringBuilder();

		for (var e : path.getElements()) {
			if (e instanceof MoveTo m) {
				sb.append("M ")
						.append(m.getX()).append(' ')
						.append(m.getY()).append(' ');
			} else if (e instanceof LineTo l) {
				sb.append("L ")
						.append(l.getX()).append(' ')
						.append(l.getY()).append(' ');
			} else if (e instanceof HLineTo h) {
				sb.append("H ")
						.append(h.getX()).append(' ');
			} else if (e instanceof VLineTo v) {
				sb.append("V ")
						.append(v.getY()).append(' ');
			} else if (e instanceof QuadCurveTo q) {
				sb.append("Q ")
						.append(q.getControlX()).append(' ')
						.append(q.getControlY()).append(' ')
						.append(q.getX()).append(' ')
						.append(q.getY()).append(' ');
			} else if (e instanceof ArcTo a) {
				sb.append("A ")
						.append(a.getRadiusX()).append(' ')
						.append(a.getRadiusY()).append(' ')
						.append(a.getXAxisRotation()).append(' ')
						.append(a.isLargeArcFlag() ? 1 : 0).append(' ')
						.append(a.isSweepFlag() ? 1 : 0).append(' ')
						.append(a.getX()).append(' ')
						.append(a.getY()).append(' ');
			} else if (e instanceof ClosePath) {
				sb.append("Z ");
			}
		}
		return sb.toString().trim();
	}

	/**
	 * Parse a Path from a string created by toString(Path).
	 * Accepts the same SVG-like format: M, L, H, V, Q, A, Z.
	 */
	public static Path fromString(String s) {
		var path = new Path();

		if (s.contains(",")) { // backwards compatibility
			var tokens = StringUtils.split(s, ',');
			for (var i = 0; i + 1 < tokens.length; i += 2) {
				var x = Double.parseDouble(tokens[i]);
				var y = Double.parseDouble(tokens[i + 1]);
				if (i == 0) {
					path.getElements().add(new MoveTo(x, y));
				} else {
					path.getElements().add(new LineTo(x, y));
				}
			}
			return path;
		}

		var tokens = s.trim().split("\\s+");
		if (tokens.length == 0)
			return path;


		double cx = 0, cy = 0; // current point, used for H/V handling

		var i = 0;
		while (i < tokens.length) {
			var cmd = tokens[i++];

			switch (cmd) {
				case "M" -> {
					var x = Double.parseDouble(tokens[i++]);
					var y = Double.parseDouble(tokens[i++]);
					path.getElements().add(new MoveTo(x, y));
					cx = x;
					cy = y;
				}
				case "L" -> {
					var x = Double.parseDouble(tokens[i++]);
					var y = Double.parseDouble(tokens[i++]);
					path.getElements().add(new LineTo(x, y));
					cx = x;
					cy = y;
				}
				case "H" -> {
					var x = Double.parseDouble(tokens[i++]);
					path.getElements().add(new LineTo(x, cy));
					cx = x;
				}
				case "V" -> {
					double y = Double.parseDouble(tokens[i++]);
					path.getElements().add(new LineTo(cx, y));
					cy = y;
				}
				case "Q" -> {
					var cpx = Double.parseDouble(tokens[i++]);
					var cpy = Double.parseDouble(tokens[i++]);
					var x = Double.parseDouble(tokens[i++]);
					var y = Double.parseDouble(tokens[i++]);
					path.getElements().add(new QuadCurveTo(cpx, cpy, x, y));
					cx = x;
					cy = y;
				}
				case "A" -> {
					var rx = Double.parseDouble(tokens[i++]);
					var ry = Double.parseDouble(tokens[i++]);
					var rot = Double.parseDouble(tokens[i++]);
					var largeArc = Integer.parseInt(tokens[i++]) != 0;
					var sweep = Integer.parseInt(tokens[i++]) != 0;
					var x = Double.parseDouble(tokens[i++]);
					var y = Double.parseDouble(tokens[i++]);

					var arc = new ArcTo();
					arc.setRadiusX(rx);
					arc.setRadiusY(ry);
					arc.setXAxisRotation(rot);
					arc.setLargeArcFlag(largeArc);
					arc.setSweepFlag(sweep);
					arc.setX(x);
					arc.setY(y);
					path.getElements().add(arc);
					cx = x;
					cy = y;
				}
				case "Z" -> {
					path.getElements().add(new ClosePath());
				}
				default -> throw new IllegalArgumentException("Unknown path command: " + cmd);
			}
		}

		return path;
	}
}
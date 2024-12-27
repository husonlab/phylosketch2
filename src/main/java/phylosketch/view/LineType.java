/*
 * LineType.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.scene.shape.Shape;

import java.util.List;

/**
 * line types
 * Daniel Huson, 11.2024
 */
public enum LineType {
	Solid(),
	Dot(2.0, 8.0),
	Dash(10.0, 10.0);

	public final List<Double> strokeDashArray;

	private LineType(Double... dashArray) {
		strokeDashArray = List.of(dashArray);
	}

	public void applyTo(Shape shape) {
		shape.getStrokeDashArray().setAll(strokeDashArray);
	}

	public static LineType fromShape(Shape shape) {
		for (var type : values()) {
			if (shape != null && shape.getStrokeDashArray().equals(type.strokeDashArray))
				return type;
		}
		return Solid;
	}
}
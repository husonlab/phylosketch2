/*
 * NodeShape.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.shape.Polygon;

import java.util.Objects;

/**
 * simple node shape class
 * Daniel Huson, 11.2025
 */
public class NodeShape extends Polygon {

	public enum Type {
		Square,
		Circle,
		TriangleUp,
		TriangleDown,
		Diamond,
		Hexagon
	}

	private static final double DEFAULT_SIZE = 6;

	private double size;   // width = height = size
	private Type type;

	public NodeShape() {
		this(Type.Circle, DEFAULT_SIZE);
	}

	public NodeShape(Type type) {
		this(type, DEFAULT_SIZE);
	}

	public NodeShape(Type type, double size) {
		this.type = Objects.requireNonNull(type, "type must not be null");
		setSize(size); // will also call updateShape()
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		if (this.type != type) {
			this.type = type;
			updateShape();
		}
	}

	/**
	 * Returns the logical size of the shape. Width = height = size.
	 */
	public double getSize() {
		return size;
	}

	/**
	 * Sets the logical size of the shape. Width = height = size.
	 * Triggers recomputation of polygon points.
	 * For circles, this also updates the number of segments used
	 * for the polygon approximation.
	 */
	public void setSize(double size) {
		if (size <= 0) {
			throw new IllegalArgumentException("size must be > 0");
		}
		if (this.size != size) {
			this.size = size;
			updateShape();
		}
	}

	/**
	 * Recomputes the polygon points based on current size and type.
	 * All shapes are centered at (0,0).
	 */
	private void updateShape() {
		getPoints().clear();
		switch (type) {
			case Square -> buildSquare();
			case Circle -> buildCircle();
			case TriangleUp -> buildTriangleUp();
			case TriangleDown -> buildTriangleDown();
			case Diamond -> buildDiamond();
			case Hexagon -> buildHexagon();
		}
	}

	private void buildSquare() {
		double h = size / 2.0;
		getPoints().addAll(
				-h, -h,
				h, -h,
				h, h,
				-h, h
		);
	}

	private void buildTriangleUp() {
		double h = size / 2.0;
		getPoints().addAll(
				0.0, -h,   // top
				-h, h,    // bottom left
				h, h     // bottom right
		);
	}

	private void buildTriangleDown() {
		double h = size / 2.0;
		getPoints().addAll(
				-h, -h,    // top left
				h, -h,    // top right
				0.0, h    // bottom
		);
	}

	private void buildDiamond() {
		double h = size / 2.0;
		getPoints().addAll(
				0.0, -h,   // top
				h, 0.0,  // right
				0.0, h,   // bottom
				-h, 0.0   // left
		);
	}

	private void buildHexagon() {
		double r = size / 2.0;
		// Regular hexagon centered at (0,0), 6 vertices 60° apart
		for (int i = 0; i < 6; i++) {
			double angle = Math.toRadians(60 * i - 30); // flat top
			double x = r * Math.cos(angle);
			double y = r * Math.sin(angle);
			getPoints().addAll(x, y);
		}
	}

	private void buildCircle() {
		double r = size / 2.0;

		// Choose number of segments based on circumference and
		// a desired segment length in pixels (here ~4 px).
		double circumference = 2.0 * Math.PI * r;
		int segments = (int) Math.round(circumference / 4.0);

		// Clamp to a reasonable range.
		segments = Math.max(12, Math.min(segments, 96));

		for (int i = 0; i < segments; i++) {
			double angle = 2.0 * Math.PI * i / segments;
			double x = r * Math.cos(angle);
			double y = r * Math.sin(angle);
			getPoints().addAll(x, y);
		}
	}
}
/*
 * LabelPlacer.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.daniel;

import javafx.geometry.Point2D;
import javafx.scene.text.TextAlignment;
import jloda.fx.control.RichTextLabel;

/**
 * place labels
 * Daniel Huson, 8.2025
 */
public class LabelPlacer {
	/**
	 * Place a leaf label for a node, using a TextFlow-based RichTextLabel.
	 * Coordinates must be in the same parent coordinate space as the label.
	 *
	 * @param label       Your RichTextLabel (extends TextFlow). Contents should already be set.
	 * @param nodePos     Node center (same coordinate space as label).
	 * @param nodeRadius  Visual radius of the node (for spacing).
	 * @param leftToRight left-to-right layout, otherwise circular
	 * @param center      Layout center for circular
	 * @param gap         Extra pixels between node boundary and label.
	 */
	public static void placeLeafLabel(RichTextLabel label, Point2D nodePos, double nodeRadius, boolean leftToRight, Point2D center, double gap) {
		label.applyCss();
		label.layout(); // compute layout bounds
		double lw = label.getLayoutBounds().getWidth();
		double lh = label.getLayoutBounds().getHeight();

		if (leftToRight)
			placeRectangular(label, nodePos, nodeRadius, gap, lw, lh);
		else
			placeCircular(label, nodePos, nodeRadius, gap, lw, lh, center);
	}

	private static void placeRectangular(RichTextLabel label, Point2D p, double radius, double gap, double lw, double lh) {
		label.setTextAlignment(TextAlignment.LEFT);
		double x = p.getX() + radius + gap;
		double y = p.getY() - lh / 2.0; // center vertically on node
		label.setLayoutX(x);
		label.setLayoutY(y);
	}

	// --- Circular: place "outside" the incoming edge (or radial from center), snapped to octants
	private static void placeCircular(RichTextLabel label, Point2D p, double r, double gap, double lw, double lh, Point2D center) {
		// Direction along incoming edge into the leaf = (p - inFrom).
		// Placing the label "outside" uses the same direction.
		Point2D d = p.subtract(center);
		if (d.magnitude() == 0) d = new Point2D(1, 0);
		Point2D u = d.normalize();
		int oct = octant(Math.atan2(u.getY(), u.getX())); // 0..7 (E,NE,N,NW,W,SW,S,SE)

		switch (oct) {
			case 0 -> { // E
				label.setTextAlignment(TextAlignment.LEFT);
				label.setLayoutX(p.getX() + r + gap);
				label.setLayoutY(p.getY() - lh / 2.0);
			}
			case 1 -> { // SE (below-right)
				label.setTextAlignment(TextAlignment.LEFT);
				label.setLayoutX(p.getX() + r + gap);
				label.setLayoutY(p.getY() + gap + lh / 4.0);
			}
			case 2 -> { // S (below)
				label.setTextAlignment(TextAlignment.CENTER);
				label.setLayoutX(p.getX() - lw / 2.0);
				label.setLayoutY(p.getY() + r + gap);
			}
			case 3 -> { // SW (below-left)
				label.setTextAlignment(TextAlignment.RIGHT);
				label.setLayoutX(p.getX() - r - gap - lw);
				label.setLayoutY(p.getY() + gap + lh / 4.0);
			}
			case 4 -> { // W
				label.setTextAlignment(TextAlignment.RIGHT);
				label.setLayoutX(p.getX() - r - gap - lw);
				label.setLayoutY(p.getY() - lh / 2.0);
			}
			case 5 -> { // NW (above-left)
				label.setTextAlignment(TextAlignment.RIGHT);
				label.setLayoutX(p.getX() - r - gap - lw);
				label.setLayoutY(p.getY() - gap - lh / 4.0);
			}
			case 6 -> { // N (above)
				label.setTextAlignment(TextAlignment.CENTER);
				label.setLayoutX(p.getX() - lw / 2.0);
				label.setLayoutY(p.getY() - r - gap - lh);
			}
			case 7 -> { // NE (above-right)
				label.setTextAlignment(TextAlignment.LEFT);
				label.setLayoutX(p.getX() + r + gap);
				label.setLayoutY(p.getY() - gap - lh / 4.0);
			}
		}
	}

	// Map angle (radians) to 8 octants: 0=E,1=NE,2=N,3=NW,4=W,5=SW,6=S,7=SE
	private static int octant(double angleRad) {
		double twoPi = Math.PI * 2.0;
		double a = angleRad;
		if (a < 0) a += twoPi;
		double oct = (a + Math.PI / 8.0) / (Math.PI / 4.0);
		return ((int) Math.floor(oct)) & 7;
	}
}
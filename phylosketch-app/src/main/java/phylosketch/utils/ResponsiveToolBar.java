/*
 *  ResponsiveToolBar.java Copyright (C) 2026 Daniel H. Huson
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
 */

package phylosketch.utils;

/*
 *  ResponsiveToolBar.java Copyright (C) 2026 Daniel H. Huson
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
 */

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * A two-part tool bar. When there is room it places the two parts side by side in one row, the left
 * part flushed left and the right part flushed right, and reports a single-row height. When the
 * window is too narrow it stacks them, the left part on top (left-aligned) and the right part below
 * (right-aligned), and reports the taller, two-row height.
 * <p>
 * The two parts are the first and second children, so this can be used directly in FXML. Because its
 * height depends on its width, it reports a {@link Orientation#HORIZONTAL} content bias; that is what
 * makes the layout system resolve the width first and then ask for the matching height, so the bar is
 * single height when wide and double when stacked.
 * <p>
 * Daniel Huson, 6.2026
 */
public class ResponsiveToolBar extends Pane {
	private final DoubleProperty gap = new SimpleDoubleProperty(this, "gap", 8);

	public ResponsiveToolBar() {
		gap.addListener((v, o, n) -> requestLayout());
	}

	public double getGap() {
		return gap.get();
	}

	public void setGap(double value) {
		gap.set(value);
	}

	public DoubleProperty gapProperty() {
		return gap;
	}

	private Region left() {
		return (Region) getChildren().get(0);
	}

	private Region right() {
		return (Region) getChildren().get(1);
	}

	@Override
	public Orientation getContentBias() {
		return Orientation.HORIZONTAL; // our height depends on our width
	}

	@Override
	protected void layoutChildren() {
		var insets = getInsets();
		var x = insets.getLeft();
		var y = insets.getTop();
		var w = getWidth() - insets.getLeft() - insets.getRight();
		var h = getHeight() - insets.getTop() - insets.getBottom();

		if (left().prefWidth(-1) + getGap() + right().prefWidth(-1) <= w) {
			// wide: one row, left flushed left, right flushed right
			layoutInArea(left(), x, y, w, h, 0, Insets.EMPTY, false, false, HPos.LEFT, VPos.CENTER);
			layoutInArea(right(), x, y, w, h, 0, Insets.EMPTY, false, false, HPos.RIGHT, VPos.CENTER);
		} else {
			// narrow: stacked, left part on top (left-aligned), right part below (right-aligned)
			var lh = left().prefHeight(-1);
			var rh = right().prefHeight(-1);
			layoutInArea(left(), x, y, w, lh, 0, Insets.EMPTY, false, false, HPos.LEFT, VPos.TOP);
			layoutInArea(right(), x, y + lh + getGap(), w, rh, 0, Insets.EMPTY, false, false, HPos.RIGHT, VPos.TOP);
		}
	}

	@Override
	protected double computePrefWidth(double height) {
		var insets = getInsets();
		return insets.getLeft() + left().prefWidth(-1) + getGap() + right().prefWidth(-1) + insets.getRight();
	}

	@Override
	protected double computeMinWidth(double height) {
		// may shrink to the wider of the two parts (the stacked case), so it never forces the
		// window to stay wide
		var insets = getInsets();
		return insets.getLeft() + Math.max(left().minWidth(-1), right().minWidth(-1)) + insets.getRight();
	}

	@Override
	protected double computePrefHeight(double width) {
		var insets = getInsets();
		// use the given width, falling back to the live width if a container passes -1
		var w = (width >= 0 ? width : getWidth()) - insets.getLeft() - insets.getRight();
		var oneRow = w > 0 && left().prefWidth(-1) + getGap() + right().prefWidth(-1) <= w;
		var content = oneRow
				? Math.max(left().prefHeight(-1), right().prefHeight(-1))
				: left().prefHeight(-1) + getGap() + right().prefHeight(-1);
		return insets.getTop() + content + insets.getBottom();
	}
}
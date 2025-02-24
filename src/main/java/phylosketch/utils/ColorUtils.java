/*
 * ColorUtils.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import jloda.fx.util.ColorUtilsFX;
import jloda.fx.window.MainWindowManager;

import java.util.regex.Pattern;

/**
 * utils for setting graph node and edge colors
 * Daniel Huson, 2.2025
 */
public class ColorUtils {
	public static void setStroke(Shape shape, String color, String StyleClass) {
		setStroke(shape, color == null ? null : ColorUtilsFX.parseColor(color), StyleClass);
	}
	
	/**
	 * set the stroke in such a way that day/night mode works as expected
	 *
	 * @param shape      the shape
	 * @param color      the new color
	 * @param styleClass the style class, e.g. graph-node or graph-edge
	 */
	public static void setStroke(Shape shape, Color color, String styleClass) {
		if (color == null || (styleClass.endsWith("graph-edge") &&
							  (!MainWindowManager.isUseDarkTheme() && color == Color.BLACK ||
							   MainWindowManager.isUseDarkTheme() && color == Color.WHITE))
			|| (styleClass.endsWith("graph-node-hollow") &&
				(!MainWindowManager.isUseDarkTheme() && color == Color.WHITE ||
				 MainWindowManager.isUseDarkTheme() && color == Color.BLACK))) {
			removeStroke(shape);
			if (!shape.getStyleClass().contains(styleClass)) {
				shape.getStyleClass().add(styleClass);
			}
		} else {
			shape.getStyleClass().remove(styleClass);
			setOrReplaceStroke(shape, color);
		}
	}

	public static void setFill(Shape shape, String color, String StyleClass) {
		setFill(shape, color == null ? null : ColorUtilsFX.parseColor(color), StyleClass);
	}

	/**
	 * set the fill in such a way that day/night mode works as expected
	 *
	 * @param shape      the shape
	 * @param color      the new color
	 * @param styleClass the style class, e.g. graph-node or graph-edge
	 */
	public static void setFill(Shape shape, Color color, String styleClass) {
		if (color == null || (styleClass.endsWith("graph-node") &&
							  (!MainWindowManager.isUseDarkTheme() && color == Color.BLACK ||
							   MainWindowManager.isUseDarkTheme() && color == Color.WHITE))
			|| (styleClass.endsWith("graph-node-hollow") &&
				(!MainWindowManager.isUseDarkTheme() && color == Color.BLACK ||
				 MainWindowManager.isUseDarkTheme() && color == Color.WHITE))) {
			removeFill(shape);
			if (!shape.getStyleClass().contains(styleClass)) {
				shape.getStyleClass().add(styleClass);
			}
		} else {
			shape.getStyleClass().remove(styleClass);
			setOrReplaceFill(shape, color);
		}
	}

	public static void setOrReplaceStroke(Shape shape, Color color) {
		var strokePattern = Pattern.compile("-fx-stroke: [^;]+;");
		var style = shape.getStyle() == null ? "" : shape.getStyle();
		var colorCSS = ColorUtilsFX.toStringCSS(color);

		if (strokePattern.matcher(style).find()) {
			style = style.replaceAll("-fx-stroke: [^;]+;", "-fx-stroke: " + colorCSS + ";");
		} else {
			style = style + " -fx-stroke: " + colorCSS + ";";
		}
		shape.setStyle(style.trim());
	}

	public static void removeStroke(Shape shape) {
		if (shape.getStyle() != null) {
			var style = shape.getStyle().replaceAll("-fx-stroke: [^;]+;", "").trim();
			shape.setStyle(style.isBlank() ? null : style.trim());
		}
	}

	public static void setOrReplaceFill(Shape shape, Color color) {
		var fillPattern = Pattern.compile("-fx-fill: [^;]+;");
		var style = shape.getStyle() == null ? "" : shape.getStyle();
		var colorCSS = ColorUtilsFX.toStringCSS(color);

		if (fillPattern.matcher(style).find()) {
			style = style.replaceAll("-fx-fill: [^;]+;", "-fx-fill: " + colorCSS + ";");
		} else {
			style = style + " -fx-fill: " + colorCSS + ";";
		}
		shape.setStyle(style.trim());
	}

	public static void removeFill(Shape shape) {
		if (shape.getStyle() != null) {
			var style = shape.getStyle().replaceAll("-fx-fill: [^;]+;", "").trim();
			shape.setStyle(style.isBlank() ? null : style.trim());
		}
	}
}

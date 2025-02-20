/*
 * RootTool.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.pane;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jloda.fx.icons.MaterialIcons;
import phylosketch.view.DrawView;

/**
 * setup root selection tool
 * Daniel Huson, 1.2025
 */
public class RootTool extends HBox {
	private static double mouseX;
	private static double mouseY;
	private static double oldHBoxTranslateX;
	private static double oldHBoxTranslateY;

	private final Circle rootCircle;

	/**
	 * constructs the root selection tool
	 *
	 * @param view draw view
	 */
	public RootTool(DrawView view) {
		setAlignment(Pos.CENTER);
		setSpacing(5);
		setTranslateX(200);
		setTranslateY(200);

		var shadow = new DropShadow();
		shadow.setRadius(5);
		shadow.setOffsetX(3);
		shadow.setOffsetY(3);
		shadow.setColor(Color.gray(0.4));

		var rootIcon = (Label) MaterialIcons.graphic(MaterialIcons.rocket_launch, "-fx-rotate: 45;-fx-text-fill: red;");
		rootIcon.setEffect(shadow);

		rootCircle = new Circle(5);
		rootCircle.setStyle("-fx-stroke: red;-fx-fill: transparent;-fx-stroke-width: 2;");
		rootCircle.setEffect(shadow);

		var rootLabel = new Label("Root location");
		rootLabel.setStyle("-fx-text-fill: red;-fx-font-size: 18;");
		rootLabel.setEffect(shadow);

		getChildren().addAll(rootIcon, rootCircle, rootLabel);

		setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			oldHBoxTranslateX = getTranslateX();
			oldHBoxTranslateY = getTranslateY();
		});

		setOnMouseDragged(e -> {
			var prev = screenToLocal(new Point2D(mouseX, mouseY));
			var loc = screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
			var delta = loc.subtract(prev);
			setTranslateX(getTranslateX() + delta.getX());
			setTranslateY(getTranslateY() + delta.getY());
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
		});
		setOnMouseReleased(e -> {
			if (!e.isStillSincePress()) {
				var newHBoxTranslateX = getTranslateX();
				var newHBoxTranslateY = getTranslateY();
				view.getUndoManager().doAndAdd("root location", () -> {
					setTranslateX(oldHBoxTranslateX);
					setTranslateY(oldHBoxTranslateY);
				}, () -> {
					setTranslateX(newHBoxTranslateX);
					setTranslateY(newHBoxTranslateY);
				});
			}
		});
	}

	public Point2D getRootLocationOnScreen() {
		return rootCircle.localToScreen(0, 0);
	}
}

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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jloda.fx.icons.MaterialIcons;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

/**
 * setup root selection tool
 * Daniel Huson, 1.2025
 */
public class RootTool extends StackPane {
	private static double mouseX;
	private static double mouseY;
	private static double oldHBoxTranslateX;
	private static double oldHBoxTranslateY;

	private final Circle rootCircle;

	private final ObjectProperty<RootPosition.Side> rootSide = new SimpleObjectProperty<>(this, "rootSide", null);

	/**
	 * constructs the root selection tool
	 *
	 * @param view draw view
	 */
	public RootTool(DrawView view) {
		var shadow = new DropShadow();
		shadow.setRadius(5);
		shadow.setOffsetX(3);
		shadow.setOffsetY(3);
		shadow.setColor(Color.gray(0.4));

		var grid = new GridPane();
		grid.setHgap(3);
		grid.setVgap(3);
		grid.setAlignment(Pos.CENTER);
		getChildren().setAll(grid);

		{
			rootCircle = new Circle(5);
			rootCircle.setStyle("-fx-stroke: red;-fx-fill: transparent;-fx-stroke-width: 2;");
			var stack = new StackPane(rootCircle);
			StackPane.setAlignment(rootCircle, Pos.CENTER);
			stack.setEffect(shadow);
			grid.add(stack, 1, 1);
		}

		{
			var rootLabel = new Label("Root");
			rootLabel.setStyle("-fx-text-fill: red;-fx-font-size: 18;");
			var stack = new StackPane(rootLabel);
			StackPane.setAlignment(rootLabel, Pos.BOTTOM_LEFT);
			stack.setEffect(shadow);
			grid.add(stack, 2, 0);

			rootSide.addListener((v, o, n) -> {
				Tooltip.install(RootTool.this, new Tooltip("Assuming root is at " + n.name().toLowerCase() + ", click to change"));
				rootLabel.setText("Root at " + n.name().toLowerCase());
			});
		}

		{
			var rootLeftArrow = (Label) MaterialIcons.graphic(MaterialIcons.rocket_launch, "-fx-rotate: 45;-fx-text-fill: red;-fx-font-size: 24;");
			rootLeftArrow.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Left).or(rootSide.isEqualTo(RootPosition.Side.Center)));
			var stack = new StackPane(rootLeftArrow);
			StackPane.setAlignment(rootLeftArrow, Pos.CENTER_LEFT);
			stack.setEffect(shadow);
			grid.add(stack, 2, 1);
		}

		{
			var rootRightArrow = (Label) MaterialIcons.graphic(MaterialIcons.rocket_launch, "-fx-rotate: 225;-fx-text-fill: red;-fx-font-size: 24;");
			rootRightArrow.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Right).or(rootSide.isEqualTo(RootPosition.Side.Center)));

			var stack = new StackPane(rootRightArrow);
			StackPane.setAlignment(rootRightArrow, Pos.CENTER_LEFT);
			stack.setEffect(shadow);
			grid.add(stack, 0, 1);
		}

		{
			var rootBottomArrow = (Label) MaterialIcons.graphic(MaterialIcons.rocket_launch, "-fx-rotate: 315;-fx-text-fill: red;-fx-font-size: 24;");
			rootBottomArrow.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Bottom).or(rootSide.isEqualTo(RootPosition.Side.Center)));
			rootBottomArrow.setEffect(shadow);
			var stack = new StackPane(rootBottomArrow);
			StackPane.setAlignment(rootBottomArrow, Pos.CENTER_LEFT);
			stack.setEffect(shadow);
			grid.add(stack, 1, 0);
		}

		{
			var rootTopArrow = (Label) MaterialIcons.graphic(MaterialIcons.rocket_launch, "-fx-rotate: 135;-fx-text-fill: red;-fx-font-size: 24;");
			rootTopArrow.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Top).or(rootSide.isEqualTo(RootPosition.Side.Center)));
			var stack = new StackPane(rootTopArrow);
			StackPane.setAlignment(rootTopArrow, Pos.CENTER_LEFT);
			stack.setEffect(shadow);
			grid.add(stack, 1, 2);
		}

		rootSide.set(RootPosition.Side.Left);

		setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			oldHBoxTranslateX = getTranslateX();
			oldHBoxTranslateY = getTranslateY();
			e.consume();
		});

		setOnMouseDragged(e -> {
			var prev = screenToLocal(new Point2D(mouseX, mouseY));
			var loc = screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
			var delta = loc.subtract(prev);
			setTranslateX(getTranslateX() + delta.getX());
			setTranslateY(getTranslateY() + delta.getY());
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});

		setOnMouseReleased(e -> {
			if (!e.isStillSincePress()) {
				var oldX = oldHBoxTranslateX;
				var oldY = oldHBoxTranslateY;
				var newX = getTranslateX();
				var newY = getTranslateY();
				view.getUndoManager().doAndAdd("root location", () -> {
					setTranslateX(oldX);
					setTranslateY(oldY);
				}, () -> {
					setTranslateX(newX);
					setTranslateY(newY);
				});
			}
			e.consume();
		});

		setOnMouseClicked(e -> {
			if (e.isStillSincePress()) {
				setRootSide(switch (getRootSide()) {
					case Left -> RootPosition.Side.Top;
					case Top -> RootPosition.Side.Right;
					case Right -> RootPosition.Side.Bottom;
					case Bottom -> RootPosition.Side.Center;
					case Center -> RootPosition.Side.Left;
				});
			}
			e.consume();
		});
	}

	public Point2D getRootLocationOnScreen() {
		return rootCircle.localToScreen(0, 0);
	}

	public RootPosition.Side getRootSide() {
		return rootSide.get();
	}

	public void setRootSide(RootPosition.Side rootSide) {
		this.rootSide.set(rootSide);
	}

	public ObjectProperty<RootPosition.Side> rootSideProperty() {
		return rootSide;
	}
}

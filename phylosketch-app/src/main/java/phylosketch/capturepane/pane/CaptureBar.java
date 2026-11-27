/*
 * CaptureBar.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import jloda.fx.icons.MaterialIcons;
import phylosketch.view.RootPosition;

/**
 * the bar that floats at the top of the pane while a picture waits to be captured: what to do, the direction in
 * which the drawing grows, and the button that captures
 * <p>
 * It is part of the window, not of the drawing, so it keeps its place and its size when the picture is scrolled or
 * zoomed, and it does not cover the root. The button can be pressed once the root marker is on a line: beside
 * a line, capture finds no root.
 * Daniel Huson, 11.2026
 */
public class CaptureBar extends HBox {
	private static final String CAPTURE_STYLE = "-fx-background-color: -fx-accent; -fx-background-radius: 6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 14 5 14;";
	private static final String CAPTURE_STYLE_HOVER = CAPTURE_STYLE.replace("-fx-background-color: -fx-accent;", "-fx-background-color: derive(-fx-accent, -15%);");

	private final Button captureButton = new Button("Capture");

	/**
	 * constructs the bar
	 *
	 * @param rootSide the side that the root is on, which the bar shows and sets
	 * @param onLine   is the root marker on a line?
	 * @param running  is a capture running?
	 * @param capture  captures the phylogeny
	 */
	public CaptureBar(ObjectProperty<RootPosition.Side> rootSide, ObservableBooleanValue onLine, ObservableBooleanValue running, Runnable capture) {
		var hint = new Label();
		hint.textProperty().bind(Bindings.when(running).then("Capturing...").otherwise("Put the marker on the root of the tree, then press Capture"));
		hint.setWrapText(true);
		hint.setMaxWidth(190);
		hint.setStyle("-fx-font-size: 12;");

		var directionButton = new MenuButton();
		directionButton.setTooltip(new javafx.scene.control.Tooltip("The direction in which the tree grows"));
		for (var side : new RootPosition.Side[]{RootPosition.Side.Left, RootPosition.Side.Top, RootPosition.Side.Right, RootPosition.Side.Bottom, RootPosition.Side.Center}) {
			var item = new MenuItem(RootTool.getDirection(side), MaterialIcons.graphic(getIcon(side)));
			item.setOnAction(e -> rootSide.set(side));
			directionButton.getItems().add(item);
		}
		rootSide.addListener((v, o, n) -> showDirection(directionButton, n));
		showDirection(directionButton, rootSide.get());
		directionButton.disableProperty().bind(running);
		// the window's style sheet makes all buttons flat, as in the tool bar: here they are to look like buttons
		directionButton.setStyle("-fx-background-color: transparent; -fx-border-color: gray; -fx-border-radius: 6; -fx-border-width: 1; -fx-padding: 3 4 3 6;");

		captureButton.setStyle(CAPTURE_STYLE);
		captureButton.hoverProperty().addListener((v, o, n) -> captureButton.setStyle(n ? CAPTURE_STYLE_HOVER : CAPTURE_STYLE));
		captureButton.setTooltip(new javafx.scene.control.Tooltip("Capture the phylogeny from the picture"));
		captureButton.setOnAction(e -> capture.run());
		captureButton.disableProperty().bind(Bindings.not(onLine).or(running));

		var controls = new HBox(8, directionButton, captureButton);
		controls.setAlignment(Pos.CENTER);
		// side by side where there is room, the controls below the text in a narrow window
		var box = new FlowPane(12, 6, hint, controls);
		box.setAlignment(Pos.CENTER);
		box.setRowValignment(javafx.geometry.VPos.CENTER);
		box.setPrefWrapLength(460);
		box.setStyle("-fx-background-color: -fx-control-inner-background; -fx-background-radius: 8; -fx-border-color: gray; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 6 10 6 10;");

		getChildren().add(box);
		setAlignment(Pos.TOP_CENTER);
		setPickOnBounds(false); // the bar spans the pane to sit in the middle of it: what is beside the box stays within reach
	}

	private static void showDirection(MenuButton button, RootPosition.Side side) {
		if (side != null) {
			button.setText(RootTool.getDirection(side));
			button.setGraphic(MaterialIcons.graphic(getIcon(side)));
		}
	}

	/**
	 * an arrow that points the way the drawing grows
	 */
	private static MaterialIcons getIcon(RootPosition.Side side) {
		return switch (side) {
			case Left -> MaterialIcons.east;
			case Right -> MaterialIcons.west;
			case Top -> MaterialIcons.south;
			case Bottom -> MaterialIcons.north;
			case Center -> MaterialIcons.open_with;
		};
	}

	public Button getCaptureButton() {
		return captureButton;
	}
}

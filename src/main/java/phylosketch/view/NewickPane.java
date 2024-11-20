/*
 * NewickPane.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.ClipboardUtils;

import java.util.function.Supplier;

public class NewickPane {
	private static double mouseX;
	private static double mouseY;

	private static final double[] defaultLayout = {20, 20, 400, 100};

	public static void setup(AnchorPane anchorPane, Property<?> updateProperty, Supplier<String> stringSupplier, BooleanProperty showProperty) {
		var textArea = new TextArea();
		textArea.setText(stringSupplier.get());
		updateProperty.addListener((v, o, n) -> {
			if (showProperty.get()) {
				textArea.setText(stringSupplier.get());
			}
		});

		showProperty.addListener((v, o, n) -> {
			if (n) {
				textArea.setText(stringSupplier.get());
			}
		});

		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setFocusTraversable(false);

		textArea.setStyle("-fx-background-color: transparent; " +
						  "-fx-padding: 0;" +
						  "-fx-focus-color: transparent;" +
						  "-fx-faint-focus-color: transparent;" +
						  "-fx-font-family: 'Courier New';" +
						  "-fx-font-size: 14;");

		textArea.setMinWidth(150);
		textArea.setMaxWidth(TextArea.USE_PREF_SIZE);
		textArea.setPrefWidth(defaultLayout[2]);
		textArea.setMinHeight(75);
		textArea.setMaxHeight(TextArea.USE_PREF_SIZE);
		textArea.setPrefHeight(defaultLayout[3]);

		var root = new HBox();
		root.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		root.getStyleClass().add("viewer-background");
		root.setStyle("-fx-border-color: gray; ");

		root.getChildren().add(textArea);
		var closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(a -> showProperty.set(false));

		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");
		resizeHandle.setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});
		resizeHandle.setOnMouseDragged(e -> {
			var dx = e.getScreenX() - mouseX;
			var dy = e.getScreenY() - mouseY;
			if (dx > 0 || textArea.getPrefWidth() + dx >= 150) {
				textArea.setPrefWidth(textArea.getPrefWidth() + dx);
				defaultLayout[2] = textArea.getPrefWidth();
			}
			if (dy > 0 || textArea.getPrefHeight() + dx >= 75) {
				textArea.setPrefHeight(textArea.getPrefHeight() + dy);
				defaultLayout[3] = textArea.getPrefHeight();
			}
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});

		var copyButton = new Button("Copy");
		MaterialIcons.setIcon(copyButton, MaterialIcons.copy);
		copyButton.setOnAction(e -> {
			if (textArea.getSelectedText().isEmpty())
				ClipboardUtils.putString(textArea.getText());
			else ClipboardUtils.putString(textArea.getSelectedText());
		});

		var rightBorderPane = new BorderPane();
		var vbox = new VBox(closeButton, copyButton);
		vbox.setSpacing(5);
		rightBorderPane.setTop(vbox);
		rightBorderPane.setBottom(resizeHandle);
		root.getChildren().add(rightBorderPane);

		AnchorPane.setLeftAnchor(root, defaultLayout[0]);
		AnchorPane.setTopAnchor(root, defaultLayout[1]);


		rightBorderPane.setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});

		rightBorderPane.setOnMouseDragged(e -> {
			var dx = e.getScreenX() - mouseX;
			var dy = e.getScreenY() - mouseY;

			if (AnchorPane.getLeftAnchor(root) + dx >= 4 && AnchorPane.getLeftAnchor(root) + dx + root.getWidth() <= anchorPane.getWidth() - 16)
				AnchorPane.setLeftAnchor(root, AnchorPane.getLeftAnchor(root) + dx);
			if (AnchorPane.getTopAnchor(root) + dy >= 4 && AnchorPane.getTopAnchor(root) + dy + root.getHeight() <= anchorPane.getHeight() - 16)
				AnchorPane.setTopAnchor(root, AnchorPane.getTopAnchor(root) + dy);
			defaultLayout[0] = AnchorPane.getLeftAnchor(root);
			defaultLayout[1] = AnchorPane.getTopAnchor(root);
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});

		if (showProperty.get()) {
			anchorPane.getChildren().add(1, root);
		}
		showProperty.addListener((v, o, n) -> {
			if (n) {
				if (!anchorPane.getChildren().contains(root))
					anchorPane.getChildren().add(1, root);
			} else {
				anchorPane.getChildren().remove(root);
			}
		});
	}
}

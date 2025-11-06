/*
 * NodeLabelEditBox.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import jloda.fx.icons.MaterialIcons;
import jloda.graph.Node;
import phylosketch.commands.ChangeNodeLabelsCommand;

import java.util.Collections;
import java.util.function.Consumer;

/**
 * the node label edit box
 * Daniel Huson, 2.2025
 */
public class NodeLabelEditBox extends HBox {
	private final TextField textField;
	private final Button cancelButton;

	/**
	 * construct a node label edit box that allows editing of a node label
	 */
	public NodeLabelEditBox() {
		textField = new TextField();
		getChildren().add(textField);

		var dragButton = new Button("Drag");
		MaterialIcons.setIcon(dragButton, MaterialIcons.swap_horiz);
		var previous = new double[1];
		dragButton.setOnMousePressed(e -> {
			previous[0] = e.getSceneX();
		});
		dragButton.setOnMouseDragged(e -> {
			var dx = e.getSceneX() - previous[0];
			textField.setPrefWidth(Math.min(1000, Math.max(100, textField.getWidth() + dx)));
			previous[0] = e.getSceneX();
		});
		dragButton.setOnMouseReleased(e -> textField.requestFocus());
		getChildren().add(dragButton);

		cancelButton = new Button("Cancel");
		MaterialIcons.setIcon(cancelButton, MaterialIcons.cancel);
		getChildren().add(cancelButton);
	}

	/**
	 * set the node edit label box
	 * @param drawView the view
	 * @param screenX screen x
	 * @param screenY screen y
	 * @param v the node
	 * @param canceled canceled property
	 * @param runAfter run after method
	 */
	public static void show(DrawView drawView, double screenX, double screenY, Node v, BooleanProperty canceled, Runnable runAfter) {
		var vid = v.getId();
		var oldLabel = DrawView.getLabel(v).getText();
		drawView.getNodeLabelEditBox().show(drawView, oldLabel, screenX, screenY, canceled, newLabel -> {
			drawView.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(drawView, Collections.singletonList(new ChangeNodeLabelsCommand.Data(vid, oldLabel, newLabel))));
		}, runAfter);
	}

	private void show(DrawView view, String oldText, double screenX, double screenY, BooleanProperty canceled, Consumer<String> consumeNewText, Runnable runAfter) {
		var local = view.screenToLocal(screenX, screenY);

		setTranslateX(local.getX());
		setTranslateY(local.getY());

		textField.setText(oldText);
		setVisible(true);
		textField.requestFocus();
		textField.selectAll();

		cancelButton.setOnAction(e -> {
			if (canceled != null)
				canceled.set(true);
			setVisible(false);
		});
		textField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				if (canceled != null)
					canceled.set(true);
				setVisible(false);
			}
		});

		textField.setOnAction(e -> {
			var newLabel = textField.getText();
			if (!newLabel.equals(oldText)) {
				consumeNewText.accept(newLabel);
			}
			setVisible(false);
			if (runAfter != null)
				Platform.runLater(runAfter);
		});
	}
}

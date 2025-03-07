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

	public NodeLabelEditBox() {
		textField = new TextField();
		textField.focusedProperty().addListener((a, o, n) -> {
			if (n)
				textField.selectAll();
		});
		getChildren().add(textField);
	}

	public static void show(DrawView drawView, double screenX, double screenY, Node v, BooleanProperty canceled, Runnable runAfter) {
		var vid = v.getId();
		var oldLabel = DrawView.getLabel(v).getText();
		drawView.getNodeLabelEditBox().show(drawView, oldLabel, screenX, screenY, canceled, newLabel -> {
			drawView.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(drawView, Collections.singletonList(new ChangeNodeLabelsCommand.Data(vid, oldLabel, newLabel))));
		}, runAfter);
	}

	private void show(DrawView view, String oldText, double screenX, double screenY, BooleanProperty canceled, Consumer<String> consumeNewText, Runnable runAfter) {
		var local = view.screenToLocal(screenX, screenY);

		Button cancel;
		if (canceled != null) {
			cancel = new Button("Cancel");
			MaterialIcons.setIcon(cancel, MaterialIcons.cancel);
			cancel.setOnAction(e -> canceled.set(true));
			getChildren().add(cancel);
		} else cancel = null;

		setTranslateX(local.getX());
		setTranslateY(local.getY());
		setVisible(true);

		textField.setText(oldText);
		textField.requestFocus();
		textField.setOnAction(e -> {
			var newLabel = textField.getText();
			if (!newLabel.equals(oldText)) {
				consumeNewText.accept(newLabel);
			}
			setVisible(false);
			if (cancel != null)
				getChildren().remove(cancel);

			if (runAfter != null)
				Platform.runLater(runAfter);
		});
	}
}

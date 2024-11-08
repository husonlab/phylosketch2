/*
 * NodeLabelDialog.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.view;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jloda.fx.label.EditLabelDialog;
import jloda.graph.Node;
import phylosketch.commands.ChangeNodeLabelsCommand;

import java.util.Collections;


/**
 * show the node label dialog
 * Daniel Huson, 1.2020
 */
public class NodeLabelDialog {

	public static boolean apply(Stage owner, DrawPane drawPane, Node v) {
		var editLabelDialog = new EditLabelDialog(owner, drawPane.getLabel(v));
		var result = editLabelDialog.showAndWait();
		if (result.isPresent()) {
			var id = v.getId();
			var oldLabel = drawPane.getLabel(v).getText();
			var newLabel = result.get();
			drawPane.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(drawPane, Collections.singletonList(new ChangeNodeLabelsCommand.Data(id, oldLabel, newLabel))));
			return true;
		} else
			return false;
	}

	public static void apply(DrawPane drawPane, double screenX, double screenY, Node v, Runnable runAfter) {
		var oldLabel = drawPane.getLabel(v).getText();
		var textField = new TextField(oldLabel);
		var local = drawPane.screenToLocal(screenX, screenY);
		textField.setTranslateX(local.getX());
		textField.setTranslateY(local.getY());
		drawPane.getChildren().add(textField);
		textField.setOnAction(e -> {
			var id = v.getId();
			var newLabel = textField.getText();
			if (!newLabel.equals(oldLabel)) {
				drawPane.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(drawPane, Collections.singletonList(new ChangeNodeLabelsCommand.Data(id, oldLabel, newLabel))));
			}
			Platform.runLater(() -> drawPane.getChildren().remove(textField));
			if (runAfter != null)
				Platform.runLater(runAfter);
		});
	}
}

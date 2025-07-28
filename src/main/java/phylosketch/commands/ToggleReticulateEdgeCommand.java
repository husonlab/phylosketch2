/*
 * ToggleReticulateEdgeCommand.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.commands;

import javafx.scene.paint.Color;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.ColorUtilsFX;
import jloda.fx.window.MainWindowManager;
import jloda.util.CollectionUtils;
import phylosketch.view.DrawView;

import java.util.ArrayList;

public class ToggleReticulateEdgeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public ToggleReticulateEdgeCommand(DrawView view) {
		super("highlight reticulate edges");

		var allHighlighted = true;
		for (var e : view.getSelectedOrAllEdges()) {
			if (!view.getGraph().isTreeEdge(e) && !view.getGraph().isTransferAcceptorEdge(e)) {
				var path = DrawView.getPath(e);
				var isHighlighted = path.getStroke().equals(Color.DARKORANGE);
				if (!isHighlighted) {
					allHighlighted = false;
				}
			}
		}

		var darkMode = MainWindowManager.isUseDarkTheme();

		var commands = new ArrayList<UndoableRedoableCommand>();

		for (var e : view.getSelectedOrAllEdges()) {
			if (!view.getGraph().isTreeEdge(e) && !view.getGraph().isTransferAcceptorEdge(e)) {
				var color = (Color) DrawView.getPath(e).getStroke();
				var isHighlighted = color.equals(Color.DARKORANGE);
				var id = e.getId();

				if (isHighlighted && allHighlighted) {
					commands.add(UndoableRedoableCommand.create("", () -> {
								var f = view.getGraph().findEdgeById(id);
								var path = DrawView.getPath(f);
								path.setStyle("-fx-stroke: darkorange;");
							}
							, () -> {
								var f = view.getGraph().findEdgeById(id);
								var path = DrawView.getPath(f);
								path.setStyle("-fx-stroke:-fx-text-background-color;");
							}
					));
				}
				if (!isHighlighted && !allHighlighted) {
					String colorString;
					{
						if (color.equals(Color.BLACK) && darkMode || color.equals(Color.WHITE) && !darkMode)
							colorString = "-fx-text-background-color";
						else colorString = ColorUtilsFX.toStringCSS(color);

					}
					commands.add(UndoableRedoableCommand.create("",
							() -> {
								var f = view.getGraph().findEdgeById(id);
								var path = DrawView.getPath(f);
								path.setStyle("-fx-stroke: %s;".formatted(colorString));
							},
							() -> {
								var f = view.getGraph().findEdgeById(id);
								var path = DrawView.getPath(f);
								path.setStyle("-fx-stroke: darkorange;");
							}
					));
				}
			}
		}

		if (commands.isEmpty()) {
			undo = null;
			redo = null;
		} else {
			undo = () -> {
				for (var command : CollectionUtils.reverse(commands)) {
					command.undo();
				}
			};
			redo = () -> {
				for (var command : commands) {
					command.redo();
				}
			};
		}
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}
}

/*
 * EdgeLabelsClearStyleCommand.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.fx.undo.UndoableRedoableCommand;
import phylosketch.view.DrawView;

import java.util.HashMap;
import java.util.Map;

/**
 * clear node label styling
 * Daniel Huson, 11.2024
 */
public class EdgeLabelsClearStyleCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, String> oldMap = new HashMap<>();
	private final Map<Integer, String> newMap = new HashMap<>();

	public EdgeLabelsClearStyleCommand(DrawView view) {
		super("clear labels");
		for (var e : view.getSelectedOrAllEdges()) {
			oldMap.put(e.getId(), DrawView.getLabel(e).getText());
			newMap.put(e.getId(), DrawView.getLabel(e).getRawText());
		}
		if (!oldMap.isEmpty()) {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					view.setLabel(e, oldMap.get(id));
				}
			};
			redo = () -> {
				for (var id : newMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					view.setLabel(e, newMap.get(id));
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

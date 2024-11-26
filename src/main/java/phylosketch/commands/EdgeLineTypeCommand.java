/*
 * NodeSizeCommand.java Copyright (C) 2024 Daniel H. Huson
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
import phylosketch.view.DrawPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * edge stroke command
 * Daniel Huson, 11.2024
 */
public class EdgeLineTypeCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, Double[]> oldMap = new HashMap<>();
	private final Map<Integer, Double[]> newMap = new HashMap<>();


	public EdgeLineTypeCommand(DrawPane view, Double[] strokeDashArray) {
		super("line type");

		for (var e : view.getSelectedOrAllEdges()) {
			var path = view.getPath(e);
			if (path != null) {
				var id = e.getId();
				oldMap.put(id, path.getStrokeDashArray().toArray(new Double[0]));
				newMap.put(id, strokeDashArray);

			}
		}
		if (!newMap.isEmpty()) {
			undo = () -> {
				for (var entry : oldMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					view.getPath(e).getStrokeDashArray().setAll(List.of(entry.getValue()));
				}
			};
			redo = () -> {
				for (var entry : newMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					view.getPath(e).getStrokeDashArray().setAll(List.of(entry.getValue()));
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

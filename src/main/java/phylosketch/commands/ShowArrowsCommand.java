/*
 * ShowArrowsCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.graph.Edge;
import phylosketch.view.DrawPane;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * show arrow heads command
 * Daniel Huson, 2024
 */
public class ShowArrowsCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, Boolean> oldMap = new HashMap<>();
	private final Map<Integer, Boolean> newMap = new HashMap<>();

	public ShowArrowsCommand(DrawPane view, Collection<Edge> edges, boolean show) {
		super("arrows");

		for (var e : edges) {
			var id = e.getId();
			oldMap.put(id, view.isShowArrow(e));
			newMap.put(id, show);
		}

		if (!oldMap.isEmpty()) {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					view.setShowArrow(e, oldMap.get(id));
				}
			};
			redo = () -> {
				for (var id : oldMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					view.setShowArrow(e, newMap.get(id));
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

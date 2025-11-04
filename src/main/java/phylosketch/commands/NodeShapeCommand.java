/*
 * NodeSizeCommand.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.view.NodeShape;

import java.util.HashMap;
import java.util.Map;

/**
 * node shape command
 * Daniel Huson, 11.2025
 */
public class NodeShapeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, NodeShape.Type> oldMap = new HashMap<>();
	private final Map<Integer, NodeShape.Type> newMap = new HashMap<>();


	public NodeShapeCommand(DrawView view, NodeShape.Type newNodeShapeType) {
		super("node shape");

		for (var v : view.getSelectedOrAllNodes()) {
			var shape = DrawView.getShape(v);
			var id = v.getId();
			oldMap.put(id, shape.getType());
			newMap.put(id, newNodeShapeType);
		}

		if (newMap.isEmpty()) {
			undo = null;
			redo = null;
		} else {
			undo = () -> {
				for (var entry : oldMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					DrawView.getShape(v).setType(entry.getValue());
				}
			};
			redo = () -> {
				for (var entry : newMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					DrawView.getShape(v).setType(entry.getValue());
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

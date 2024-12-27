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

import javafx.scene.shape.Circle;
import jloda.fx.undo.UndoableRedoableCommand;
import phylosketch.view.DrawPane;

import java.util.HashMap;
import java.util.Map;

/**
 * node size command
 * Daniel Huson, 11.2024
 */
public class NodeSizeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Double> oldSizeMap = new HashMap<>();
	private final Map<Integer, Double> newSizeMap = new HashMap<>();


	public NodeSizeCommand(DrawPane view, double size) {
		super("node size");

		for (var v : view.getSelectedOrAllNodes()) {
			if (view.getShape(v) instanceof Circle circle) {
				var id = v.getId();
				oldSizeMap.put(id, circle.getRadius());
				newSizeMap.put(id, size);
			}
		}
		if (newSizeMap.isEmpty()) {
			undo = null;
			redo = null;
		} else {
			undo = () -> {
				for (var entry : oldSizeMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (view.getShape(v) instanceof Circle circle) {
						circle.setRadius(entry.getValue());
					}
				}
			};
			redo = () -> {
				for (var entry : newSizeMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (view.getShape(v) instanceof Circle circle) {
						circle.setRadius(entry.getValue());
					}
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

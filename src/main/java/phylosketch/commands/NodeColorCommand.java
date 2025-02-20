/*
 * NodeColorCommand.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.view.DrawView;

import java.util.HashMap;
import java.util.Map;

/**
 * node  color command
 * Daniel Huson, 11.2024
 */
public class NodeColorCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	public enum Which {stroke, fill}

	;

	private final Map<Integer, Color> oldMap = new HashMap<>();
	private final Map<Integer, Color> newMap = new HashMap<>();

	public NodeColorCommand(DrawView view, Which which, Color color) {
		super("color");

		for (var v : view.getSelectedOrAllNodes()) {
			var id = v.getId();
			var shape = DrawView.getShape(v);
			if (shape != null) {
				oldMap.put(id, (Color) (which == Which.stroke ? shape.getStroke() : shape.getFill()));
				newMap.put(id, color);
			}
		}
		if (!newMap.isEmpty()) {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var shape = DrawView.getShape(v);
					if (shape != null) {
						if (which == Which.stroke) {
							shape.setStroke(oldMap.get(id));
						} else if (which == Which.fill) {
							shape.setFill(oldMap.get(id));
						}
					}
				}
			};
			redo = () -> {
				for (var id : newMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var shape = DrawView.getShape(v);
					if (shape != null) {
						if (which == Which.stroke) {
							shape.setStroke(newMap.get(id));
						} else if (which == Which.fill) {
							shape.setFill(newMap.get(id));
						}
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


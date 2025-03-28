/*
 * EdgeColorCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Edge;
import phylosketch.utils.ColorUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * edge  color command
 * edge Huson, 11.2024
 */
public class EdgeColorCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, Color> oldMap = new HashMap<>();
	private final Map<Integer, Color> newMap = new HashMap<>();

	public EdgeColorCommand(DrawView view, Collection<Edge> edges, Color color) {
		super("color");

		for (var e : edges) {
			var id = e.getId();
			var path = DrawView.getPath(e);
			if (path != null) {
				oldMap.put(id, (Color) path.getStroke());
				newMap.put(id, color);
			}
		}
		if (!newMap.isEmpty()) {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					var path = DrawView.getPath(e);
					if (path != null) {
						ColorUtils.setStroke(path, oldMap.get(id), "graph-edge");
					}
				}
			};
			redo = () -> {
				for (var id : newMap.keySet()) {
					var e = view.getGraph().findEdgeById(id);
					var path = DrawView.getPath(e);
					if (path != null) {
						ColorUtils.setStroke(path, newMap.get(id), "graph-edge");
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


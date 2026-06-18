/*
 * SmoothCommand.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * smooth edges command
 * Daniel Huson, 9.2024
 */
public class SmoothCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public SmoothCommand(DrawView view, Collection<Edge> edges) {
		super("smooth");

		if (edges == null || edges.isEmpty()) {
			return;
		}
		for (var e : edges) {
			if (e.getData() instanceof EdgePath path) {
				var id = e.getId();
				oldEdgeMap.put(id, path.copy());
				var points = PathSmoother.apply(PathUtils.extractPoints(path), 50);
				var newPath = new EdgePath();
				newPath.setFreeform(points);
				newEdgeMap.put(id, newPath);
			}
		}

		undo = () -> {
			oldEdgeMap.forEach((key, value) -> {
				var e = view.getGraph().findEdgeById(key);
				var path = DrawView.getPath(e);
				path.set(value.getElements(), value.getType());
			});
		};
		redo = () -> {
			newEdgeMap.forEach((key, value) -> {
				var e = view.getGraph().findEdgeById(key);
				var path = DrawView.getPath(e);
				path.set(value.getElements(), value.getType());
			});
		};

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

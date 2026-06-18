/*
 * StraightenCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.phylo.PhyloTree;
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * straighten edges command
 * Daniel Huson, 9.2024
 */
public class StraightenCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public StraightenCommand(PhyloTree graph, Collection<Edge> edges) {
		super("straighten");

		for (var e : edges) {
			if (e.getData() instanceof EdgePath path) {
				var id = e.getId();
				oldEdgeMap.put(id, path.copy());
				var newPath = new EdgePath();
				newPath.setStraight(PathUtils.getCoordinates(path.getElements().get(0)), PathUtils.getCoordinates(path.getElements().get(path.getElements().size() - 1)));
				newEdgeMap.put(id, newPath);
			}
		}

		undo = () -> {
			for (var entry : oldEdgeMap.entrySet()) {
				var e = graph.findEdgeById(entry.getKey());
				var path = DrawView.getPath(e);
				path.set(entry.getValue().getElements(), entry.getValue().getType());
			}
		};
		redo = () -> {
			for (var entry : newEdgeMap.entrySet()) {
				var e = graph.findEdgeById(entry.getKey());
				var path = DrawView.getPath(e);
				path.set(entry.getValue().getElements(), entry.getValue().getType());
			}
		};
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

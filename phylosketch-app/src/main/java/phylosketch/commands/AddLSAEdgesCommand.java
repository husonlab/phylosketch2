/*
 * AddLSAEdgesCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.util.Pair;
import phylosketch.paths.EdgePath;
import phylosketch.view.DrawView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * add LSA edges command
 * Daniel Huson, 11.2024
 */
public class AddLSAEdgesCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Pair<Node, Node>, Integer> newMap = new HashMap<>();

	public AddLSAEdgesCommand(DrawView view) {
		super("add LSA edges");

		var graph = view.getGraph();

		var roots = graph.nodeStream().filter(v -> v.getInDegree() == 0).toList();
		if (roots.size() != 1)
			return;

		graph.setRoot(roots.get(0));

		var activeReticulations = view.getSelectedOrAllNodes().stream().filter(v -> v.getInDegree() > 1).collect(Collectors.toSet());

		if (activeReticulations.isEmpty())
			return;

		try (NodeArray<Node> reticulation2LSA = graph.newNodeArray()) {
			LSAUtils.computeReticulation2LSA(graph, reticulation2LSA);
			for (var target : reticulation2LSA.keySet()) {
				if (activeReticulations.contains(target)) {
					var source = reticulation2LSA.get(target);
					if (source != target && !source.isChild(target)) {
						newMap.put(new Pair<>(source, target), -1);
					}
				}
			}
		}

		undo = () -> view.deleteEdge(newMap.values().stream().filter(id -> id != -1).map(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toArray(Edge[]::new));

		redo = () -> {
			for (var entry : newMap.entrySet()) {
				var v = entry.getKey().getFirst();
				var w = entry.getKey().getSecond();
				var path = new EdgePath();
				path.setStraight(DrawView.getPoint(v), DrawView.getPoint(w));
				var e = view.createEdge(v, w, path, entry.getValue());
				entry.setValue(e.getId());
				path.applyCss();
				path.setStroke(Color.LIGHTGREEN);
			}
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

/*
 * InduceCommand.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import phylosketch.view.DrawView;

import java.util.*;

public class InduceCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private CompositeCommand command;

	public InduceCommand(DrawView view, Collection<Node> nodes) {
		super("induce");

		var ids = nodes.stream().mapToInt(v -> v.getId()).toArray();

		if (ids.length > 0) {
			undo = () -> {
				if (command != null && command.isUndoable())
					command.undo();
			};
			redo = () -> {
				var initialNodes = Arrays.stream(ids).mapToObj(id -> view.getGraph().findNodeById(id)).toList();
				var allNodes = new HashSet<Node>();
				var allEdges = new HashSet<Edge>();
				for (var v : initialNodes) {
					allAboveRec(v, allNodes, allEdges);
				}
				if (allNodes.size() < view.getGraph().getNumberOfNodes()) {
					command = new CompositeCommand(getName());
					var toDelete = view.getGraph().nodeStream().filter(v -> !allNodes.contains(v)).toList();
					var deleteCommand = new DeleteCommand(view, toDelete, List.of());
					if (deleteCommand.isRedoable()) {
						command.add(deleteCommand);
						deleteCommand.redo();
						var removeCommand = new RemoveThruNodesCommand(view, allNodes);
						if (removeCommand.isRedoable()) {
							removeCommand.redo();
							command.add(removeCommand);
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

	private void allAboveRec(Node v, Set<Node> nodes, Set<Edge> edges) {
		if (!nodes.contains(v)) {
			nodes.add(v);
			for (var e : v.inEdges()) {
				edges.add(e);
				var w = e.getSource();
				allAboveRec(w, nodes, edges);
			}
		}
	}
}

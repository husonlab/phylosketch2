/*
 *  DuplicateCommand.java Copyright (C) 2026 Daniel H. Huson
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
 */

package phylosketch.commands;

import javafx.beans.property.BooleanProperty;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;
import phylosketch.view.NodeShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class DuplicateCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, Integer> oldNewNodeIds = new HashMap<>();
	private final Map<Integer, Integer> oldNewEdgeIds = new HashMap<>();

	public DuplicateCommand(DrawView view, BooleanProperty resizeMode) {
		super("duplicate");

		var graph = view.getGraph();

		var hasSelectedNodes = view.getNodeSelection().size() > 0;
		var hasSelectedEdges = view.getEdgeSelection().size() > 0;

		var nodes = new HashSet<>(view.getSelectedOrAllNodes());
		var selectedEdges = new HashSet<>(view.getEdgeSelection().getSelectedItems());
		var edges = graph.edgeStream().filter(e -> selectedEdges.contains(e)
												   && nodes.contains(e.getSource()) && nodes.contains(e.getTarget()))
				.collect(Collectors.toSet());

		if (nodes.isEmpty())
			return;

		for (var v : nodes) {
			oldNewNodeIds.put(v.getId(), -1);
		}
		for (var e : edges) {
			oldNewEdgeIds.put(e.getId(), -1);
		}

		undo = () -> {
			resizeMode.set(false);
			view.deleteNode(oldNewNodeIds.values().stream().map(graph::findNodeById).toArray(Node[]::new));
			if (hasSelectedNodes) {
				view.getNodeSelection().clearSelection();
				view.getNodeSelection().selectAll(oldNewNodeIds.keySet().stream().map(graph::findNodeById).toList());
			}
			if (hasSelectedEdges) {
				view.getEdgeSelection().clearSelection();
				view.getEdgeSelection().selectAll(oldNewEdgeIds.keySet().stream().map(graph::findEdgeById).toList());
			}
		};
		redo = () -> {
			view.getNodeSelection().clearSelection();
			view.getEdgeSelection().clearSelection();

			var newNodes = new ArrayList<Node>();
			for (var entry : oldNewNodeIds.entrySet()) {
				var oldId = entry.getKey();
				var newId = entry.getValue();

				var v = graph.findNodeById(oldId);
				var shape = (DrawView.getShape(v) != null ? new NodeShape(DrawView.getShape(v)) : null);
				var label = DrawView.getLabel(v).getText();

				var w = view.createNode(shape, label, newId);
				view.setLocation(w, view.getLocation(v));
				if (newId == -1) {
					entry.setValue(w.getId());
				}
				newNodes.add(w);
			}
			var newEdges = new ArrayList<Edge>();

			for (var entry : oldNewEdgeIds.entrySet()) {
				var oldId = entry.getKey();
				var newId = entry.getValue();

				var e = graph.findEdgeById(oldId);

				var v = graph.findNodeById(oldNewNodeIds.get(e.getSource().getId()));
				var w = graph.findNodeById(oldNewNodeIds.get(e.getTarget().getId()));

				var f = view.createEdge(v, w, PathUtils.copy(DrawView.getPath(e)), newId);
				if (newId == -1) {
					entry.setValue(f.getId());
				}
				newEdges.add(f);
			}

			var moveCommand = new MoveNodesEdgesCommand(view, newNodes, () -> {
				view.getNodeSelection().selectAll(newNodes);
				view.getEdgeSelection().selectAll(newEdges);
				resizeMode.set(false);
			});
			moveCommand.moveNodesAndEdges(20, 20);
			if (moveCommand.isRedoable())
				moveCommand.redo();
		};
	}

	@Override
	public void undo() {
		if (undo != null)
			undo.run();
	}

	@Override
	public void redo() {
		if (redo != null)
			redo.run();
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}
}

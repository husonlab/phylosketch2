/*
 * MoveNodesEdgesCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.geometry.Point2D;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathTransforms;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * move nodes command
 * Daniel Huson, 2024
 */
public class MoveNodesEdgesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final DrawView view;
	private final Map<Integer, Point2D> oldNodeMap = new HashMap<>();
	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> newNodeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public MoveNodesEdgesCommand(DrawView view, Collection<Node> nodes, Runnable runOnUpdated) {
		super("move");
		this.view = view;

		for (var v : nodes) {
			var location = view.getLocation(v);
			oldNodeMap.put(v.getId(), location);
		}

		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var path = DrawView.getPath(e);
				oldEdgeMap.put(e.getId(), path.copy());
			}
		}

		undo = () -> {
			for (var id : oldNodeMap.keySet()) {
				view.setLocation(view.getGraph().findNodeById(id), oldNodeMap.get(id));
			}
			for (var entry : oldEdgeMap.entrySet()) {
				var e = view.getGraph().findEdgeById(entry.getKey());
				var path = DrawView.getPath(e);
				path.set(entry.getValue().getElements(), entry.getValue().getType());
			}
			if (runOnUpdated != null) {
				runOnUpdated.run();
			}
		};

		redo = () -> {
			for (var id : newNodeMap.keySet()) {
				view.setLocation(view.getGraph().findNodeById(id), newNodeMap.get(id));
			}
			for (var entry : newEdgeMap.entrySet()) {
				var e = view.getGraph().findEdgeById(entry.getKey());
				var path = DrawView.getPath(e);
				path.set(entry.getValue().getElements(), entry.getValue().getType());
			}
			if (runOnUpdated != null) {
				runOnUpdated.run();
			}
		};
	}

	@Override
	public boolean isUndoable() {
		return undo != null && !oldNodeMap.isEmpty();
	}

	@Override
	public boolean isRedoable() {
		return redo != null && !newNodeMap.isEmpty();
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	public void moveNodesAndEdges(double dx, double dy) {
		newNodeMap.clear();
		for (var id : oldNodeMap.keySet()) {
			var v = view.getGraph().findNodeById(id);
			var point = view.getLocation(v);
			var newPoint = point.add(dx, dy);
			newNodeMap.put(v.getId(), newPoint);
			view.setLocation(v, point.add(dx, dy));
		}
		moveEdges(dx, dy);
	}

	public void moveEdges(double dx, double dy) {
		newEdgeMap.clear();
		var nodeIds = oldNodeMap.keySet();
		for (var e : view.getGraph().edges()) {
			if (nodeIds.contains(e.getSource().getId()) || nodeIds.contains(e.getTarget().getId())) {
				var path = DrawView.getPath(e);
				if (nodeIds.contains(e.getSource().getId()) && nodeIds.contains(e.getTarget().getId())) {
					var elements = PathTransforms.translate(path, dx, dy).getElements();
					newEdgeMap.put(e.getId(), path.copy());
					path.getElements().setAll(elements);
				} else if (nodeIds.contains(e.getSource().getId())) {
					var index = 0;
					var tmp = path.copyToFreeform();
					PathReshape.apply(tmp, index, dx, dy);
					var elements = PathNormalize.apply(tmp, 2, 5);
					path.set(elements, EdgePath.Type.Freeform);
					newEdgeMap.put(e.getId(), path);
				} else if (nodeIds.contains(e.getTarget().getId())) {
					var index = path.getElements().size() - 1;
					var tmp = path.copyToFreeform();
					PathReshape.apply(tmp, index, dx, dy);
					var elements = PathNormalize.apply(tmp, 2, 5);
					path.set(elements, EdgePath.Type.Freeform);
					newEdgeMap.put(e.getId(), path);
				}
			}
		}
	}
}

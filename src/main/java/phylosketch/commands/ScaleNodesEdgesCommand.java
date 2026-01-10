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
import phylosketch.paths.*;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * scale nodes and edges command
 * Daniel Huson, 2024
 */
public class ScaleNodesEdgesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final DrawView view;
	private final Map<Integer, Point2D> oldNodeMap = new HashMap<>();
	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> newNodeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public ScaleNodesEdgesCommand(DrawView view, Collection<Node> nodes, Runnable runOnUpdated) {
		super("scale");
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

	public void scaleNodesAndEdges(double dx, double dy) {
		newNodeMap.clear();

		var nodes = oldNodeMap.keySet().stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());

		if (dx != 0 || dy != 0) {
			var oldBBox = BBox.compute(nodes);
			var newBBox = new BBox(oldBBox.xMin(), oldBBox.yMin(), oldBBox.xMax() + dx, oldBBox.yMax() + dy);

			var xFactor = (oldBBox.width() > 0 ? newBBox.width() / oldBBox.width() : 1.0);
			var yFactor = (oldBBox.height() > 0 ? newBBox.height() / oldBBox.height() : 1.0);

			for (var v : nodes) {
				var x = DrawView.getX(v);
				var y = DrawView.getY(v);
				var newX = oldBBox.width() <= 0 ? x : (x - oldBBox.xMin()) * xFactor + newBBox.xMin();
				var newY = oldBBox.height() <= 0 ? y : (y - oldBBox.yMax()) * yFactor + newBBox.yMax();
				var newPoint = new Point2D(newX, newY);
				view.setLocation(v, newPoint);
				newNodeMap.put(v.getId(), newPoint);
			}
			rescaleEdges(oldBBox, newBBox);
		}
	}

	private void rescaleEdges(BBox oldBBox, BBox newBBox) {
		newEdgeMap.clear();
		var nodeIds = oldNodeMap.keySet();
		for (var e : view.getGraph().edges()) {
			var eId = e.getId();
			var source = e.getSource();
			var sourceId = source.getId();
			var target = e.getTarget();
			var targetId = target.getId();

			if (nodeIds.contains(sourceId) || nodeIds.contains(targetId)) {
				var path = DrawView.getPath(e);

				if (nodeIds.contains(sourceId) && nodeIds.contains(targetId)) {
					var elements = PathTransforms.fitToBounds(path, oldBBox, newBBox).getElements();
					path.getElements().setAll(elements);
				} else if (nodeIds.contains(sourceId)) {
					var diff = newNodeMap.get(sourceId).subtract(oldNodeMap.get(sourceId));
					var tmp = oldEdgeMap.get(eId).copyToFreeform();
					var index = 0;
					PathReshape.apply(tmp, index, diff.getX(), diff.getY());
					var elements = PathNormalize.apply(tmp, 2, 5);
					path.set(elements, EdgePath.Type.Freeform);
				} else if (nodeIds.contains(targetId)) {
					var diff = newNodeMap.get(targetId).subtract(oldNodeMap.get(targetId));
					var tmp = oldEdgeMap.get(eId).copyToFreeform();
					var index = tmp.getElements().size() - 1;
					PathReshape.apply(tmp, index, diff.getX(), diff.getY());
					var elements = PathNormalize.apply(tmp, 2, 5);
					path.set(elements, EdgePath.Type.Freeform);
				}
				newEdgeMap.put(eId, path);
			}
		}
	}
}

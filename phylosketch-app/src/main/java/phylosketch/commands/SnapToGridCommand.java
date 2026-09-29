/*
 * SnapToGridCommand.java Copyright (C) 2026 Daniel H. Huson
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
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;
import phylosketch.view.NodeGrid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * snaps all nodes to the grid and switches the grid on, so that dragged nodes then move in grid steps
 * <p>
 * The grid spacing is chosen for the current drawing, see {@link NodeGrid#computeSpacing}, and no two nodes go onto
 * the same grid point. The edges follow their nodes, keeping their type where they can, see
 * {@link PathReshape#moveEnds}, so that a rectangular drawing stays rectangular. Undoing this also switches the grid
 * off.
 * <p>
 * Daniel Huson, 9.2026
 */
public class SnapToGridCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> oldNodeMap = new HashMap<>();
	private final Map<Integer, Point2D> newNodeMap = new HashMap<>();
	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	/**
	 * snaps all nodes to the grid
	 *
	 * @param view the view
	 * @param grid the grid, whose spacing is set when this is done
	 */
	public SnapToGridCommand(DrawView view, NodeGrid grid) {
		super("snap to grid");
		var graph = view.getGraph();

		var oldPoints = new LinkedHashMap<Node, Point2D>(); // in node order, which breaks ties
		for (var v : graph.nodes())
			oldPoints.put(v, view.getLocation(v));
		if (oldPoints.isEmpty()) {
			undo = redo = null;
			return;
		}

		var spacing = NodeGrid.computeSpacing(graph, oldPoints::get);
		var newPoints = NodeGrid.snapAll(oldPoints, spacing);
		for (var v : oldPoints.keySet()) {
			oldNodeMap.put(v.getId(), oldPoints.get(v));
			newNodeMap.put(v.getId(), newPoints.get(v));
		}

		for (var e : graph.edges()) {
			var path = DrawView.getPath(e);
			if (path != null && !path.getElements().isEmpty()) {
				var sourceDelta = newPoints.get(e.getSource()).subtract(oldPoints.get(e.getSource()));
				var targetDelta = newPoints.get(e.getTarget()).subtract(oldPoints.get(e.getTarget()));
				// the path of a captured edge may run from target to source
				var first = PathUtils.getCoordinates(path.getElements().get(0));
				var sourceFirst = (first.distance(oldPoints.get(e.getSource())) <= first.distance(oldPoints.get(e.getTarget())));
				oldEdgeMap.put(e.getId(), path.copy());
				newEdgeMap.put(e.getId(), PathReshape.moveEnds(path, sourceFirst ? sourceDelta : targetDelta, sourceFirst ? targetDelta : sourceDelta));
			}
		}

		undo = () -> {
			grid.setSnap(false);
			setPositions(view, oldNodeMap, oldEdgeMap);
		};
		redo = () -> {
			grid.setSpacing(spacing);
			grid.setSnap(true);
			setPositions(view, newNodeMap, newEdgeMap);
		};
	}

	private static void setPositions(DrawView view, Map<Integer, Point2D> nodeMap, Map<Integer, EdgePath> edgeMap) {
		for (var entry : nodeMap.entrySet()) {
			view.setLocation(view.getGraph().findNodeById(entry.getKey()), entry.getValue());
		}
		for (var entry : edgeMap.entrySet()) {
			var path = DrawView.getPath(view.getGraph().findEdgeById(entry.getKey()));
			path.set(entry.getValue().getElements(), entry.getValue().getType());
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

/*
 * RectangularCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Edge;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.SetUtils;
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * rectangular edges command
 * Daniel Huson, 9.2024
 */
public class RectangularCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public RectangularCommand(DrawView view, Collection<Edge> edges) {
		super("rectangular");

		try (NodeArray<RootPosition> nodeRootLocationMap = view.getGraph().newNodeArray()) {
			var nodes = edges.stream().map(Edge::nodes).flatMap(Collection::stream).collect(Collectors.toSet());
			for (var component : ConnectedComponents.components(view.getGraph())) {
				if (SetUtils.intersect(nodes, component)) {
					var rootLocation = RootPosition.compute(component);
					for (var v : component) {
						nodeRootLocationMap.put(v, rootLocation);
					}
				}
			}

			for (var e : edges) {
				if (e.getData() instanceof EdgePath path) {
					var id = e.getId();
					oldEdgeMap.put(id, path.copy());

					var first = PathUtils.getCoordinates(path.getElements().get(0));
					var last = PathUtils.getCoordinates(path.getElements().get(path.getElements().size() - 1));

					var points = switch (nodeRootLocationMap.get(e.getSource()).side()) {
						case Top, Bottom -> List.of(first, new Point2D(last.getX(), first.getY()), last);
						case Left, Right, Center -> List.of(first, new Point2D(first.getX(), last.getY()), last);
					};
					var newPath = new EdgePath();
					newPath.setRectangular(points.get(0), points.get(1), points.get(2));
					newEdgeMap.put(id, newPath);
				}
			}
		}


		undo = () -> {
			for (var entry : oldEdgeMap.entrySet()) {
				var e = view.getGraph().findEdgeById(entry.getKey());
				var path = DrawView.getPath(e);
				path.set(entry.getValue().getElements(), entry.getValue().getType());
			}
		};
		redo = () -> {
			for (var entry : newEdgeMap.entrySet()) {
				var e = view.getGraph().findEdgeById(entry.getKey());
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

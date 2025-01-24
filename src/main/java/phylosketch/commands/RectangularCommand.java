/*
 * SmoothCommand.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.scene.shape.Path;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathUtils;
import phylosketch.view.RootLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * rectangular edges command
 * Daniel Huson, 9.2024
 */
public class RectangularCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final int[] edgeIds;
	private final Map<Integer, List<Point2D>> idOldPointsMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> idNewPointsMap = new HashMap<>();

	public RectangularCommand(Graph graph, Collection<Edge> edges) {
		super("rectangular");
		edgeIds = edges.stream().mapToInt(e -> e.getId()).toArray();

		try (NodeArray<RootLocation> nodeRootLocationMap = graph.newNodeArray()) {
			for (var component : ConnectedComponents.components(graph)) {
				var rootLocation = RootLocation.compute(component);
				for (var v : component) {
					nodeRootLocationMap.put(v, rootLocation);
				}
			}

			for (var e : edges) {
				if (e.getData() instanceof Path path) {
					idOldPointsMap.put(e.getId(), PathUtils.extractPoints(path));

					var first = PathUtils.getCoordinates(path.getElements().get(0));
					var last = PathUtils.getCoordinates(path.getElements().get(path.getElements().size() - 1));

					var points = switch (nodeRootLocationMap.get(e.getSource())) {
						case Top, Bottom -> List.of(first, new Point2D(last.getX(), first.getY()), last);
						case Left, Right, Center -> List.of(first, new Point2D(first.getX(), last.getY()), last);
					};

					idNewPointsMap.put(e.getId(), PathNormalize.refine(points, 5));
				}
			}
		}

		undo = () -> {
			for (var id : edgeIds) {
				var e = graph.findEdgeById(id);
				if (e.getData() instanceof Path path) {
					path.getElements().setAll(PathUtils.toPathElements(idOldPointsMap.get(id)));
				}
			}
		};
		redo = () -> {
			for (var id : edgeIds) {
				var e = graph.findEdgeById(id);
				if (e.getData() instanceof Path path) {
					path.getElements().setAll(PathUtils.toPathElements(idNewPointsMap.get(id)));
				}
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

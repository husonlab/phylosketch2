/*
 * QuadraticCurveCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.SetUtils;
import phylosketch.paths.PathUtils;
import phylosketch.utils.QuadraticCurve;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * quadratic curve command
 * Daniel Huson, 11.2024
 */
public class QuadraticCurveCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final int[] edgeIds;
	private final Map<Integer, List<Point2D>> idOldPointsMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> idNewPointsMap = new HashMap<>();

	public QuadraticCurveCommand(DrawView view, Collection<Edge> edges) {
		super("quadratic curve");
		var graph = view.getGraph();
		edgeIds = edges.stream().mapToInt(e -> e.getId()).toArray();

		try (NodeArray<RootPosition> nodeRootLocationMap = graph.newNodeArray()) {
			{
				var nodes = edges.stream().map(Edge::nodes).flatMap(Collection::stream).collect(Collectors.toSet());
				for (var component : ConnectedComponents.components(graph)) {
					if (SetUtils.intersect(nodes, component)) {
						var rootLocation = RootPosition.compute(component);
						for (var v : component) {
							nodeRootLocationMap.put(v, rootLocation);
						}
					}
				}
			}

			for (var e : edges) {
				if (e.getData() instanceof Path path) {
					idOldPointsMap.put(e.getId(), PathUtils.extractPoints(path));

					var first = PathUtils.getCoordinates(path.getElements().get(0));
					var last = PathUtils.getCoordinates(path.getElements().get(path.getElements().size() - 1));

					Point2D control;
					if (graph.isTransferEdge(e) || (graph.isReticulateEdge(e) && nodeRootLocationMap.get(e.getSource()).side() == RootPosition.Side.Center)) {
						control = QuadraticCurve.computeControlForBowEdge(first, last);
					} else {
						control = switch (nodeRootLocationMap.get(e.getSource()).side()) {
							case Top, Bottom -> new Point2D(last.getX(), first.getY());
							case Left, Right -> new Point2D(first.getX(), last.getY());
							case Center -> new Point2D(first.getX(), last.getY());
						};
					}
					idNewPointsMap.put(e.getId(), QuadraticCurve.apply(first, control, last, 5));
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

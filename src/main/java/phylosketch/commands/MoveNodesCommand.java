/*
 * MoveNodesCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Graph;
import jloda.graph.Node;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;
import phylosketch.view.DrawPane;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * move nodes command
 * Daniel Huson, 2024
 */
public class MoveNodesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Set<Integer> nodeIds;

	public MoveNodesCommand(DrawPane view, Collection<Node> nodes, double dx, double dy) {
		super("move");

		nodeIds = nodes.stream().map(v -> v.getId()).collect(Collectors.toSet());

		undo = () -> {
			var set = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
			moveNodesAndEdges(view.getGraph(), set, -dx, -dy, true);
		};
		redo = () -> {
			var set = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
			moveNodesAndEdges(view.getGraph(), set, dx, dy, true);

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

	public static void moveNodesAndEdges(Graph graph, Collection<Node> nodes, double dx, double dy, boolean normalizePaths) {
		for (var v : nodes) {
			if (v.getData() instanceof Shape shape) {
				shape.setTranslateX(shape.getTranslateX() + dx);
				shape.setTranslateY(shape.getTranslateY() + dy);
			}
		}
		moveEdges(graph, nodes, dx, dy, normalizePaths);
	}

	public static void moveEdges(Graph graph, Collection<Node> nodes, double dx, double dy, boolean normalizePaths) {
		for (var e : graph.edges()) {
			if (e.getData() instanceof Path path) {
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					PathReshape.apply(path, dx, dy);
					if (normalizePaths) {
						path.getElements().setAll(PathNormalize.apply(path, 2, 5));
					}
				} else if (nodes.contains(e.getSource()) && !nodes.contains(e.getTarget())) {
					var index = 0;
					PathReshape.apply(path, index, dx, dy);
					if (normalizePaths) {
						path.getElements().setAll(PathNormalize.apply(path, 2, 5));
					}
				} else if (!nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					var index = path.getElements().size() - 1;
					PathReshape.apply(path, index, dx, dy);
					if (normalizePaths) {
						path.getElements().setAll(PathNormalize.apply(path, 2, 5));
					}
				}
			}
		}
	}

}

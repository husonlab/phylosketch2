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

import javafx.application.Platform;
import javafx.scene.paint.Color;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.util.Basic;
import phylosketch.embed.LSATreeUtilities;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * add LSA edges command
 * Daniel Huson, 11.2024
 */
public class AddLSAEdgesCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final Set<Integer> newEdgeIds = new HashSet<>();

	public AddLSAEdgesCommand(DrawPane view) {
		super("add LSA edges");

		var graph = view.getGraph();

		var roots = graph.nodeStream().filter(v -> v.getInDegree() == 0).toList();
		if (roots.size() != 1)
			return;

		graph.setRoot(roots.get(0));

		undo = () -> {
			view.deleteEdge(newEdgeIds.stream().map(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toArray(Edge[]::new));
		};
		redo = () -> {
			try (NodeArray<Node> reticulation2LSA = graph.newNodeArray()) {
				LSATreeUtilities.computeReticulation2LSA(graph, reticulation2LSA, null);
				for (var target : reticulation2LSA.keySet()) {
					if (target.getInDegree() > 1) {
						var source = reticulation2LSA.get(target);
						var path = PathUtils.createPath(List.of(view.getPoint(source), view.getPoint(target)), true);

						if (source != target && !target.isChild(source)) {
							var e = view.createEdge(source, target, path);
							newEdgeIds.add(e.getId());
							Platform.runLater(() -> view.getPath(e).setStroke(Color.ORANGE));
						}
					}
				}
			} catch (Exception e) {
				Basic.caught(e);
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

/*
 * RerootCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.HashSet;
import java.util.Set;

/**
 * reroot command
 * Daniel Huson, 2024
 */
public class RerootCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private Integer oldRootId;
	private final Integer oldNodeId;
	private final Integer oldEdgeId;
	private final boolean oldEdgeArrow;
	private final Integer oldSourceId;
	private final Integer oldTargetId;
	private final Path oldEdgePath;

	private int newEdgeAtSourceId = -1;
	private int newEdgeAtTargetId = -1;
	private Integer newNodeId = -1;
	private Integer newRootId = -1;

	private final Set<Integer> edgesToChange = new HashSet<>();

	/**
	 * constructor. Either v or e must be non-null
	 *
	 * @param view the view
	 * @param v    the new root node
	 * @param e    the new edge node
	 */
	public RerootCommand(DrawPane view, Node v, Edge e) {
		super("reroot");

		if ((v != null) == (e != null))
			throw new IllegalArgumentException("either v or e must be non-null");

		if (e != null) {
			v = e.getSource();
			oldNodeId = -1;
			oldEdgeId = e.getId();
			oldEdgeArrow = view.getEdgeArrowMap().containsKey(e);
			oldSourceId = e.getSource().getId();
			oldTargetId = e.getTarget().getId();
			oldEdgePath = (Path) e.getData();
			// newRootId will be set in redo()
		} else {
			oldNodeId = v.getId();
			oldEdgeId = -1;
			oldEdgeArrow = false;
			oldSourceId = -1;
			oldTargetId = -1;
			oldEdgePath = null;
			newRootId = v.getId();
		}

		var graph = view.getGraph();
		if (v.getInDegree() > 0 && isOkAsRoot(v)) {
			{
				var f = v.getFirstInEdge();
				while (f != null) {
					edgesToChange.add(f.getId());
					if (f.getSource().getInDegree() == 1)
						f = f.getSource().getFirstInEdge();
					else {
						if (f.getTarget() == graph.getRoot()) {
							oldRootId = f.getTarget().getId();
						}
						f = null;
					}
				}
			}

			undo = () -> {
				view.getNodeSelection().clearSelection();
				view.getEdgeSelection().clearSelection();

				for (var id : edgesToChange) {
					var f = graph.findEdgeById(id);
					if (f.getData() instanceof Path path) {
						PathUtils.reverse(path);
					}
					f.reverse();
				}
				if (newNodeId != -1)
					view.deleteNode(graph.findNodeById(newNodeId));
				if (oldEdgeId != -1) {
					var f = view.createEdge(graph.findNodeById(oldSourceId), graph.findNodeById(oldTargetId), oldEdgePath, oldEdgeId);
					view.setShowArrow(f, true);
					view.getEdgeSelection().select(f);
				} else {
					view.getNodeSelection().select(graph.findNodeById(oldNodeId));
				}
				if (oldRootId != null)
					graph.setRoot(graph.findNodeById(oldRootId));

			};

			redo = () -> {
				view.getNodeSelection().clearSelection();
				view.getEdgeSelection().clearSelection();

				for (var id : edgesToChange) {
					var f = graph.findEdgeById(id);
					if (f.getData() instanceof Path path) {
						PathUtils.reverse(path);
						view.getEdgeSelection().select(f);
						view.getNodeSelection().select(f.getSource());
						view.getNodeSelection().select(f.getTarget());
					}
					f.reverse();
				}
				if (oldEdgeId != -1) {
					var index = oldEdgePath.getElements().size() / 2;
					var edgeHit = new CreateEdgeCommand.EdgeHit(graph.findEdgeById(oldEdgeId), oldEdgePath, index);
					var parts = edgeHit.splitPath();
					var location = PathUtils.getCoordinates(parts.getSecond().getElements().get(0));
					var w = view.createNode(location, newNodeId);
					newNodeId = w.getId();

					var firstPath = PathUtils.createPath(CollectionUtils.reverse(PathUtils.extractPoints(parts.getFirst())), false);
					var e1 = view.createEdge(w, graph.findNodeById(oldSourceId), firstPath, newEdgeAtSourceId);
					newEdgeAtSourceId = e1.getId();
					var e2 = view.createEdge(w, graph.findNodeById(oldTargetId), parts.getSecond(), newEdgeAtTargetId);
					newEdgeAtTargetId = e2.getId();

					view.getNodeSelection().select(w);
					view.getEdgeSelection().select(e1);
					view.getNodeSelection().select(e1.getTarget());
					view.getEdgeSelection().select(e2);
					view.getNodeSelection().select(e2.getTarget());

					if (oldEdgeArrow) {
						view.setShowArrow(e1, true);
						view.setShowArrow(e2, true);
					}
					view.deleteEdge(graph.findEdgeById(oldEdgeId));
					if (oldRootId != null) {
						newRootId = newNodeId;
					}
				}
				if (newRootId != -1)
					graph.setRoot(graph.findNodeById(newRootId));
			};
		} else {
			undo = null;
			redo = null;
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

	public static boolean isOkAsRoot(Node v) {
		while (v != null) {
			if (v.getInDegree() == 1)
				v = v.getParent();
			else return v.getInDegree() == 0;
		}
		return false;
	}
}

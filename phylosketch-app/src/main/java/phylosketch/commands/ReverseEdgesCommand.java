/*
 * ReverseEdgesCommand.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import phylosketch.paths.EdgePath;
import phylosketch.view.DrawView;

import java.util.*;

public class ReverseEdgesCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private EdgePath oldEdgePath;
	private EdgePath newEdgePath;

	public ReverseEdgesCommand(DrawView view, Edge e) {
		super("reverse");
		try (var used = view.getGraph().newNodeSet()) {
			if (canReachDirectedRec(e.getSource(), e.getTarget(), e, Collections.emptySet(), used))
				return;
		}

		var path = DrawView.getPath(e);
		oldEdgePath = path.copy();
		newEdgePath = path.reverse();

		var showArrow = view.isShowArrow(e);
		var eId = e.getId();

		undo = () -> {
			var f = view.getGraph().findEdgeById(eId);
			if (showArrow)
				view.setShowArrow(f, false);
			f.reverse();
			var fPath = DrawView.getPath(f);
			fPath.set(oldEdgePath.getElements(), oldEdgePath.getType());
			view.getEdgeSelection().select(f);

			if (showArrow)
				view.setShowArrow(f, true);

		};
		redo = () -> {
			var f = view.getGraph().findEdgeById(eId);
			if (showArrow)
				view.setShowArrow(f, false);
			f.reverse();
			view.getEdgeSelection().select(f);
			var fPath = DrawView.getPath(f);
			fPath.set(newEdgePath.getElements(), newEdgePath.getType());
			if (showArrow)
				view.setShowArrow(f, true);
		};
	}

	public ReverseEdgesCommand(DrawView view, Collection<Edge> edges) {
		super("reverse");

		var edgeIds = edges.stream().mapToInt(e -> e.getId()).toArray();

		var commands = new CompositeCommand("reverse");

		undo = commands::undo;

		redo = () -> {
			for (var id : edgeIds) {
				var e = view.getGraph().findEdgeById(id);
				var reverseCommand = new ReverseEdgesCommand(view, e);
				if (reverseCommand.isRedoable()) {
					reverseCommand.redo();
					commands.add(reverseCommand);
				}
			}
		};
	}

	public ReverseEdgesCommand(DrawView view, Node rootNode) {
		super("reverse");
		var toFlip = new HashSet<Edge>();
		while (true) {
			var additional = getFlippable(view.getGraph(), rootNode, toFlip);
			if (additional.isEmpty())
				break;
			else
				toFlip.addAll(additional);
		}
		if (!toFlip.isEmpty()) {
			var command = new ReverseEdgesCommand(view, toFlip);
			undo = command::undo;
			redo = command::redo;
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

	public static List<Edge> getFlippable(PhyloTree graph, Node root, Set<Edge> flipped) {
		try (var reachable = graph.newNodeSet()) {
			canReachDirectedRec(root, null, null, flipped, reachable);
			var result = new ArrayList<Edge>();
			try (var used = graph.newNodeSet()) {
				var edgesPointingIn = graph.edgeStream()
						.filter(e ->
								(!flipped.contains(e) && !reachable.contains(e.getSource()) && reachable.contains(e.getTarget()))
								|| (flipped.contains(e) && reachable.contains(e.getSource()) && !reachable.contains(e.getTarget()))
						).toList();
				for (var e : edgesPointingIn) {
					var v = (flipped.contains(e) ? e.getTarget() : e.getSource());
					var w = (flipped.contains(e) ? e.getSource() : e.getTarget());
					if (!canReachDirectedRec(v, w, e, flipped, used)) {
						result.add(e);
					}
					used.clear();
				}
			}
			return result;
		}
	}

	private static boolean canReachDirectedRec(Node v, Node target, Edge e, Set<Edge> flipped, NodeSet used) {
		if (v == target) {
			return true;
		} else {
			used.add(v);
			for (var f : v.adjacentEdges()) {
				if (f != e && ((!flipped.contains(f) && v == f.getSource()) || flipped.contains(f) && v == f.getTarget())) {
					var w = f.getOpposite(v);
					if (!used.contains(w) && canReachDirectedRec(v.getOpposite(f), target, e, flipped, used))
						return true;
				}
			}
		}
		return false;
	}

}

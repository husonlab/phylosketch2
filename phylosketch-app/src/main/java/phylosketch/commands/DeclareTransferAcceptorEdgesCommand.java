/*
 * DeclareTransferAcceptorEdgesCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.paint.Color;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.util.IteratorUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.List;

public class DeclareTransferAcceptorEdgesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public DeclareTransferAcceptorEdgesCommand(DrawView view, Edge e) {
		super("transfer acceptor");

		if (e.getTarget().getInDegree() > 1) {
			var originalAcceptor = IteratorUtils.asStream(e.getTarget().inEdges()).filter(f -> view.getGraph().isTransferAcceptorEdge(f)).findAny().orElse(null);
			var originalAcceptorId = (originalAcceptor != null ? originalAcceptor.getId() : null);
			var oldState = (originalAcceptorId != null && e.getId() == originalAcceptorId);
			var newState = !oldState;
			var id = e.getId();

			undo = () -> {
				var edge = view.getGraph().findEdgeById(id);
				if (oldState) {
					view.getGraph().setTransferAcceptor(edge, true);
				} else {
					view.getGraph().setTransferAcceptor(edge, false);
					if (originalAcceptorId != null) {
						view.getGraph().setTransferAcceptor(view.getGraph().findEdgeById(originalAcceptorId), true);
					}
				}
			};
			redo = () -> {
				var edge = view.getGraph().findEdgeById(id);
				if (newState) {
					if (originalAcceptorId != null) {
						view.getGraph().setTransferAcceptor(view.getGraph().findEdgeById(originalAcceptorId), false);
					}
					view.getGraph().setTransferAcceptor(edge, true);
				} else {
					view.getGraph().setTransferAcceptor(edge, false);
				}
			};

		} else {
			undo = null;
			redo = null;
		}
	}

	public DeclareTransferAcceptorEdgesCommand(DrawView view, Collection<Edge> edges) {
		super("transfer acceptor");
		var list = new CompositeCommand(this.getName());
		for (var e : edges) {
			var command = new DeclareTransferAcceptorEdgesCommand(view, e);
			if (command.isUndoable() && command.isRedoable()) {
				list.add(command);
				list.add(new EdgeColorCommand(view, IteratorUtils.asList(e.getTarget().inEdges()), Color.DARKORANGE));
				var newState = !view.getGraph().isTransferAcceptorEdge(e);
				if (newState) {
					list.add(new EdgeColorCommand(view, List.of(e), Color.BLACK.deriveColor(1, 1, 1, 0.999)));
				}
				list.add(new ShowArrowsCommand(view, List.of(e), !newState));
			}
		}
		if (list.isUndoable() && list.isRedoable()) {
			undo = list::undo;
			redo = list::redo;
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
}

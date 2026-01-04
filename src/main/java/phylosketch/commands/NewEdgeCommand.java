/*
 * NewEdgeCommand.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.view.DrawView;

import java.util.function.Consumer;

/**
 * creates a new edge
 * Daniel Huson, 12.2024
 */
public class NewEdgeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final int sourceId;
	private final int targetId;
	private int newEdgeId = -1;

	/**
	 * constructor
	 *
	 * @param view the window
	 * @param path the path
	 */
	public NewEdgeCommand(DrawView view, Node source, Node target, Path path, Consumer<Edge> newEdgeConsumer) {
		super("create edge");

		sourceId = source.getId();
		targetId = target.getId();

		undo = () -> {
			if (newEdgeId != -1)
				view.deleteEdge(view.getGraph().findEdgeById(newEdgeId));
		};

		redo = () -> {
			var e = view.createEdge(view.getGraph().findNodeById(sourceId), view.getGraph().findNodeById(targetId), path, newEdgeId);
			newEdgeId = e.getId();
			if (newEdgeConsumer != null)
				newEdgeConsumer.accept(e);
		};
	}

	public int getNewEdgeId() {
		return newEdgeId;
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


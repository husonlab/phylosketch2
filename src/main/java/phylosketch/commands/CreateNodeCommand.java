/*
 * CreateNodeCommand.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.view.DrawView;

/**
 * creates a new node
 * Daniel Huson, 9.2024
 */
public class CreateNodeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private int newNodeId = -1;

	/**
	 * constructor
	 *
	 * @param view     the window
	 * @param location the location
	 * @param label label or null
	 */
	public CreateNodeCommand(DrawView view, Point2D location, String label) {
		super("create node");

		undo = () -> {
			if (newNodeId != -1)
				view.deleteNode(view.getGraph().findNodeById(newNodeId));
		};

		redo = () -> {
			var v = view.createNode(location, newNodeId);
			if (label != null)
				view.setLabel(v, label);
			newNodeId = v.getId();
		};
	}

	public int getNewNodeId() {
		return newNodeId;
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


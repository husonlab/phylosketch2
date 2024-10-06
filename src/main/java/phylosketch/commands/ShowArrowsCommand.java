/*
 * ShowArrowsCommand.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.fx.undo.UndoableRedoableCommand;
import phylosketch.view.DrawPane;

import java.util.Arrays;
import java.util.Objects;

public class ShowArrowsCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final int[] edgeIds;

	public ShowArrowsCommand(DrawPane view, boolean show) {
		super("arrows");

		edgeIds = view.getSelectedOrAllEdges().stream().mapToInt(e -> e.getId()).toArray();

		undo = () -> {
			var edges = Arrays.stream(edgeIds).mapToObj(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toList();
			for (var e : edges) {
				view.setShowArrow(e, !show);
			}
		};
		redo = () -> {
			var edges = Arrays.stream(edgeIds).mapToObj(id -> view.getGraph().findEdgeById(id)).filter(Objects::nonNull).toList();
			for (var e : edges) {
				view.setShowArrow(e, show);
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

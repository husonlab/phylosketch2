/*
 * SetupFormatting.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.view;

import phylosketch.commands.EdgeWidthCommand;
import phylosketch.commands.LabelFormattingCommand;
import phylosketch.commands.NodeSizeCommand;
import phylosketch.window.MainWindowController;

public class SetupFormatting {

	public static void apply(DrawPane view, MainWindowController controller) {
		controller.getBoldButton().setOnAction(e -> {
			var select = view.getSelectedOrAllNodes().stream().map(view::getLabel).anyMatch(t -> t != null && !t.isBold());
			view.getUndoManager().doAndAdd(new LabelFormattingCommand(view, false, false, select, null));
		});
		controller.getItalicButton().setOnAction(e -> {
			var select = view.getSelectedOrAllNodes().stream().map(view::getLabel).anyMatch(t -> t != null && !t.isItalic());
			view.getUndoManager().doAndAdd(new LabelFormattingCommand(view, false, false, null, select));
		});
		controller.getIncreaseFontButton().setOnAction(e -> view.getUndoManager().doAndAdd(new LabelFormattingCommand(view, true, false, null, null)));
		controller.getDecreaseFontButton().setOnAction(e -> view.getUndoManager().doAndAdd(new LabelFormattingCommand(view, false, true, null, null)));
		controller.getSmallNodeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new NodeSizeCommand(view, 3)));
		controller.getMediumNodeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new NodeSizeCommand(view, 5)));
		controller.getLargeNodeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new NodeSizeCommand(view, 7)));

		controller.getThinEdgeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new EdgeWidthCommand(view, 1)));
		controller.getMediumEdgeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new EdgeWidthCommand(view, 3)));
		controller.getThickEdgeButton().setOnAction(e -> view.getUndoManager().doAndAdd(new EdgeWidthCommand(view, 5)));
	}
}

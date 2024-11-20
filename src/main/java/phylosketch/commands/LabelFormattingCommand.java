/*
 * LabelFormattingCommand.java Copyright (C) 2024 Daniel H. Huson
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

import java.util.HashMap;
import java.util.Map;

/**
 * simple formatting of node labels
 * Daniel Huson, 11.2024
 */
public class LabelFormattingCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;
	private final Map<Integer, Formatting> oldFormattingMap = new HashMap<>();
	private final Map<Integer, Formatting> newFormattingMap = new HashMap<>();

	public LabelFormattingCommand(DrawPane view, boolean increaseSize, boolean decreaseSize, Boolean bold, Boolean italic) {
		super("format");

		for (var v : view.getSelectedOrAllNodes()) {
			var id = v.getId();
			var label = view.getLabel(v);
			if (label != null) {
				var size = label.getFontSize();
				oldFormattingMap.put(id, new Formatting(size, label.isBold(), label.isItalic()));
				if (increaseSize)
					size *= 1.2;
				if (decreaseSize)
					size /= 1.2;
				boolean boldValue = (bold == null ? label.isBold() : bold);
				boolean italicValue = (italic == null ? label.isItalic() : italic);
				newFormattingMap.put(id, new Formatting(size, boldValue, italicValue));
			}
		}

		if (newFormattingMap.isEmpty()) {
			undo = null;
			redo = null;
		} else {
			undo = () -> {
				for (var entry : oldFormattingMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					var label = view.getLabel(v);
					var formatting = entry.getValue();
					label.setFontSize(formatting.fontSize());
					label.setBold(formatting.bold());
					label.setItalic(formatting.italics());
				}
			};
			redo = () -> {
				for (var entry : newFormattingMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					var label = view.getLabel(v);
					var formatting = entry.getValue();
					label.setFontSize(formatting.fontSize());
					label.setBold(formatting.bold());
					label.setItalic(formatting.italics());
				}
			};
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

	public record Formatting(double fontSize, boolean bold, boolean italics) {
	}
}

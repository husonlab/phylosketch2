/*
 * NodeLabelFormatCommand.java Copyright (C) 2024 Daniel H. Huson
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
 * node label formating
 * Daniel Huson, 11.2024
 */
public class NodeLabelFormatCommand extends UndoableRedoableCommand {
	public enum Which {font, size, bold, italic, underlined, strike}

	private final Runnable undo;
	private final Runnable redo;
	private final Map<Integer, Formatting> oldMap = new HashMap<>();
	private final Map<Integer, Formatting> newMap = new HashMap<>();

	public NodeLabelFormatCommand(DrawPane view, Which which, Boolean newValue, Double newSize, String newFontFamily) {
		super("format");

		for (var v : view.getSelectedOrAllNodes()) {
			var id = v.getId();
			var label = view.getLabel(v);
			if (label != null) {
				if (which == Which.font) {
					oldMap.put(id, new Formatting(which, null, null, label.getFontFamily()));
					newMap.put(id, new Formatting(which, null, null, newFontFamily));
				} else if (which == Which.size) {
					oldMap.put(id, new Formatting(which, null, label.getFontSize(), null));
					newMap.put(id, new Formatting(which, null, newSize, null));
				} else {
					var oldValue = switch (which) {
						case bold -> label.isBold();
						case italic -> label.isItalic();
						case underlined -> label.isUnderline();
						case strike -> label.isStrike();
						default -> null;
					};
					oldMap.put(id, new Formatting(which, oldValue, null, null));
					newMap.put(id, new Formatting(which, newValue, null, null));
				}
			}
		}

		if (newMap.isEmpty()) {
			undo = null;
			redo = null;
		} else {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = view.getLabel(v);
					if (label != null) {
						var formatting = oldMap.get(id);
						switch (which) {
							case bold -> label.setBold(formatting.value());
							case italic -> label.setItalic(formatting.value());
							case underlined -> label.setUnderline(formatting.value());
							case strike -> label.setStrike(formatting.value());
							case size -> label.setFontSize(formatting.size());
							case font -> label.setFontFamily(formatting.fontFamily());
						}
					}
				}
			};
			redo = () -> {
				for (var id : newMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = view.getLabel(v);
					if (label != null) {
						var formatting = newMap.get(id);
						switch (which) {
							case bold -> label.setBold(formatting.value());
							case italic -> label.setItalic(formatting.value());
							case underlined -> label.setUnderline(formatting.value());
							case strike -> label.setStrike(formatting.value());
							case size -> label.setFontSize(formatting.size());
							case font -> label.setFontFamily(formatting.fontFamily());
						}
					}
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


	public record Formatting(Which which, Boolean value, Double size, String fontFamily) {
	}
}

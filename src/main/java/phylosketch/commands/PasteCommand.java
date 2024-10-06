/*
 * PasteCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.fx.util.ClipboardUtils;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.StringUtils;
import phylosketch.io.ImportNewick;
import phylosketch.view.DrawPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * paste newick or labels command
 * Daniel Huson, 9.2024
 */
public class PasteCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	// used when pasted Newick:
	private final String pastedLine;
	private Collection<Node> newNodes;

	// used when pasted labels:
	private final Map<Node, String> nodeOldLabelMap = new HashMap<>();
	private final Map<Node, String> nodeNewLabelMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view the view
	 */
	public PasteCommand(DrawPane view) {
		super("paste");

		var pastedString = ClipboardUtils.getTextFilesContentOrString(1000000);
		pastedLine = StringUtils.getFirstLine(pastedString);
		if (pastedLine.startsWith("(") && (pastedLine.endsWith(")") || pastedLine.endsWith(";"))) {
			try {
				var tree = new PhyloTree();
				tree.parseBracketNotation(pastedLine, true);
			} catch (IOException ex) {
				return;
			}
			undo = () -> {
				if (newNodes != null) {
					view.deleteNode(newNodes.toArray(new Node[0]));
					newNodes = null;
				}
			};
			redo = () -> {
				newNodes = null;
				try (BufferedReader r = new BufferedReader(new StringReader(pastedLine))) {
					newNodes = ImportNewick.apply(r, view, 1);
				} catch (IOException ignored) {
					undo = null;
					redo = null;
				}
			};
		} else {
			var selectedLeaves = view.getNodeSelection().getSelectedItems().stream().filter(Node::isLeaf).toList();
			if (!selectedLeaves.isEmpty()) {
				var lines = StringUtils.getLinesFromString(pastedString, selectedLeaves.size());
				var count = 0;
				for (var v : selectedLeaves) {
					if (count < lines.size()) {
						nodeOldLabelMap.put(v, view.getLabel(v).getText());
						var line = lines.get(count);
						if (line.length() > 1024)
							line = line.substring(0, 1024);
						nodeNewLabelMap.put(v, line);
						count++;
					} else break;
				}
				if (!nodeNewLabelMap.isEmpty()) {
					undo = () -> {
						for (var entry : nodeOldLabelMap.entrySet()) {
							view.getLabel(entry.getKey()).setText(entry.getValue());
						}
					};
					redo = () -> {
						for (var entry : nodeNewLabelMap.entrySet()) {
							view.getLabel(entry.getKey()).setText(entry.getValue());
						}
					};
				}
			}
		}
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}
}

/*
 * PasteCommand.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
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
	private Collection<Node> newNodes;

	// used when pasted labels:
	private final Map<Integer, Pair<String, String>> nodeOldLabelMap = new HashMap<>();
	private final Map<Integer, Pair<String, String>> nodeNewLabelMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view the window
	 */
	public PasteCommand(DrawPane view, String pastedString) {
		super("paste");

		var pastedLines = StringUtils.getLinesFromString(pastedString, 1000);
		var looksLikeNewick = !pastedLines.isEmpty() && pastedLines.stream().allMatch(s -> s.trim().startsWith("(") && (s.trim().endsWith(")") || s.trim().endsWith(";")));

		if (looksLikeNewick) {
			try {
				var tree = new PhyloTree();
				tree.parseBracketNotation(pastedLines.get(0), true);
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
				try (BufferedReader r = new BufferedReader(new StringReader(StringUtils.toString(pastedLines, "\n")))) {
					newNodes = ImportNewick.apply(r, view);
				} catch (IOException ignored) {
					undo = null;
					redo = null;
				}
			};
		} else {
			var selectedLeaves = view.getNodeSelection().getSelectedItems().stream().filter(Node::isLeaf).toList();
			if (!selectedLeaves.isEmpty()) {
				var count = 0;
				for (var v : selectedLeaves) {
					if (count < pastedLines.size()) {
						var id = v.getId();
						var graphLabel = view.getGraph().getLabel(v);
						var displayLabel = view.getLabel(v).getText();
						nodeOldLabelMap.put(id, new Pair<>(graphLabel, displayLabel));
						var line = pastedLines.get(count);
						if (line.length() > 1024)
							line = line.substring(0, 1024);
						var newGraphLabel = RichTextLabel.getRawText(line);
						nodeNewLabelMap.put(id, new Pair<>(newGraphLabel, line));
						count++;
					} else break;
				}
				if (!nodeNewLabelMap.isEmpty()) {
					undo = () -> {
						for (var entry : nodeOldLabelMap.entrySet()) {
							var v = view.getGraph().findNodeById(entry.getKey());
							var pair = entry.getValue();
							view.getGraph().setLabel(v, pair.getFirst());
							view.getLabel(v).setText(pair.getSecond());
						}
					};
					redo = () -> {
						for (var entry : nodeNewLabelMap.entrySet()) {
							var v = view.getGraph().findNodeById(entry.getKey());
							var pair = entry.getValue();
							view.getGraph().setLabel(v, pair.getFirst());
							view.getLabel(v).setText(pair.getSecond());
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

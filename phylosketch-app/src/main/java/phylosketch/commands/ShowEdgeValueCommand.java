/*
 * ShowEdgeValueCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Edge;
import jloda.util.StringUtils;
import phylosketch.view.DrawView;

import java.util.HashMap;
import java.util.Map;

/**
 * show edge values command
 * Daniel Huson, 2024
 */
public class ShowEdgeValueCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Show> oldMap = new HashMap<>();
	private final Map<Integer, Show> newMap = new HashMap<>();

	public ShowEdgeValueCommand(DrawView view, Boolean showWeights, Boolean showSupport, Boolean showProbability) {
		super("edge labels");

		var graph = view.getGraph();

		for (var e : view.getSelectedOrAllEdges()) {
			var label = DrawView.getLabel(e);
			if (label != null) {
				var oldLabel = label.getText();
				var oldShow = new Show(hasWeights(oldLabel), hasSupport(oldLabel), hasProbability(oldLabel));
				oldMap.put(e.getId(), oldShow);
				var newShowWeights = (showWeights != null ? showWeights : hasWeights(oldLabel));
				var newShowSupport = (showSupport != null ? showSupport : hasSupport(oldLabel));
				var newShowProbability = (showProbability != null ? showProbability : hasProbability(oldLabel));
				var newShow = new Show(newShowWeights, newShowSupport, newShowProbability);
				newMap.put(e.getId(), newShow);
			}
		}

		undo = () -> {
			for (var id : oldMap.keySet()) {
				var e = graph.findEdgeById(id);
				var show = oldMap.get(id);
				view.setLabel(e, show.makeLabel(view, e));
			}
		};

		redo = () -> {
			for (var id : newMap.keySet()) {
				var e = graph.findEdgeById(id);
				var show = newMap.get(id);
				view.setLabel(e, show.makeLabel(view, e));
			}
		};
	}

	public static boolean hasWeights(String label) {
		return !label.isBlank() && !label.trim().startsWith(":");
	}

	public static boolean hasSupport(String label) {
		return !label.isBlank() && label.contains(":") && !label.contains("::");
	}

	public static boolean hasProbability(String label) {
		return !label.isBlank() && StringUtils.countOccurrences(label, ':') == 2;
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	public record Show(boolean weights, boolean support, boolean probability) {
		public String makeLabel(DrawView view, Edge e) {
			var graph = view.getGraph();
			var buf = new StringBuilder();
			if (weights && graph.hasEdgeWeights())
				buf.append(StringUtils.removeTrailingZerosAfterDot(graph.getWeight(e)));
			if (support) {
				if (e.getTarget().getOutDegree() > 0) {
					buf.append(":");
					buf.append(StringUtils.removeTrailingZerosAfterDot(graph.getConfidence(e)));
				}
			}
			if (probability) {
				if (graph.hasEdgeProbabilities() && e.getTarget().getInDegree() > 1) {
					if (e.getTarget().getOutDegree() <= 1)
						buf.append(":");
					buf.append(":");
					buf.append(StringUtils.removeTrailingZerosAfterDot(graph.getProbability(e)));
				}
			}
			return buf.toString();
		}
	}
}

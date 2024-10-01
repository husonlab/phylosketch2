/*
 * ShowEdgeValueCommand.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch2.commands;

import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.util.StringUtils;
import phylosketch2.view.DrawPane;

import java.util.HashMap;
import java.util.Map;

public class ShowEdgeValueCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, String> edgeOldLabelMap = new HashMap<>();
	private final Map<Integer, String> edgeNewLabelMap = new HashMap<>();

	public ShowEdgeValueCommand(DrawPane view, boolean showWeights, boolean showConfidence, boolean showProbability) {
		super("edge labels");

		var graph = view.getGraph();

		for (var e : view.getSelectedOrAllEdges()) {
			edgeOldLabelMap.put(e.getId(), view.getLabel(e).getText());
			edgeNewLabelMap.put(e.getId(), makeLabel(view, e, showWeights, showConfidence, showProbability));
		}

		undo = () -> {
			for (var entry : edgeOldLabelMap.entrySet()) {
				view.setLabel(graph.findEdgeById(entry.getKey()), entry.getValue());
			}
		};

		redo = () -> {
			for (var entry : edgeNewLabelMap.entrySet()) {
				view.setLabel(graph.findEdgeById(entry.getKey()), entry.getValue());
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

	public static String makeLabel(DrawPane view, Edge e, boolean showWeights, boolean showConfidence, boolean showProbability) {
		return makeLabel(view, e, showWeights, null, showConfidence, null, showProbability, null);
	}

	public static String makeLabel(DrawPane view, Edge e, boolean showWeights, Double newWeight, boolean showConfidence, Double newConfidence, boolean showProbability, Double newProbability) {
		var graph = view.getGraph();
		var buf = new StringBuilder();
		if (showWeights && graph.hasEdgeWeights())
			buf.append(StringUtils.removeTrailingZerosAfterDot(newWeight == null ? graph.getWeight(e) : newWeight));
		if (showConfidence) {
			if (graph.hasEdgeConfidences() && e.getTarget().getOutDegree() > 0) {
				if (!buf.isEmpty())
					buf.append(":");
				buf.append(StringUtils.removeTrailingZerosAfterDot(newConfidence == null ? graph.getConfidence(e) : newConfidence));
			}
		}
		if (showProbability) {
			if (graph.hasEdgeProbabilities() && e.getTarget().getInDegree() > 1) {
				if (!buf.isEmpty())
					buf.append(":");
				buf.append(StringUtils.removeTrailingZerosAfterDot(newProbability == null ? graph.getProbability(e) : newProbability));
			}
		}
		return buf.toString();
	}
}

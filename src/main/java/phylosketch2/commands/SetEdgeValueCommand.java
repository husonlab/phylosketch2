/*
 * SetWeightsCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import phylosketch2.view.DrawPane;
import phylosketch2.view.RootLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * set edge value command
 * Daniel Huson, 9.2024
 */
public class SetEdgeValueCommand extends UndoableRedoableCommand {
	public enum What {Weight, Confidence, Probability}

	final private Runnable undo;
	final private Runnable redo;

	final private Map<Integer, Double> edgeOldMap = new HashMap<>();
	final private Map<Integer, Double> edgeNewMap = new HashMap<>();
	private final Map<Integer, String> edgeOldLabelMap = new HashMap<>();
	private final Map<Integer, String> edgeNewLabelMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view  the view
	 * @param what  what to set
	 * @param value the value to set to. If what is Weight and value=-1, will set to weights graphical edge lengths
	 */
	public SetEdgeValueCommand(DrawPane view, What what, double value) {
		super("weights");

		var graph = view.getGraph();

		final Map<Node, RootLocation> nodeRootOrientationMap;
		if (what == What.Weight && value == -1) {
			nodeRootOrientationMap = new HashMap<>();
			for (var component : ConnectedComponents.components(graph)) {
				var rootLocation = RootLocation.compute(component);
				for (var e : view.getEdgeSelection().getSelectedItems()) {
					if (component.contains(e.getSource())) {
						nodeRootOrientationMap.put(e.getSource(), rootLocation);
					}
				}
			}
			;
		} else nodeRootOrientationMap = null;

		for (var e : view.getEdgeSelection().getSelectedItems()) {
			switch (what) {
				case Weight -> {
					var useValue = (value == -1 ? computeGraphicalEdgeLength(view, nodeRootOrientationMap.get(e.getSource()), e) : value);
					edgeOldMap.put(e.getId(), graph.getWeight(e));
					edgeNewMap.put(e.getId(), useValue);
					edgeOldLabelMap.put(e.getId(), view.getLabel(e).getText());
					edgeNewLabelMap.put(e.getId(), ShowEdgeValueCommand.makeLabel(view, e, view.isShowWeight(), useValue, view.isShowConfidence(), null, view.isShowProbability(), null));
				}
				case Confidence -> {
					if (e.getTarget().getInDegree() > 1) {
						edgeOldMap.put(e.getId(), graph.getConfidence(e));
						edgeNewMap.put(e.getId(), value);
						edgeOldLabelMap.put(e.getId(), view.getLabel(e).getText());
						edgeNewLabelMap.put(e.getId(), ShowEdgeValueCommand.makeLabel(view, e, view.isShowWeight(), null, view.isShowConfidence(), value, view.isShowProbability(), null));
					}
				}
				case Probability -> {
					if (e.getTarget().getInDegree() > 1) {
						edgeOldMap.put(e.getId(), graph.getProbability(e));
						edgeNewMap.put(e.getId(), value);
						edgeOldLabelMap.put(e.getId(), view.getLabel(e).getText());
						edgeNewLabelMap.put(e.getId(), ShowEdgeValueCommand.makeLabel(view, e, view.isShowWeight(), null, view.isShowConfidence(), null, view.isShowProbability(), value));
						if ((value > 0 && value < 1) && e.getTarget().getInDegree() == 2) {
							var f = (e.getTarget().getFirstInEdge() == e ? e.getTarget().getLastInEdge() : e.getTarget().getFirstInEdge());
							if (!view.getEdgeSelection().isSelected(f)) {
								edgeOldMap.put(f.getId(), graph.getProbability(f));
								edgeOldLabelMap.put(f.getId(), view.getLabel(f).getText());
								edgeNewMap.put(f.getId(), 1.0 - value);
								edgeNewLabelMap.put(f.getId(), ShowEdgeValueCommand.makeLabel(view, f, view.isShowWeight(), null, view.isShowConfidence(), null, view.isShowProbability(), 1.0 - value));
							}
						}
					}
				}
			}
		}

		if (!edgeNewMap.isEmpty()) {
			undo = () -> {
				for (var entry : edgeOldMap.entrySet()) {
					var e = graph.findEdgeById(entry.getKey());
					switch (what) {
						case Weight -> graph.setWeight(e, entry.getValue());
						case Confidence -> graph.setConfidence(e, entry.getValue());
						case Probability -> graph.setProbability(e, entry.getValue());
					}
					view.getLabel(e).setText(edgeOldLabelMap.get(entry.getKey()));
				}
			};

			redo = () -> {
				for (var entry : edgeNewMap.entrySet()) {
					var e = graph.findEdgeById(entry.getKey());
					switch (what) {
						case Weight -> graph.setWeight(e, entry.getValue());
						case Confidence -> graph.setConfidence(e, entry.getValue());
						case Probability -> graph.setProbability(e, entry.getValue());
					}
					view.getLabel(e).setText(edgeNewLabelMap.get(entry.getKey()));
				}
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	public Double computeGraphicalEdgeLength(DrawPane view, RootLocation rootLocation, Edge e) {
		var a = view.getPoint(e.getSource());
		var b = view.getPoint(e.getTarget());
		return switch (rootLocation) {
			case Left, Right -> Math.abs(a.getX() - b.getX());
			case Top, Bottom -> Math.abs(a.getY() - b.getY());
		};
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
}

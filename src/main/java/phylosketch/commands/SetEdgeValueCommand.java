/*
 * SetEdgeValueCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * set edge value command
 * Daniel Huson, 9.2024
 */
public class SetEdgeValueCommand extends UndoableRedoableCommand {
	public static final double UNSET = Double.MIN_VALUE;

	final private Runnable undo;
	final private Runnable redo;

	public enum What {Weight, Confidence, Probability}

	final private Map<Integer, Double> edgeOldMap = new HashMap<>();
	final private Map<Integer, Double> edgeNewMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view  the window
	 * @param what  what to set
	 * @param value the value to set to. If what is Weight and value=-1, will set to weights graphical edge lengths
	 */
	public SetEdgeValueCommand(DrawView view, What what, double value) {
		super("set " + what.toString().toLowerCase());

		var graph = view.getGraph();

		if (view.getEdgeSelection().size() == 0)
			view.getEdgeSelection().selectAll(view.getGraphFX().getEdgeList());

		final Map<Node, RootPosition> nodeRootOrientationMap;
		if (what == What.Weight && value == -1) {
			nodeRootOrientationMap = new HashMap<>();
			for (var component : ConnectedComponents.components(graph)) {
				var rootLocation = RootPosition.compute(component);
				for (var e : view.getEdgeSelection().getSelectedItems()) {
					if (component.contains(e.getSource())) {
						nodeRootOrientationMap.put(e.getSource(), rootLocation);
					}
				}
			}
		} else nodeRootOrientationMap = null;

		var additionalToSelect = new ArrayList<Edge>();
		for (var e : view.getEdgeSelection().getSelectedItems()) {
			switch (what) {
				case Weight -> {
					var useValue = (value == -1 ? (int) Math.round(computeGraphicalEdgeLength(nodeRootOrientationMap.get(e.getSource()), e)) : value);
					var oldValue = (graph.hasEdgeWeights() ? graph.getEdgeWeights().getOrDefault(e, UNSET) : UNSET);
					edgeOldMap.put(e.getId(), oldValue);
					edgeNewMap.put(e.getId(), useValue);
				}
				case Confidence -> {
					if (e.getTarget().getOutDegree() > 0) {
						var oldValue = (graph.hasEdgeConfidences() ? graph.getEdgeConfidences().getOrDefault(e, UNSET) : UNSET);
						edgeOldMap.put(e.getId(), oldValue);
						edgeNewMap.put(e.getId(), value);
					}
				}
				case Probability -> {
					if (e.getTarget().getInDegree() > 1) {
						var oldValue = (graph.hasEdgeProbabilities() ? graph.getEdgeProbabilities().getOrDefault(e, UNSET) : UNSET);
						edgeOldMap.put(e.getId(), oldValue);
						edgeNewMap.put(e.getId(), value);
						if ((value > 0 && value < 1) && e.getTarget().getInDegree() == 2) {
							var f = (e.getTarget().getFirstInEdge() == e ? e.getTarget().getLastInEdge() : e.getTarget().getFirstInEdge());
							if (!view.getEdgeSelection().isSelected(f)) {
								edgeOldMap.put(f.getId(), graph.getProbability(f));
								edgeNewMap.put(f.getId(), 1.0 - value);
								additionalToSelect.add(f);
							}
						}
					}
				}
			}
			view.getEdgeSelection().getSelectedItems().addAll(additionalToSelect);
		}

		if (!edgeNewMap.isEmpty()) {
			undo = () -> {
				for (var entry : edgeOldMap.entrySet()) {
					var e = graph.findEdgeById(entry.getKey());
					switch (what) {
						case Weight -> {
							if (entry.getValue() != UNSET)
								graph.setWeight(e, entry.getValue());
							else if (graph.hasEdgeWeights())
								graph.getEdgeWeights().remove(e);
						}
						case Confidence -> {
							if (entry.getValue() != UNSET)
								graph.setConfidence(e, entry.getValue());
							else if (graph.hasEdgeConfidences())
								graph.getEdgeConfidences().remove(e);
						}
						case Probability -> {
							if (entry.getValue() != UNSET)
								graph.setProbability(e, entry.getValue());
							else if (graph.hasEdgeProbabilities())
								graph.getEdgeProbabilities().remove(e);
						}
					}
				}
			};

			redo = () -> {
				for (var entry : edgeNewMap.entrySet()) {
					var e = graph.findEdgeById(entry.getKey());
					switch (what) {
						case Weight -> {
							if (entry.getValue() != UNSET)
								graph.setWeight(e, entry.getValue());
							else if (graph.hasEdgeWeights())
								graph.getEdgeWeights().remove(e);
						}
						case Confidence -> {
							if (entry.getValue() != UNSET)
								graph.setConfidence(e, entry.getValue());
							else if (graph.hasEdgeConfidences())
								graph.getEdgeConfidences().remove(e);
						}
						case Probability -> {
							if (entry.getValue() != UNSET)
								graph.setProbability(e, entry.getValue());
							else if (graph.hasEdgeProbabilities())
								graph.getEdgeProbabilities().remove(e);
						}
					}
				}
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	public static Double computeGraphicalEdgeLength(RootPosition rootPosition, Edge e) {
		var a = DrawView.getPoint(e.getSource());
		var b = DrawView.getPoint(e.getTarget());
		return switch (rootPosition.side()) {
			case Left, Right -> Math.abs(a.getX() - b.getX());
			case Top, Bottom -> Math.abs(a.getY() - b.getY());
			case Center -> a.distance(b);
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

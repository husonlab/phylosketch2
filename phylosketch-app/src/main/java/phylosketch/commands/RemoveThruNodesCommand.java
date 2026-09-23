/*
 * RemoveThruNodesCommand.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * remove all or selected "true nodes" (di-vertices) from graph
 * Daniel Huson, 9.2024, 1.2025
 */
public class RemoveThruNodesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private int vId;

	private CompositeCommand compositeCommand;

	/**
	 * constructor
	 *
	 * @param view the draw view
	 * @param v the node
	 */
	public RemoveThruNodesCommand(DrawView view, Node v) {
		super("remove thru nodes");

		if (v.getInDegree() == 1 && v.getOutDegree() == 1 && DrawView.getLabel(v).getRawText().isBlank()) {
			vId = v.getId();

			undo = () -> {
				if (compositeCommand != null && compositeCommand.isUndoable())
					compositeCommand.undo();
			};

			redo = () -> {
				var graph = view.getGraph();
				var u = graph.findNodeById(vId);

				var e = u.getFirstInEdge();
				var f = u.getFirstOutEdge();

				// carry the edge values over to the new edge when suppressing the di-vertex:
				// weights add up, confidences take the minimum and probabilities multiply;
				// if only one edge has a value, that value is used
				final var newWeight = combinedWeight(graph, e, f);
				final var newConfidence = combinedConfidence(graph, e, f);
				final var newProbability = combinedProbability(graph, e, f);

				// carry over which of these values were being displayed on the replaced edges
				var eText = (DrawView.getLabel(e) != null ? DrawView.getLabel(e).getText() : "");
				var fText = (DrawView.getLabel(f) != null ? DrawView.getLabel(f).getText() : "");
				final var showWeight = newWeight != null && (ShowEdgeValueCommand.hasWeights(eText) || ShowEdgeValueCommand.hasWeights(fText));
				final var showSupport = newConfidence != null && (ShowEdgeValueCommand.hasSupport(eText) || ShowEdgeValueCommand.hasSupport(fText));
				final var showProbability = newProbability != null && (ShowEdgeValueCommand.hasProbability(eText) || ShowEdgeValueCommand.hasProbability(fText));

				var strokeWidth = Math.abs(0.5 * (DrawView.getPath(e).getStrokeWidth() + DrawView.getPath(f).getStrokeWidth()));
				Consumer<Edge> styleEdges = h -> {
					DrawView.getPath(h).setStrokeWidth(strokeWidth);
					if (newWeight != null)
						graph.setWeight(h, newWeight);
					if (newConfidence != null)
						graph.setConfidence(h, newConfidence);
					if (newProbability != null)
						graph.setProbability(h, newProbability);
					if (showWeight || showSupport || showProbability)
						view.setLabel(h, new ShowEdgeValueCommand.Show(showWeight, showSupport, showProbability).makeLabel(view, h));
				};



				var points = CollectionUtils.concatenate(PathUtils.extractPoints(DrawView.getPath(e)), PathUtils.extractPoints(DrawView.getPath(f)));
				var createEdge = new DrawEdgeCommand(view, PathUtils.createPath(points, true), styleEdges);
				var deleteNode = new DeleteCommand(view, List.of(v), List.of());
				compositeCommand = new CompositeCommand("remove thru node", createEdge, deleteNode);
				if (compositeCommand.isRedoable())
					compositeCommand.redo();
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	/**
	 * constructor
	 *
	 * @param view  the draw view
	 * @param nodes the node s
	 */
	public RemoveThruNodesCommand(DrawView view, Collection<Node> nodes) {
		super("remove thru nodes");

		var ids = nodes.stream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1 && DrawView.getLabel(v).getRawText().isBlank())
				.mapToInt(v -> v.getId()).toArray();

		if (ids.length > 0) {
			undo = () -> {
				if (compositeCommand != null && compositeCommand.isUndoable())
					compositeCommand.undo();
			};

			redo = () -> {
				compositeCommand = new CompositeCommand("remove thru nodes");
				for (var id : ids) {
					var v = view.getGraph().findNodeById(id);
					compositeCommand.add(new RemoveThruNodesCommand(view, v));
				}
				if (compositeCommand.isRedoable())
					compositeCommand.redo();
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	/**
	 * weight for the new edge: the sum if both edges have a weight, otherwise the one present value, or null if neither has one
	 */
	private static Double combinedWeight(PhyloTree graph, Edge e, Edge f) {
		var he = graph.hasEdgeWeights() && graph.getEdgeWeights().get(e) != null;
		var hf = graph.hasEdgeWeights() && graph.getEdgeWeights().get(f) != null;
		if (he && hf)
			return graph.getWeight(e) + graph.getWeight(f);
		else if (he)
			return graph.getWeight(e);
		else if (hf)
			return graph.getWeight(f);
		else
			return null;
	}

	/**
	 * confidence for the new edge: the minimum if both edges have a confidence, otherwise the one present value, or null if neither has one
	 */
	private static Double combinedConfidence(PhyloTree graph, Edge e, Edge f) {
		var he = graph.hasEdgeConfidences() && graph.getEdgeConfidences().get(e) != null;
		var hf = graph.hasEdgeConfidences() && graph.getEdgeConfidences().get(f) != null;
		if (he && hf)
			return Math.min(graph.getConfidence(e), graph.getConfidence(f));
		else if (he)
			return graph.getConfidence(e);
		else if (hf)
			return graph.getConfidence(f);
		else
			return null;
	}

	/**
	 * probability for the new edge: the product if both edges have a probability, otherwise the one present value, or null if neither has one
	 */
	private static Double combinedProbability(PhyloTree graph, Edge e, Edge f) {
		var he = graph.hasEdgeProbabilities() && graph.getEdgeProbabilities().get(e) != null;
		var hf = graph.hasEdgeProbabilities() && graph.getEdgeProbabilities().get(f) != null;
		if (he && hf)
			return graph.getProbability(e) * graph.getProbability(f);
		else if (he)
			return graph.getProbability(e);
		else if (hf)
			return graph.getProbability(f);
		else
			return null;
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

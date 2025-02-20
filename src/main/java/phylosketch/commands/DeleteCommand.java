/*
 * DeleteCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.IteratorUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.*;

/**
 * delete the provided nodes and/or edges
 * Daniel Huson, 10.2024
 */
public class DeleteCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final ArrayList<NodeData> nodeDataList = new ArrayList<>();
	private final ArrayList<EdgeData> edgeDataList = new ArrayList<>();

	/**
	 * constructor
	 *
	 * @param view
	 * @param nodes
	 * @param edges0
	 */
	public DeleteCommand(DrawView view, Collection<Node> nodes, Collection<Edge> edges0) {
		super("delete");

		if (nodes.isEmpty() && edges0.isEmpty()) {
			undo = null;
			redo = null;
			return;
		}

		var edges = new HashSet<>(edges0);
		for (var v : nodes) {
			edges.addAll(IteratorUtils.asList(v.adjacentEdges()));
		}

		for (var v : nodes) {
			nodeDataList.add(new NodeData(view, v));
		}
		for (var e : edges) {
			edgeDataList.add(new EdgeData(view, e));
		}

		undo = () -> {
			for (var d : nodeDataList) {
				var v = view.createNode(d.shape, d.label, d.id);
				if (d.text != null)
					view.getGraph().setLabel(v, d.text);
			}
			for (var d : edgeDataList) {
				var v = view.getGraph().findNodeById(d.sourceId);
				var w = view.getGraph().findNodeById(d.targetId);
				var e = view.createEdge(v, w, new Path(), d.id);
				d.apply(view, e);
			}
		};

		redo = () -> {
			view.deleteEdge(edgeDataList.stream().map(d -> d.id).map(view.getGraph()::findEdgeById).filter(Objects::nonNull).toArray(Edge[]::new));
			view.deleteNode(nodeDataList.stream().map(d -> d.id).map(view.getGraph()::findNodeById).filter(Objects::nonNull).toArray(Node[]::new));
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

	public record NodeData(int id, String text, Shape shape, String label) {
		public NodeData(DrawView view, Node v) {
			this(v.getId(), view.getGraph().getLabel(v), DrawView.getShape(v), DrawView.getLabel(v).getText());
		}
	}

	public record EdgeData(int id, int sourceId, int targetId, boolean arrow, Double weight,
						   Double confidence, Double probability, String label, List<Point2D> points,
						   Color stroke, double strokeWidth, List<Double> dashArray) {
		public EdgeData(DrawView view, Edge e) {
			this(e.getId(), e.getSource().getId(), e.getTarget().getId(), view.getEdgeArrowMap().containsKey(e),
					view.getGraph().hasEdgeWeights() ? view.getGraph().getWeight(e) : null,
					view.getGraph().hasEdgeConfidences() ? view.getGraph().getConfidence(e) : null,
					view.getGraph().hasEdgeProbabilities() ? view.getGraph().getProbability(e) : null,
					DrawView.getLabel(e).getRawText(), PathUtils.extractPoints(DrawView.getPath(e)), (Color) DrawView.getPath(e).getStroke(),
					DrawView.getPath(e).getStrokeWidth(), new ArrayList<>(DrawView.getPath(e).getStrokeDashArray()));
		}

		public void apply(DrawView view, Edge e) {
			if (weight != null)
				view.getGraph().setWeight(e, weight);
			if (confidence != null)
				view.getGraph().setWeight(e, confidence);
			if (probability != null)
				view.getGraph().setWeight(e, probability);
			if (arrow)
				view.setShowArrow(e, true);
			if (label != null)
				view.setLabel(e, label);
			DrawView.getPath(e).getElements().setAll(PathUtils.toPathElements(points));
			DrawView.getPath(e).setStroke(stroke);
			DrawView.getPath(e).setStrokeWidth(strokeWidth);
			DrawView.getPath(e).getStrokeDashArray().setAll(dashArray);
		}
	}
}

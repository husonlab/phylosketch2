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

import javafx.geometry.Point2D;
import javafx.scene.shape.Path;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.SelectionEffectBlue;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.*;

/**
 * more all selected nodes into one
 * Daniel Huson, 9.2024, 1.2025
 */
public class MergeNodesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private int vId;

	private CompositeCommand compositeCommand;

	/**
	 * constructor
	 *
	 * @param view  the draw view
	 * @param nodes the nodes
	 */
	public MergeNodesCommand(DrawView view, Collection<Node> nodes) {
		super("merge nodes");

		var nodeIds = new HashSet<>(nodes.stream().map(Node::getId).toList());
		for (var v : view.getGraph().nodes()) {
			if (!nodeIds.contains(v.getId())) {
				if (v.getInDegree() > 0 && v.getOutDegree() > 0 && IteratorUtils.asStream(v.adjacentNodes()).allMatch(w -> nodeIds.contains(w.getId()))) {
					nodeIds.add(v.getId());
				}
			}
		}
		if (nodeIds.size() >= 2) {
			undo = () -> {
				if (compositeCommand != null && compositeCommand.isUndoable())
					compositeCommand.undo();
			};
			redo = () -> {
				var blobNodes = new ArrayList<>(nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).filter(Objects::nonNull).toList());

				// if there is a label on an isolated node, then place it on the non-isolated one
				if (blobNodes.size() == 2 && blobNodes.stream().anyMatch(v -> v.getDegree() == 0 && !DrawView.getLabel(v).getRawText().isBlank())
					&& blobNodes.stream().anyMatch(v -> v.getDegree() > 0 && DrawView.getLabel(v).getRawText().isBlank())) {
					blobNodes.sort(Comparator.comparingInt(Node::getDegree));
					var v = blobNodes.get(0);
					var w = blobNodes.get(1);

					var vLabel = DrawView.getLabel(v).getText();
					var rootPosition = RootPosition.compute(ConnectedComponents.component(w));
					compositeCommand = new CompositeCommand("merge",
							new SetNodeLabelsCommand(view, rootPosition, List.of(w), vLabel),
							new DeleteCommand(view, List.of(v), List.of()));
					if (compositeCommand.isRedoable())
						compositeCommand.redo();
					return;
				}

				var centerNode = getCenterNode(blobNodes);
				blobNodes.remove(centerNode);

				if (false) {
					for (var w : blobNodes) {
						DrawView.getShape(w).setEffect(SelectionEffectBlue.getInstance());
					}
				} else {
					var paths = new ArrayList<Path>();
					{
						var sources = new HashSet<Node>();
						sources.add(centerNode);
						var inEdges = new ArrayList<Edge>();
						view.getGraph().edgeStream().filter(e -> !blobNodes.contains(e.getSource()) && blobNodes.contains(e.getTarget()))
								.forEach(e -> {
									if (!sources.contains(e.getSource())) {
										sources.add(e.getSource());
										inEdges.add(e);
									}
								});
						for (var e : inEdges) {
							paths.add(PathUtils.createPath(CollectionUtils.concatenate(DrawView.getPoints(e), List.of(DrawView.getPoint(centerNode))), true));
						}
					}
					{
						var targets = new HashSet<Node>();
						targets.add(centerNode);
						var outEdges = new ArrayList<Edge>();
						view.getGraph().edgeStream().filter(e -> blobNodes.contains(e.getSource()) && !blobNodes.contains(e.getTarget()))
								.forEach(e -> {
									if (!targets.contains(e.getTarget())) {
										targets.add(e.getTarget());
										outEdges.add(e);
									}
								});
						for (var e : outEdges) {
							paths.add(PathUtils.createPath(CollectionUtils.concatenate(List.of(DrawView.getPoint(centerNode)), DrawView.getPoints(e)), true));
						}
					}
					var commands = new ArrayList<UndoableRedoableCommand>();
					commands.add(new DeleteCommand(view, blobNodes, List.of()));
					for (var path : paths) {
						commands.add(new DrawEdgeCommand(view, path, null));
					}
					compositeCommand = new CompositeCommand(this.getName(), commands.toArray(new UndoableRedoableCommand[0]));
					if (compositeCommand.isRedoable())
						compositeCommand.redo();
				}
			};
		} else {
			undo = null;
			redo = null;
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
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}

	/**
	 * gets the node nearest the center. If any of the nodes has a non-blank label, will only consider the labeled nodes
	 *
	 * @param nodes nodes
	 * @return center node, with label, if possible
	 */
	public static Node getCenterNode(List<Node> nodes) {
		var labeled = nodes.stream().filter(v -> !DrawView.getLabel(v).getRawText().isBlank()).toList();
		if (!labeled.isEmpty()) {
			nodes = labeled;
		}

		// if there is one node that is ancestor of all others, then use it as center
		{
			for (var v : nodes) {
				var stack = new Stack<Node>();
				stack.add(v);
				var visited = new HashSet<Node>();
				while (!stack.isEmpty()) {
					var w = stack.pop();
					visited.add(w);
					for (var u : w.children()) {
						if (nodes.contains(u) && !visited.contains(u)) {
							stack.push(u);
						}
					}
				}
				if (visited.size() == nodes.size()) {
					return v;
				}
			}
		}

		var maxDegree = nodes.stream().mapToInt(Node::getDegree).max().orElse(0);
		var maxDegreeNodes = nodes.stream().filter(v -> v.getDegree() == maxDegree).toList();

		if (maxDegreeNodes.size() == 1) {
			return maxDegreeNodes.get(0);
		}

		var sumX = 0.0;
		var sumY = 0.0;
		for (var v : nodes) {
			var p = DrawView.getPoint(v);
			sumX += p.getX();
			sumY += p.getY();
		}
		var n = nodes.size();
		var centroid = new Point2D(sumX / n, sumY / n);

		var w = nodes.get(0);
		var minDistance = centroid.distance(DrawView.getPoint(w));

		for (var v : nodes) {
			double distance = centroid.distance(DrawView.getPoint(v));
			if (distance < minDistance) {
				minDistance = distance;
				w = v;
			}
		}
		return w;
	}
}

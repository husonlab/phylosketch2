/*
 *  Copyright (C) 2018. Daniel H. Huson
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
 */

package phylosketch2.view;

import javafx.beans.binding.Bindings;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.algorithms.RootedNetworkProperties;
import phylosketch2.window.MainWindowController;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * setup selection menu items
 * Daniel Huson, 9.2024
 */
public class SetupSelection {
	public static void apply(DrawPane view, MainWindowController controller) {
		var graph = view.getGraph();
		var nodeSelection = view.getNodeSelection();
		var edgeSelection = view.getEdgeSelection();

		controller.getSelectButton().setOnAction(a -> {

			for (var e : graph.edges()) {
				if (nodeSelection.isSelected(e.getSource()) && !nodeSelection.isSelected(e.getTarget())) {
					controller.getSelectAllBelowMenuItem().fire();
					return;
				}
			}
			for (var e : graph.edges()) {
				if (!nodeSelection.isSelected(e.getSource()) && nodeSelection.isSelected(e.getTarget())) {
					controller.getSelectAllAboveMenuItem().fire();
					return;
				}
			}

			if (nodeSelection.size() < graph.getNumberOfNodes() || edgeSelection.size() < graph.getNumberOfEdges()) {
				graph.nodes().forEach(nodeSelection::select);
				graph.edges().forEach(edgeSelection::select);
			} else {
				nodeSelection.clearSelection();
				edgeSelection.clearSelection();
			}
		});
		controller.getSelectAllMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectAllMenuItem().setOnAction(e -> {
			graph.nodes().forEach(nodeSelection::select);
			graph.edges().forEach(edgeSelection::select);
		});
		controller.getSelectAllMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectNoneMenuItem().setOnAction(e -> {
			nodeSelection.clearSelection();
			edgeSelection.clearSelection();
		});
		controller.getSelectNoneMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()).and(Bindings.isEmpty(edgeSelection.getSelectedItems())));

		controller.getSelectInvertMenuItem().setOnAction(e -> {
			graph.nodes().forEach(nodeSelection::toggleSelection);
			graph.edges().forEach(edgeSelection::toggleSelection);
		});
		controller.getSelectInvertMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExtendSelectionMenuItem().setOnAction(e -> controller.getSelectButton().fire());
		controller.getExtendSelectionMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());


		controller.getSelectRootsMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() == 0).forEach(nodeSelection::select));
		controller.getSelectRootsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectLeavesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getOutDegree() == 0).forEach(nodeSelection::select));
		controller.getSelectLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectReticulateNodesMenuitem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() > 1).forEach(nodeSelection::select));
		controller.getSelectReticulateNodesMenuitem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectStableNodesMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllCompletelyStableInternal(graph).forEach(nodeSelection::select));
		controller.getSelectStableNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectVisibleNodesMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllVisibleNodes(graph, null).forEach(nodeSelection::select));
		controller.getSelectVisibleNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectVisibleReticulationsMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllVisibleNodes(graph, null).stream().filter(v -> v.getInDegree() > 1).forEach(nodeSelection::select));
		controller.getSelectVisibleReticulationsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectTreeNodesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() <= 1 && v.getOutDegree() > 0).forEach(nodeSelection::select));
		controller.getSelectTreeNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectAllAboveMenuItem().setOnAction(c -> {
			var list = new LinkedList<>(nodeSelection.getSelectedItems());

			while (!list.isEmpty()) {
				Node v = list.remove();
				for (Edge e : v.inEdges()) {
					Node w = e.getSource();
					edgeSelection.select(e);
					if (!nodeSelection.isSelected(w)) {
						nodeSelection.select(w);
						list.add(w);
					}
				}
			}
		});
		controller.getSelectAllAboveMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()));

		controller.getSelectAllBelowMenuItem().setOnAction(c -> {
			final Queue<Node> list = new LinkedList<>(nodeSelection.getSelectedItems());

			while (!list.isEmpty()) {
				Node v = list.remove();
				for (Edge e : v.outEdges()) {
					Node w = e.getTarget();
					edgeSelection.select(e);
					if (!nodeSelection.isSelected(w)) {
						nodeSelection.select(w);
						list.add(w);
					}
				}
			}
		});
		controller.getSelectAllBelowMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()));

		controller.getSelectPossibleRootLocationsMenuItem().setOnAction(a -> {
			view.getEdgeSelection().clearSelection();
			view.getNodeSelection().clearSelection();
			var stack = new Stack<Node>();
			for (var v : graph.nodes()) {
				if (v.getInDegree() == 0) {
					stack.push(v);
					while (!stack.isEmpty()) {
						v = stack.pop();
						for (var e : v.outEdges()) {
							var w = e.getTarget();
							if (w.getInDegree() == 1) {
								if (!nodeSelection.isSelected(w)) {
									view.getEdgeSelection().select(e);
									view.getNodeSelection().select(w);
									stack.push(w);
								}
							}
						}
					}
				}
			}
		});

		controller.getSelectLowestStableAncestorMenuItem().setOnAction(e -> nodeSelection.selectAll(RootedNetworkProperties.computeAllLowestStableAncestors(graph, nodeSelection.getSelectedItems())));
		controller.getSelectLowestStableAncestorMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()));

		controller.getSelectTreeEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() <= 1).forEach(edgeSelection::select));
		controller.getSelectTreeEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectReticulateEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).forEach(edgeSelection::select));
		controller.getSelectReticulateEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSelectThruNodesMenuItem().setOnAction(c -> graph.nodeStream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1 && view.getLabel(v).getRawText().isBlank()).forEach(nodeSelection::select));
		controller.getSelectThruNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

	}

}

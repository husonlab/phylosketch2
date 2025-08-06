/*
 * DrawNetwork.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.draw;

import javafx.geometry.Point2D;
import javafx.scene.shape.Path;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathNormalize;
import phylosketch.utils.CircleSegment;
import phylosketch.utils.QuadraticCurve;
import phylosketch.view.DrawView;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * draw a network
 * Daniel Huson, 2.2025
 */
public class DrawNetwork {
	/**
	 * for a given tree, copies it into the view graph and draws it using the given points
	 *
	 * @param view   the view
	 * @param tree   the source tree
	 * @param points the points
	 */
	public static void apply(DrawView view, PhyloTree tree, Map<Node, Double> angles, Map<Node, Point2D> points) {
		try (NodeArray<Node> srcTarNode = tree.newNodeArray(); EdgeArray<Edge> srcTarEdge = tree.newEdgeArray()) {
			for (var v : tree.nodes()) {
				srcTarNode.put(v, view.createNode());
			}

			for (var e : tree.edges()) {
				var f = view.createEdge(srcTarNode.get(e.getSource()), srcTarNode.get(e.getTarget()), new Path());
				srcTarEdge.put(e, f);
			}

			apply(view, tree, srcTarNode, srcTarEdge, angles, points, false);
		}
	}

	/**
	 * for a given tree that has already been copied into the view graph
	 *
	 * @param view       the view
	 * @param tree       the source tree
	 * @param srcTarNode mapping of nodes in tree to nodes in view graph
	 * @param srcTarEdge mapping of edges in tree to edges in view graph
	 * @param points     the points as a function of source tree nodes
	 * @param circular
	 */
	public static void apply(DrawView view, PhyloTree tree, Function<Node, Node> srcTarNode, Function<Edge, Edge> srcTarEdge, Map<Node, Double> angles, Map<Node, Point2D> points, boolean circular) {
		var rootPoint = (circular ? points.get(tree.getRoot()) : null);

		for (var v : tree.nodes()) {
			var w = srcTarNode.apply(v);
			view.setLocation(w, points.get(v));
		}

		for (var e : tree.edges()) {
			var v = srcTarNode.apply(e.getSource());
			var w = srcTarNode.apply(e.getTarget());
			var f = srcTarEdge.apply(e);
			var vPoint = DrawView.getPoint(v);
			var wPoint = DrawView.getPoint(w);

			List<Point2D> list;

			var reticulate = false;
			if (e.getTarget().getInDegree() == 1 || tree.isTransferAcceptorEdge(e)) {
				if (circular) {
					var circleSegment = CircleSegment.apply(rootPoint, vPoint.distance(rootPoint), angles.get(e.getSource()), angles.get(e.getTarget()));
					list = PathNormalize.apply(CollectionUtils.concatenate(circleSegment, List.of(wPoint)), 2, 5);
				} else {
					list = CollectionUtils.concatenate(
							PathNormalize.apply(List.of(vPoint, new Point2D(vPoint.getX(), wPoint.getY())), 2, 5),
							PathNormalize.apply(List.of(new Point2D(vPoint.getX(), wPoint.getY()), wPoint), 2, 5));
				}
			} else if (tree.isTransferEdge(e) || (tree.isReticulateEdge(e) && circular)) {
				var control = QuadraticCurve.computeControlForBowEdge(vPoint, wPoint);
				list = QuadraticCurve.apply(vPoint, control, wPoint);
				reticulate = true;
			} else if (circular) {
				list = PathNormalize.apply(List.of(vPoint, wPoint), 2, 5);
				reticulate = true;
			} else {
				list = QuadraticCurve.apply(vPoint, new Point2D(vPoint.getX(), wPoint.getY()), wPoint);
				reticulate = true;
			}

			var path = DrawView.setPoints(f, list);

			if (tree.isReticulateEdge(e))
				view.getGraph().setReticulate(f, true);
			if (tree.isTransferAcceptorEdge(e))
				view.getGraph().setTransferAcceptor(f, true);
			if (reticulate && !view.getGraph().isTransferAcceptorEdge(e)) {
				view.setShowArrow(f, true);
				path.getStyleClass().add("graph-special-edge");
			}
			if (tree.hasEdgeWeights() && tree.getEdgeWeights().get(e) != null) {
				view.getGraph().setWeight(f, tree.getWeight(e));
			}
			if (tree.hasEdgeConfidences() && tree.getEdgeConfidences().get(e) != null) {
				view.getGraph().setConfidence(f, tree.getConfidence(e));
			}
			if (tree.hasEdgeProbabilities() && tree.getEdgeProbabilities().get(e) != null) {
				view.getGraph().setProbability(f, tree.getProbability(e));
			}
		}

		for (var v : tree.nodes()) {
			var w = srcTarNode.apply(v);
			if (tree.getLabel(v) != null) {

				if (DrawView.getLabel(v) == null) {
					view.createLabel(w, tree.getLabel(v));
				} else if (!DrawView.getLabel(v).getRawText().equals(tree.getLabel(v))) {
					view.setLabel(w, tree.getLabel(v));
				}
			}
		}
	}
}


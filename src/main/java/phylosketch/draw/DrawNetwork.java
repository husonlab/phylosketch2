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
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import phylosketch.paths.PathUtils;
import phylosketch.utils.CircleSegment;
import phylosketch.utils.QuadraticCurve;
import phylosketch.view.DrawView;
import phylosketch.view.LayoutLabels;

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
	public static void apply(DrawView view, PhyloTree tree, Map<Node, Point2D> points, LayoutRootedPhylogeny.Layout layout) {
		try (NodeArray<Node> srcTarNode = tree.newNodeArray(); EdgeArray<Edge> srcTarEdge = tree.newEdgeArray()) {
			for (var v : tree.nodes()) {
				srcTarNode.put(v, view.createNode());
			}

			for (var e : tree.edges()) {
				var f = view.createEdge(srcTarNode.get(e.getSource()), srcTarNode.get(e.getTarget()), new Path());
				srcTarEdge.put(e, f);
			}

			apply(view, tree, srcTarNode, srcTarEdge, points, layout);
		}
	}

	/**
	 * for a given tree that has already been copied into the view graph
	 *
	 * @param view         the view
	 * @param tree         the source tree
	 * @param srcTarNode   mapping of nodes in tree to nodes in view graph
	 * @param srcTarEdge   mapping of edges in tree to edges in view graph
	 * @param nodePointMap the points as a function of source tree nodes
	 */
	public static void apply(DrawView view, PhyloTree tree, Function<Node, Node> srcTarNode, Function<Edge, Edge> srcTarEdge, Map<Node, Point2D> nodePointMap, LayoutRootedPhylogeny.Layout layout) {
		var rootNode = (tree.getRoot() != null) ? tree.getRoot() : tree.nodeStream().filter(v -> v.getInDegree() == 0).findAny().orElse(tree.getFirstNode());
		var rootPoint = (layout == LayoutRootedPhylogeny.Layout.Rectangular ? null : nodePointMap.get(rootNode));

		for (var v : tree.nodes()) {
			var w = srcTarNode.apply(v);
			view.setLocation(w, nodePointMap.get(v));
			var label = DrawView.getLabel(v);
			if (label != null) {
				label.setRotate(0);
				label.setLayoutX(0);
				label.setLayoutY(0);
			}
		}

		for (var oe : tree.edges()) {
			var source = srcTarNode.apply(oe.getSource());
			var target = srcTarNode.apply(oe.getTarget());
			var e = srcTarEdge.apply(oe);
			var sourcePoint = DrawView.getPoint(source);
			var targetPoint = DrawView.getPoint(target);

			if (tree.isReticulateEdge(oe))
				view.getGraph().setReticulate(e, true);
			if (tree.isTransferAcceptorEdge(oe))
				view.getGraph().setTransferAcceptor(e, true);
			if (tree.hasEdgeWeights() && tree.getEdgeWeights().get(oe) != null) {
				view.getGraph().setWeight(e, tree.getWeight(oe));
			}
			if (tree.hasEdgeConfidences() && tree.getEdgeConfidences().get(oe) != null) {
				view.getGraph().setConfidence(e, tree.getConfidence(oe));
			}
			if (tree.hasEdgeProbabilities() && tree.getEdgeProbabilities().get(oe) != null) {
				view.getGraph().setProbability(e, tree.getProbability(oe));
			}


			var path = new Path();

			path.getElements().add(new MoveTo(sourcePoint.getX(), sourcePoint.getY()));

			if (tree.isTreeEdge(oe) || tree.isTransferAcceptorEdge(oe)) {
				switch (layout) {
					case Rectangular -> {
						var controlPoint = new Point2D(sourcePoint.getX(), targetPoint.getY());
						path.getElements().setAll(PathUtils.createPath(List.of(sourcePoint, controlPoint, targetPoint), true).getElements());
					}
					case Circular -> {
						var sourceAngle = GeometryUtilsFX.computeAngle(sourcePoint.subtract(rootPoint));
						var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));
						var circle = CircleSegment.apply(rootPoint, rootPoint.distance(sourcePoint), sourceAngle, targetAngle);
						path.getElements().addAll(PathUtils.toPathElements(circle));
						var last = circle.get(circle.size() - 1);
						path.getElements().addAll(PathUtils.createPath(List.of(last, targetPoint), true).getElements());
					}
					case Radial -> {
						path.getElements().setAll(PathUtils.createPath(List.of(sourcePoint, targetPoint), true).getElements());
					}
				}
			} else if (tree.isTransferEdge(oe)) {
				path.getElements().setAll(PathUtils.createPath(List.of(sourcePoint, targetPoint), true).getElements());
			} else { // reticulate edge
				switch (layout) {
					case Rectangular -> {
						var controlPoint = new Point2D(sourcePoint.getX(), targetPoint.getY());
						path.getElements().addAll(PathUtils.createElements(QuadraticCurve.apply(sourcePoint, controlPoint, targetPoint, 5)));
					}
					case Circular -> {
						var sourceAngle = GeometryUtilsFX.computeAngle(sourcePoint.subtract(rootPoint));
						var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));
						var controlPoint = CircleSegment.getEndPoint(rootPoint, rootPoint.distance(sourcePoint), sourceAngle, targetAngle);
						path.getElements().addAll(PathUtils.createElements(QuadraticCurve.apply(sourcePoint, controlPoint, targetPoint, 5)));
					}
					case Radial -> {
						var diff = targetPoint.distance(rootPoint) - sourcePoint.distance(rootPoint);
						var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));
						var controlPoint = GeometryUtilsFX.translateByAngle(targetPoint, targetAngle, -diff);
						path.getElements().addAll(PathUtils.createElements(QuadraticCurve.apply(sourcePoint, controlPoint, targetPoint, 5)));
					}
				}
			}

			DrawView.getPath(e).getElements().setAll(path.getElements());

			if (tree.isReticulateEdge(oe) && !tree.isTransferAcceptorEdge(oe)) {
				view.setShowArrow(e, true);
				DrawView.getPath(e).getStyleClass().add("graph-special-edge");
			}
		}

		for (var ov : tree.nodes()) {
			var v = srcTarNode.apply(ov);
			if (tree.getLabel(ov) != null) {
				if (DrawView.getLabel(ov) == null) {
					view.createLabel(v, tree.getLabel(ov));
				} else if (!DrawView.getLabel(ov).getRawText().equals(tree.getLabel(ov))) {
					view.setLabel(v, tree.getLabel(ov));
				}
			}
		}

		LayoutLabels.apply(view, tree.getRoot(), tree.getNodesAsList(), layout, 10);
	}
}


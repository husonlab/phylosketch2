/*
 * EmbedNetwork.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.treesnet;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import jloda.fx.control.RichTextLabel;
import jloda.fx.phylo.embed.Averaging;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import phylosketch.paths.PathUtils;
import phylosketch.utils.CircleSegment;
import phylosketch.utils.ScaleUtils;

import java.util.Random;

/**
 * compute an embedding of the network
 * Daniel Huson, 4.2025
 */
public class EmbedNetwork {
	/**
	 * embed a network an return a group of edges and labels
	 *
	 * @param network   the network
	 * @param edgeWidth the width to use for the edges
	 * @param toScale   draw to scale?
	 * @param xMin      min x coordinate
	 * @param xMax      max x coordinate
	 * @param yMin      min y coordinate
	 * @param yMax      max y coordinate
	 * @return group
	 */
	public static Group apply(PhyloTree network, double edgeWidth, boolean toScale, double xMin, double xMax, double yMin, double yMax) {
		return apply(network, toScale ? LayoutRootedPhylogeny.Phylogram : LayoutRootedPhylogeny.CladogramEarly, edgeWidth, xMin, xMax, yMin, yMax);
	}

	/**
	 * compute an embedding of the network and return it as a group.
	 * Use v.getData() or e.getData() ta access shape associated with node or edge
	 * Use circle.getUserData() or path.getUserData() to get node or edge associated with shape
	 *
	 * @param network the network
	 * @param layout  desired layout type
	 * @param xMin    bounding box
	 * @param xMax    bounding box
	 * @param yMin    bounding box
	 * @param yMax    bounding box
	 * @return group of shapes
	 */
	public static Group apply(PhyloTree network, LayoutRootedPhylogeny layout, double edgeWidth, double xMin, double xMax, double yMin, double yMax) {
		if (layout.isCircular()) {
			var width = Math.min(xMax - xMin, yMax - yMin);
			xMax = xMin + width;
			yMax = yMin + width;
		}

		if (!network.hasLSAChildrenMap())
			LSAUtils.setLSAChildrenAndTransfersMap(network);

		var vertices = new Group();
		var edges = new Group();
		var labels = new Group();

		try (var nodeAngleMap = network.newNodeDoubleArray(); NodeArray<Point2D> points = network.newNodeArray(); NodeArray<Circle> circles = network.newNodeArray()) {
			LayoutRootedPhylogeny.apply(network, layout, Averaging.ChildAverage, true, new Random(666), nodeAngleMap, points);

			ScaleUtils.scaleToBox(points, xMin, xMax, yMin, yMax);

			for (var v : network.nodes()) {
				var point = points.get(v);
				var circle = new Circle(1.5);
				circle.getStyleClass().add("graph-node");
				circle.setTranslateX(point.getX());
				circle.setTranslateY(point.getY());
				circle.setUserData(v);
				v.setData(circle);
				vertices.getChildren().add(circle);
				circles.put(v, circle);
			}

			for (var e : network.edges()) {
				var path = new Path();
				var source = circles.get(e.getSource());
				var target = circles.get(e.getTarget());

				var moveTo = new MoveTo();
				moveTo.xProperty().bind(source.translateXProperty());
				moveTo.yProperty().bind(source.translateYProperty());
				path.getElements().add(moveTo);

				if ((network.isTreeEdge(e) || network.isTransferAcceptorEdge(e)) && !layout.isCircular()) {
					var lineTo = new LineTo();
					lineTo.xProperty().bind(source.translateXProperty());
					lineTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(lineTo);
				}

				if (((network.isTreeEdge(e) || network.isTransferAcceptorEdge(e)) && !layout.isCircular()) || network.isTransferEdge(e)) {
					var lineTo = new LineTo();
					lineTo.xProperty().bind(target.translateXProperty());
					lineTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(lineTo);
				} else if ((network.isTreeEdge(e) || network.isTransferAcceptorEdge(e)) && layout.isCircular()) {
					var rootPoint = points.get(network.getRoot());
					var sourcePoint = points.get(e.getSource());
					var circle = CircleSegment.apply(rootPoint, rootPoint.distance(sourcePoint), nodeAngleMap.get(e.getSource()), nodeAngleMap.get(e.getTarget()));
					path.getElements().addAll(PathUtils.toPathElements(circle));

					var lineTo = new LineTo();
					lineTo.xProperty().bind(target.translateXProperty());
					lineTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(lineTo);
				} else if (layout.isCircular()) { // reticulate edge
					var rootPoint = points.get(network.getRoot());
					var sourcePoint = points.get(e.getSource());
					var circle = CircleSegment.apply(rootPoint, rootPoint.distance(sourcePoint), nodeAngleMap.get(e.getSource()), nodeAngleMap.get(e.getTarget()));
					var controlPoint = circle.get(circle.size() / 2);
					var quadCurveTo = new QuadCurveTo();
					quadCurveTo.setControlX(controlPoint.getX());
					quadCurveTo.setControlY(controlPoint.getY());
					quadCurveTo.xProperty().bind(target.translateXProperty());
					quadCurveTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(quadCurveTo);

				}
				else { // reticulate edge
					var quadCurveTo = new QuadCurveTo();
					quadCurveTo.controlXProperty().bind(source.translateXProperty());
					quadCurveTo.controlYProperty().bind(target.translateYProperty());
					quadCurveTo.xProperty().bind(target.translateXProperty());
					quadCurveTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(quadCurveTo);
				}
				if (network.isTreeEdge(e) || network.isTransferAcceptorEdge(e)) {
					path.getStyleClass().add("graph-edge");
				} else {
					path.getStyleClass().add("graph-special-edge");
					path.setStrokeLineCap(StrokeLineCap.ROUND);
				}
				/*
				if(e.getSource().getInDegree()>0 && e.getTarget().getOutDegree()>0)
					path.setStrokeLineCap(StrokeLineCap.ROUND);
				path.setStrokeLineJoin(StrokeLineJoin.ROUND);
				 */
				if (layout.isCircular()) {
					path.setStrokeLineCap(StrokeLineCap.ROUND);
					path.setStrokeLineJoin(StrokeLineJoin.ROUND);
				}

				path.setUserData(e);
				e.setData(path);
				edges.getChildren().add(path);
				path.setStyle("-fx-stroke: white;-fx-stroke-width: %.1f;".formatted(edgeWidth));
				path.applyCss();
			}

			for (var v : network.nodes()) {
				var label = network.getLabel(v);
				if (label != null && !label.isBlank() && v.getData() instanceof Circle circle) {
					var richText = new RichTextLabel(label);
					richText.getStyleClass().add("graph-label");
					richText.applyCss();

					Point2D offset;
					var angle = GeometryUtilsFX.modulo360(nodeAngleMap.getOrDefault(v, 0.0));
					if (angle > 45 && angle < 135) {
						offset = new Point2D(-0.5 * richText.getEstimatedWidth(), +richText.getFontSize() + 0.5 * edgeWidth + 5);
					} else if (angle > 135 && angle < 225) {
						offset = new Point2D(-richText.getEstimatedWidth() - 0.5 * edgeWidth - 5, -0.5 * richText.getFontSize() - 5);
					} else if (angle > 225 && angle < 315) {
						offset = new Point2D(-0.5 * richText.getEstimatedWidth(), -richText.getFontSize() - 0.5 * edgeWidth - 5);
					} else {
						offset = new Point2D(0.5 * edgeWidth + 5, -0.5 * richText.getFontSize());
					}
					richText.setTranslateX(circle.getTranslateX() + offset.getX());
					richText.setTranslateY(circle.getTranslateY() + offset.getY());
					labels.getChildren().add(richText);
				}
			}
		}

		edges.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 0.5, 0.5, 0.0, 0.0));
		return new Group(edges, labels);
	}
}

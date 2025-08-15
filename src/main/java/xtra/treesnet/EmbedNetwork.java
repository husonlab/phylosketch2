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
import jloda.fx.util.RunAfterAWhile;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import phylosketch.paths.PathUtils;
import phylosketch.utils.CircleSegment;
import phylosketch.utils.ScaleUtils;
import xtra.daniel.LabelPlacer;

import java.util.ArrayList;
import java.util.Random;

/**
 * compute an embedding of the network
 * Daniel Huson, 4.2025
 */
public class EmbedNetwork {
	/**
	 * embed a network and return a group of edges and labels
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
		return apply(network, LayoutRootedPhylogeny.Layout.Rectangular, toScale ? LayoutRootedPhylogeny.Scaling.ToScale : LayoutRootedPhylogeny.Scaling.EarlyBranching, edgeWidth, xMin, xMax, yMin, yMax);
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
	public static Group apply(PhyloTree network, LayoutRootedPhylogeny.Layout layout, LayoutRootedPhylogeny.Scaling scaling, double edgeWidth, double xMin, double xMax, double yMin, double yMax) {
		if (layout == LayoutRootedPhylogeny.Layout.Circular || layout == LayoutRootedPhylogeny.Layout.Radial) {
			var width = Math.min(xMax - xMin, yMax - yMin);
			xMax = xMin + width;
			yMax = yMin + width;
		}

		if (!network.hasLSAChildrenMap())
			LSAUtils.setLSAChildrenAndTransfersMap(network);

		var vertices = new Group();
		var edges = new Group();
		var labels = new Group();

		try (var nodeAngleMap = network.newNodeDoubleArray(); NodeArray<Point2D> pointMap = network.newNodeArray(); NodeArray<Circle> circles = network.newNodeArray()) {
			LayoutRootedPhylogeny.apply(network, layout, scaling, Averaging.LeafAverage, true, new Random(666), nodeAngleMap, pointMap);

			ScaleUtils.scaleToBox(pointMap, xMin, xMax, yMin, yMax);

			for (var v : network.nodes()) {
				var point = pointMap.get(v);
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
				var rootPoint = pointMap.get(network.getRoot());
				var sourcePoint = pointMap.get(e.getSource());
				var targetPoint = pointMap.get(e.getTarget());


				path.getElements().add(new MoveTo(sourcePoint.getX(), sourcePoint.getY()));

				if ((network.isTreeEdge(e) || network.isTransferAcceptorEdge(e))) {
					switch (layout) {
						case Rectangular -> {
							path.getElements().add(new LineTo(sourcePoint.getX(), targetPoint.getY()));
							path.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));
						}
						case Circular -> {
							var sourceAngle = GeometryUtilsFX.computeAngle(sourcePoint.subtract(rootPoint));
							var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));
							var circle = CircleSegment.apply(rootPoint, rootPoint.distance(sourcePoint), sourceAngle, targetAngle);
							path.getElements().addAll(PathUtils.toPathElements(circle));
							path.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));
						}
						case Radial -> {
							path.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));
						}
					}
				} else if (network.isTransferEdge(e)) {
					path.getElements().add(new LineTo(sourcePoint.getX(), targetPoint.getY()));
					path.getElements().add(new LineTo(targetPoint.getX(), targetPoint.getY()));
				} else { // reticulate edge
					switch (layout) {
						case Rectangular -> {
							var quadCurveTo = new QuadCurveTo();
							quadCurveTo.setControlX(sourcePoint.getX());
							quadCurveTo.setControlY(targetPoint.getY());
							quadCurveTo.setX(targetPoint.getX());
							quadCurveTo.setY(targetPoint.getY());
							path.getElements().add(quadCurveTo);

						}
						case Circular -> {
							var sourceAngle = GeometryUtilsFX.computeAngle(sourcePoint.subtract(rootPoint));
							var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));

							var controlPoint = CircleSegment.getEndPoint(rootPoint, rootPoint.distance(sourcePoint), sourceAngle, targetAngle);
							var quadCurveTo = new QuadCurveTo();
							quadCurveTo.setControlX(controlPoint.getX());
							quadCurveTo.setControlY(controlPoint.getY());
							quadCurveTo.setX(targetPoint.getX());
							quadCurveTo.setY(targetPoint.getY());
							path.getElements().add(quadCurveTo);

						}
						case Radial -> {
							var diff = targetPoint.distance(rootPoint) - sourcePoint.distance(rootPoint);
							var targetAngle = GeometryUtilsFX.computeAngle(targetPoint.subtract(rootPoint));
							var controlPoint = GeometryUtilsFX.translateByAngle(targetPoint, targetAngle, -diff);
							var quadCurveTo = new QuadCurveTo();
							quadCurveTo.setControlX(controlPoint.getX());
							quadCurveTo.setControlY(controlPoint.getY());
							quadCurveTo.setX(targetPoint.getX());
							quadCurveTo.setY(targetPoint.getY());
							path.getElements().add(quadCurveTo);
						}
					}
				}
				/*
				if(e.getSource().getInDegree()>0 && e.getTarget().getOutDegree()>0)
					path.setStrokeLineCap(StrokeLineCap.ROUND);
				path.setStrokeLineJoin(StrokeLineJoin.ROUND);
				 */
				if (layout == LayoutRootedPhylogeny.Layout.Circular || layout == LayoutRootedPhylogeny.Layout.Radial) {
					path.setStrokeLineCap(StrokeLineCap.ROUND);
					path.setStrokeLineJoin(StrokeLineJoin.ROUND);
				}

				path.setUserData(e);
				e.setData(path);
				edges.getChildren().add(path);
				path.setStyle("-fx-stroke: white;-fx-stroke-width: %.1f;".formatted(edgeWidth));
				path.applyCss();
			}

			var rootPoint = pointMap.get(network.getRoot());
			var list = new ArrayList<Runnable>();
			for (var v : network.nodes()) {
				var label = network.getLabel(v);
				if (label != null && !label.isBlank() && v.getData() instanceof Circle circle) {
					var richText = new RichTextLabel(label);
					richText.getStyleClass().add("graph-label");
					richText.setVisible(false);
					list.add(() -> {
						LabelPlacer.placeLeafLabel(richText, pointMap.get(v), circle.getRadius(), layout == LayoutRootedPhylogeny.Layout.Rectangular, rootPoint, edgeWidth / 2.0 + 5);
						richText.setVisible(true);
					});
					labels.getChildren().add(richText);
				}
			}
			RunAfterAWhile.applyInFXThread(list, () -> {
				for (var runnable : list) {
					runnable.run();
				}
			});
		}

		edges.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 0.5, 0.5, 0.0, 0.0));
		return new Group(edges, labels);
	}
}

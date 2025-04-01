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

package xtra.test;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jloda.fx.util.GeometryUtilsFX;
import jloda.fx.util.Icebergs;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathUtils;
import phylosketch.utils.CircleSegment;
import phylosketch.utils.CubicCurve;
import phylosketch.utils.QuadraticCurve;
import phylosketch.utils.ScaleUtils;

import java.util.List;
import java.util.Map;

/**
 * draw the network
 * Daniel Huson, 2.2025
 */
public class DrawNetwork {
	/**
	 * compute the drawing
	 *
	 * @param tree       the network
	 * @param points     the  node locations
	 * @param nodeGroup  nodes group
	 * @param edgeGroup  edges group
	 * @param labelGroup labels groups
	 */
	public static void apply(boolean circular, PhyloTree tree, Map<Node, Point2D> points, Group nodeGroup, Group edgeGroup, Group labelGroup) {
		var lsaChildren = tree.getLSAChildrenMap();


		nodeGroup.getChildren().clear();
		edgeGroup.getChildren().clear();
		labelGroup.getChildren().clear();

		var rootPoint = (circular ? points.get(tree.getRoot()) : null);

		if (true)
			ScaleUtils.scaleToBox(points, 0, 600, 0, 600);

		for (var v : tree.nodes()) {
			var vPoint = points.get(v);
			for (var w : lsaChildren.get(v)) {
				if (w.getInDegree() > 1) {
					var wPoint = points.get(w);
					var path = PathUtils.createPath(List.of(vPoint, wPoint), true);
					path.setStroke(Color.LIGHTBLUE);
					path.setFill(Color.TRANSPARENT);
					path.setStrokeWidth(0.75);
					edgeGroup.getChildren().add(path);
				}
			}
		}

		for (var v : tree.nodes()) {
			var point = points.get(v);
			var circle = new Circle(1);
			circle.setTranslateX(point.getX());
			circle.setTranslateY(point.getY());
			v.setInfo(circle);
			nodeGroup.getChildren().add(circle);
			nodeGroup.getChildren().add(Icebergs.create(circle, true));
			if (tree.getLabel(v) != null && !tree.getLabel(v).isBlank()) {
				var label = new Label(tree.getLabel(v));
				label.setTranslateX(point.getX() + 5);
				label.setTranslateY(point.getY() - 5);
				labelGroup.getChildren().add(label);
			}
		}

		for (var e : tree.edges()) {
			var v = e.getSource();
			var w = e.getTarget();
			var vPoint = points.get(v);
			var wPoint = points.get(w);

			List<Point2D> list;

			var reticulate = false;
			if (e.getTarget().getInDegree() == 1 || tree.isTransferAcceptorEdge(e)) {
				if (circular) {
					var angleV = GeometryUtilsFX.computeAngle(vPoint.subtract(rootPoint));
					var angleW = GeometryUtilsFX.computeAngle(wPoint.subtract(rootPoint));
					var cPoint = GeometryUtilsFX.rotateAbout(vPoint, angleW - angleV, rootPoint);

					double startAngle;
					double endAngle;
					boolean flip;

					if (angleW > angleV && Math.abs(angleW - angleV) <= 180 || angleW < angleV && Math.abs(angleW - angleV) >= 180) {
						startAngle = angleV;
						endAngle = angleW;
						flip = false;
					} else {
						startAngle = angleW;
						endAngle = angleV;
						flip = true;
					}

					var circleSegment = CircleSegment.apply(rootPoint, vPoint.distance(rootPoint), startAngle, endAngle);
					if (flip)
						circleSegment = CollectionUtils.reverse(circleSegment);

					list = CollectionUtils.concatenate(circleSegment,
							PathNormalize.apply(List.of(cPoint, wPoint), 2, 5));
				} else {
					list = CollectionUtils.concatenate(
							PathNormalize.apply(List.of(vPoint, new Point2D(vPoint.getX(), wPoint.getY())), 2, 5),
							PathNormalize.apply(List.of(new Point2D(vPoint.getX(), wPoint.getY()), wPoint), 2, 5));
				}
			} else if (tree.isTransferEdge(e) || circular) {
				list = PathNormalize.apply(List.of(vPoint, wPoint), 2, 5);
				reticulate = true;
			} else {
				if (false) {
					var mid = vPoint.getY() + (wPoint.getY() - vPoint.getY()) * 0.7;
					var mid2 = vPoint.getY() + (wPoint.getY() - vPoint.getY()) * 0.3;
					list = CubicCurve.apply(vPoint, new Point2D(vPoint.getX(), mid), new Point2D(wPoint.getX(), mid2), wPoint, 5);
				} else {
					var mid = new Point2D(vPoint.getX() + 0.1 * (wPoint.getX() - vPoint.getX()), wPoint.getY());
					list = QuadraticCurve.apply(vPoint, mid, wPoint);
				}
				reticulate = true;
			}
			if (false) {
				list = PathNormalize.apply(List.of(vPoint, wPoint), 2, 5);
			}
			var path = PathUtils.createPath(list, false);
			path.setStroke(reticulate ? Color.DARKORANGE : Color.BLACK);
			path.setFill(Color.TRANSPARENT);
			e.setInfo(path);
			edgeGroup.getChildren().add(path);
		}
	}
}

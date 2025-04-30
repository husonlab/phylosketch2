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
import jloda.fx.util.BasicFX;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import phylosketch.embed.HeightAndAngles;
import phylosketch.embed.RectangularPhylogenyLayout;
import phylosketch.utils.ScaleUtils;

/**
 * compute an embedding of the network
 * Daniel Huson, 4.2025
 */
public class EmbedNetwork {
	/**
	 * compute an embedding of the network and return it as a group.
	 * Use v.getData() or e.getData() ta access shape associated with node or edge
	 * Use circle.getUserData() or path.getUserData() to get node or edge associated with shape
	 *
	 * @param network the network
	 * @param toScale draw to scale?
	 * @param xMin    bounding box
	 * @param xMax    bounding box
	 * @param yMin    bounding box
	 * @param yMax    bounding box
	 * @return group of shapes
	 */
	public static Group apply(PhyloTree network, boolean toScale, double xMin, double xMax, double yMin, double yMax) {
		var vertices = new Group();
		var edges = new Group();
		var labels = new Group();

		try (NodeArray<Point2D> points = network.newNodeArray(); NodeArray<Circle> circles = network.newNodeArray()) {
			RectangularPhylogenyLayout.apply(network, toScale, HeightAndAngles.Averaging.ChildAverage, true, points);
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
				var label = network.getLabel(v);
				if (label != null && !label.isBlank()) {
					var richText = new RichTextLabel(label);
					richText.getStyleClass().add("graph-label");
					richText.translateXProperty().bind(circle.translateXProperty().add(14));
					richText.translateYProperty().bind(circle.translateYProperty().add(-7));
					labels.getChildren().add(richText);
				}
			}

			for (var e : network.edges()) {
				var path = new Path();
				var source = circles.get(e.getSource());
				var target = circles.get(e.getTarget());

				var moveTo = new MoveTo();
				moveTo.xProperty().bind(source.translateXProperty());
				moveTo.yProperty().bind(source.translateYProperty());
				path.getElements().add(moveTo);

				if (network.isTreeEdge(e) || network.isTransferAcceptorEdge(e)) {
					var lineTo = new LineTo();
					lineTo.xProperty().bind(source.translateXProperty());
					lineTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(lineTo);
				}

				if (network.isTreeEdge(e) || network.isTransferAcceptorEdge(e) || network.isTransferEdge(e)) {
					var lineTo = new LineTo();
					lineTo.xProperty().bind(target.translateXProperty());
					lineTo.yProperty().bind(target.translateYProperty());
					path.getElements().add(lineTo);
				} else {
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
					path.setStyle("-fx-stroke: darkorange;");
				}
				path.setUserData(e);
				e.setData(path);
				edges.getChildren().add(path);
			}
		}
		if (false) {
			return new Group(edges, vertices, labels);
		} else {
			for (var path : BasicFX.getAllRecursively(edges, Path.class)) {
				path.setStyle("-fx-stroke: white;-fx-stroke-width: 25;");
			}
			edges.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 0.5, 0.5, 0.0, 0.0));
			return new Group(edges, labels);
		}
	}
}

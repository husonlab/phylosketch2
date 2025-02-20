/*
 * DrawOrderingGraph.java Copyright (C) 2025 Daniel H. Huson
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

package xtra;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.util.ColorSchemeManager;
import jloda.fx.util.SelectionEffect;
import jloda.graph.Node;
import phylosketch.paths.PathUtils;
import phylosketch.utils.QuadraticCurve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jloda.graph.DAGTraversals.postOrderTraversal;

/**
 * draw the ordering graph
 * Daniel Huson, 2.2024
 */
public class DrawOrderingGraph {
	/**
	 * creates and draws the ordering graph
	 *
	 * @param v           the node
	 * @param lsaChildren the node to LSA children map
	 * @param group       the group to place drawing into
	 */
	public static void apply(Node v, List<Node> originalOrdering, Map<Node, List<Node>> lsaChildren, Map<Node, Point2D> points, Group group) {
		var orderingGraph = new OrderingGraph(v, lsaChildren, points);
		var layoutNodes = orderingGraph.getLayoutNodes();
		var nodeLayoutNodeMap = orderingGraph.getPhyloNodeToLayoutNodeMap();

		var layoutNodeColorMap = new HashMap<Node, Color>();
		var aboveLayoutNode = layoutNodes.get(0);
		layoutNodeColorMap.put(aboveLayoutNode, Color.GHOSTWHITE);
		var belowLayoutNode = layoutNodes.get(layoutNodes.size() - 1);
		layoutNodeColorMap.put(belowLayoutNode, Color.GHOSTWHITE);

		var count = 0;
		var colors = ColorSchemeManager.getInstance().getColorScheme("Retro29");

		for (var w : originalOrdering) {
			var layoutNode = nodeLayoutNodeMap.get(w);
			var color = colors.get((count++) % colors.size());
			layoutNodeColorMap.put(layoutNode, color);

			var nodesBelow = new ArrayList<Node>();
			postOrderTraversal(w, lsaChildren::get, nodesBelow::add);
			var effect = SelectionEffect.create(layoutNodeColorMap.get(nodeLayoutNodeMap.get(w)));
			for (var a : nodesBelow) {
				if (a.getInfo() instanceof Shape shape) {
					shape.setEffect(effect);
				}
				for (var e : a.outEdges()) {
					var b = e.getTarget();
					if (nodesBelow.contains(b) && e.getInfo() instanceof Path path) {
						path.setEffect(effect);
					}
				}
			}
		}

		var layoutNodesGroup = new Group();
		var layoutEdgesGroup = new Group();

		var y = 50;
		for (var n : layoutNodes) {
			var shape = new Circle(6);
			shape.setFill(layoutNodeColorMap.get(n));
			shape.setStroke(Color.BLACK);
			shape.setTranslateX(75);
			shape.setTranslateY(y);
			n.setInfo(shape);
			Tooltip.install(shape, new Tooltip("" + orderingGraph.getNodeHeight(n)));
			y += 50;
			layoutNodesGroup.getChildren().add(shape);
		}

		for (var e : orderingGraph.getGraph().edges()) {
			var sShape = (Shape) e.getSource().getInfo();
			var tShape = (Shape) e.getTarget().getInfo();
			var sPoint = new Point2D(sShape.getTranslateX(), sShape.getTranslateY());
			var tPoint = new Point2D(tShape.getTranslateX(), tShape.getTranslateY());
			var dy = (int) Math.round(Math.abs(sPoint.getY() - tPoint.getY()));
			var cX = sPoint.getX() + (dy % 100 == 50 ? 0.5 : -0.5) * dy;
			var cY = 0.5 * (sPoint.getY() + tPoint.getY());
			var path = PathUtils.createPath(QuadraticCurve.apply(sPoint, new Point2D(cX, cY), tPoint), true);
			path.setStroke(Color.BLACK);
			path.setFill(Color.TRANSPARENT);
			path.setStrokeWidth(orderingGraph.getEdgeWeight(e));
			Tooltip.install(path, new Tooltip("" + orderingGraph.getEdgeWeight(e)));
			layoutEdgesGroup.getChildren().add(path);
		}
		group.getChildren().setAll(layoutEdgesGroup, layoutNodesGroup);
	}
}

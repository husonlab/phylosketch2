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
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathUtils;
import phylosketch.utils.QuadraticCurve;
import phylosketch.view.DrawView;

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
	 * @param tree   the network
	 * @param points the  node locations
	 */
	public static void apply(DrawView view, PhyloTree tree, Map<Node, Point2D> points) {
		try (NodeArray<Node> srcTarMap = tree.newNodeArray()) {
			for (var v : tree.nodes()) {
				srcTarMap.put(v, view.createNode(points.get(v)));
			}

			for (var v : tree.nodes()) {
				var w = srcTarMap.get(v);
				if (tree.getLabel(v) != null)
					view.createLabel(w, tree.getLabel(v));
			}

			for (var e : tree.edges()) {
				var v = srcTarMap.get(e.getSource());
				var w = srcTarMap.get(e.getTarget());
				var vPoint = DrawView.getPoint(v);
				var wPoint = DrawView.getPoint(w);

				List<Point2D> list;

				var reticulate = false;
				if (e.getTarget().getInDegree() == 1 || tree.isTransferAcceptorEdge(e)) {
					list = CollectionUtils.concatenate(
							PathNormalize.apply(List.of(vPoint, new Point2D(vPoint.getX(), wPoint.getY())), 2, 5),
							PathNormalize.apply(List.of(new Point2D(vPoint.getX(), wPoint.getY()), wPoint), 2, 5));
				} else if (tree.isTransferEdge(e)) {
					list = PathNormalize.apply(List.of(vPoint, wPoint), 2, 5);
					reticulate = true;
				} else {
					list = QuadraticCurve.apply(vPoint, new Point2D(vPoint.getX(), wPoint.getY()), wPoint);
					reticulate = true;
				}
				var path = PathUtils.createPath(list, false);
				var f = view.createEdge(v, w, path);
				if (reticulate) {
					view.setShowArrow(f, true);
					path.getStyleClass().add("graph-special-edge");
				}
			}
		}
	}
}


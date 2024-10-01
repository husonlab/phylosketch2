/*
 * ImportNewick.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch2.io;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.FileUtils;
import jloda.util.IteratorUtils;
import phylosketch2.embed.RootedNetworkEmbedder;
import phylosketch2.paths.PathUtils;
import phylosketch2.view.DrawPane;
import phylosketch2.view.RootLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * newick import
 * Daniel Huson, 8.2024
 */
public class ImportNewick {
	/**
	 * import from file
	 *
	 * @param fileName file
	 * @param view     the view to import into
	 * @throws IOException
	 */
	public static void apply(String fileName, DrawPane view) throws IOException {
		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(fileName))) {
			apply(r, view, (int) FileUtils.getNumberOfLinesInFile(fileName));
		}
	}

	/**
	 * import from a buffered reader
	 *
	 * @param r        reader
	 * @param view     view
	 * @param maxTrees max number to import
	 * @return set of new nodes
	 * @throws IOException
	 */
	public static Collection<Node> apply(BufferedReader r, DrawPane view, int maxTrees) throws IOException {
		var rootLocation = RootLocation.Left;

		var nTrees = Math.min(maxTrees, 6);

		var width = 750.0 / (1 + (nTrees > 3 ? 1 : 0));

		var oldNodes = IteratorUtils.asSet(view.getGraph().nodes());

		var count = 0;
		while (r.ready()) {
			var line = r.readLine();
			var tree = new PhyloTree();
			(new NewickIO()).parseBracketNotation(tree, line, true, false);
			tree.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).forEach(e -> tree.setReticulate(e, true));

			try (var nodePointMap = RootedNetworkEmbedder.apply(tree); NodeArray<Node> srcTarMap = tree.newNodeArray()) {
				var xMin = (count < 3 ? count : count - 3) * (50 + width);
				var xMax = xMin + width;
				var yMin = (count < 3 ? 50 : 100 + width);
				var yMax = yMin + width;
				scaleToBox(nodePointMap, xMin, xMax, yMin, yMax);
				for (var v : tree.nodes()) {
					var shape = new Circle(3);
					shape.setTranslateX(nodePointMap.get(v).getX());
					shape.setTranslateY(nodePointMap.get(v).getY());
					srcTarMap.put(v, view.createNode(shape));
				}
				// need to run this later otherwise labels will be placed incorrectly
				Platform.runLater(()-> {
					for (var v : tree.nodes()) {
						var w = srcTarMap.get(v);
						if (tree.getLabel(v) != null)
							view.createLabel(w, tree.getLabel(v));
					}
				});
				for (var e : tree.edges()) {
					var v = srcTarMap.get(e.getSource());
					var w = srcTarMap.get(e.getTarget());
					var first = view.getPoint(v);
					var last = view.getPoint(w);

					List<Point2D> points;
					if (e.getTarget().getInDegree() >= 2) {
						points = List.of(first, last);
					} else {
						points = switch (rootLocation) {
							case Top, Bottom -> List.of(first, new Point2D(last.getX(), first.getY()), last);
							case Left, Right -> List.of(first, new Point2D(first.getX(), last.getY()), last);
						};
					}
					view.createEdge(v, w, PathUtils.createPath(points, true));
				}
			}
			if (++count == nTrees)
				break;
		}
		var newNodes = IteratorUtils.asSet(view.getGraph().nodes());
		newNodes.removeAll(oldNodes);

		view.getNodeSelection().clearSelection();
		view.getEdgeSelection().clearSelection();
		for (var v : newNodes) {
			view.getNodeSelection().select(v);
			for (var e : v.outEdges())
				view.getEdgeSelection().select(e);
		}
		return newNodes;
	}

	private static void scaleToBox(NodeArray<Point2D> nodePointMap, double xMin, double xMax, double yMin, double yMax) {
		var pxMin = nodePointMap.values().stream().mapToDouble(Point2D::getX).min().orElse(0);
		var pxMax = nodePointMap.values().stream().mapToDouble(Point2D::getX).max().orElse(0);
		var pyMin = nodePointMap.values().stream().mapToDouble(Point2D::getY).min().orElse(0);
		var pyMax = nodePointMap.values().stream().mapToDouble(Point2D::getY).max().orElse(0);

		for (var v : nodePointMap.keySet()) {
			var point = nodePointMap.get(v);
			var px = point.getX();
			var py = point.getY();

			var x = xMin + (px - pxMin) * (xMax - xMin) / (pxMax - pxMin);

			var y = yMin + (py - pyMin) * (yMax - yMin) / (pyMax - pyMin);

			nodePointMap.put(v, new Point2D(x, y));
		}
	}
}

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

package phylosketch.io;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.FileUtils;
import phylosketch.view.DrawPane;
import phylosketch.view.DrawUtils;
import splitstree6.layout.tree.HeightAndAngles;
import splitstree6.layout.tree.LayoutTreeRectangular;

import java.io.BufferedReader;
import java.io.IOException;

public class ImportNewick {
	public static void apply(String fileName, DrawPane view) throws IOException {
		var nTrees = Math.min(6, FileUtils.getNumberOfLinesInFile(fileName));

		var width = 750.0 / (1 + (nTrees > 3 ? 1 : 0));

		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(fileName))) {
			var count = 0;
			while (r.ready()) {
				var line = r.readLine();
				var tree = new PhyloTree();
				tree.parseBracketNotation(line, true);
				tree.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).forEach(e -> tree.setReticulate(e, true));

				try (var nodePointMap = LayoutTreeRectangular.apply(tree, true, HeightAndAngles.Averaging.ChildAverage);
					 NodeArray<Node> srcTarMap = tree.newNodeArray()) {
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
					for (var v : tree.nodes()) {
						var w = srcTarMap.get(v);
						if (tree.getLabel(v) != null)
							view.createLabel(w, tree.getLabel(v));
					}
					for (var e : tree.edges()) {
						var v = srcTarMap.get(e.getSource());
						var w = srcTarMap.get(e.getTarget());
						view.createEdge(v, w, DrawUtils.createPath(nodePointMap.get(e.getSource()), nodePointMap.get(e.getTarget()), 3));
					}
				}
				view.getNodeSelection().selectAll(view.getGraph().getNodesAsList());
				view.getEdgeSelection().selectAll(view.getGraph().getEdgesAsList());
				if (++count == nTrees)
					return;
			}
		}
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

/*
 * ImportNewick.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.graph.Node;
import jloda.phylo.LSAUtils;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.FileUtils;
import jloda.util.IteratorUtils;
import phylosketch.draw.DrawNetwork;
import phylosketch.embed.HeightAndAngles;
import phylosketch.embed.RectangularPhylogenyLayout;
import phylosketch.embed.optimize.OptimizeLayout;
import phylosketch.utils.ScaleUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;

/**
 * newick import
 * Daniel Huson, 8.2024
 */
public class ImportNewick {
	/**
	 * import from file
	 *
	 * @param fileName file
	 * @param view     the window to import into
	 * @throws IOException
	 */
	public static void apply(String fileName, DrawView view) throws IOException {
		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(fileName))) {
			apply(r, view);
		}
	}

	/**
	 * import from a buffered reader
	 *
	 * @param r    reader
	 * @param view window
	 * @return set of new nodes
	 * @throws IOException
	 */
	public static Collection<Node> apply(BufferedReader r, DrawView view) throws IOException {
		var rootSide = RootPosition.Side.Left;

		view.applyCss();

		final var width = 300;
		var gap = 50.0;
		var totalWidth = Math.max(width + gap, view.getWidth() - gap);
		var totalHeight = Math.max(width + gap, view.getHeight() - gap);
		var xMin = gap;
		var yMin = gap;

		var originalNodes = IteratorUtils.asSet(view.getGraph().nodes());

		while (r.ready()) {
			var line = r.readLine();
			if (line == null)
				break;
			if (!line.isBlank() && line.trim().startsWith("(")) {
				var tree = new PhyloTree();
				(new NewickIO()).parseBracketNotation(tree, line, true, false);
				LSAUtils.setLSAChildrenAndTransfersMap(tree);

				var hasWeights = tree.hasEdgeWeights() && tree.edgeStream().anyMatch(e -> tree.getWeight(e) != 1.0 && tree.getWeight(e) != 0.0);

				try (var points = RectangularPhylogenyLayout.apply(tree, hasWeights, HeightAndAngles.Averaging.ChildAverage)) {
					if (true) {
						OptimizeLayout.optimizeOrdering(tree, points, new Random(666));
					}

					var height = Math.min(width, tree.nodeStream().filter(Node::isLeaf).count() * 20);

					if (yMin + gap + height > totalHeight) {
						yMin = gap;
						xMin += width + gap;
						if (xMin + gap + height >= totalWidth)
							break;
					}

					var xMax = xMin + width;
					var yMax = yMin + height;
					ScaleUtils.scaleToBox(points, xMin, xMax, yMin, yMax);
					yMin += height + gap;
					DrawNetwork.apply(view, tree, points);
				}
			}
		}
		var newNodes = IteratorUtils.asSet(view.getGraph().nodes());
		newNodes.removeAll(originalNodes);

		view.getNodeSelection().clearSelection();
		view.getEdgeSelection().clearSelection();
		for (var v : newNodes) {
			view.getNodeSelection().select(v);
			for (var e : v.outEdges())
				view.getEdgeSelection().select(e);
		}
		return newNodes;
	}
}

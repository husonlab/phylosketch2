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

import javafx.application.Platform;
import javafx.geometry.Point2D;
import jloda.fx.util.ProgramProperties;
import jloda.fx.windownotifications.WindowNotifications;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.phylogeny.layout.Averaging;
import jloda.util.FileUtils;
import jloda.util.IteratorUtils;
import phylosketch.draw.DrawNetwork;
import phylosketch.utils.Dialogs;
import phylosketch.utils.LayoutRootedPhylogeny;
import phylosketch.view.DrawView;
import phylosketch.window.MainWindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * newick import
 * Daniel Huson, 8.2024
 */
public class ImportNewick {
	/**
	 * import from file
	 *
	 * @param fileName file
	 * @param window     the window to import into
	 * @throws IOException
	 */
	public static void apply(MainWindow window, String fileName) throws IOException {
		WindowNotifications.showInfo(window.getController().getCenterAnchorPane(), "Importing from file " + FileUtils.getFileNameWithoutPath(fileName));

		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(fileName))) {
			apply(r, window);
		}
	}

	/**
	 * import from a buffered reader
	 *
	 * @param r    reader
	 * @param window window
	 * @return set of new nodes
	 * @throws IOException
	 */
	public static Collection<Node> apply(BufferedReader r, MainWindow window) throws IOException {
		var view = window.getDrawView();
		view.applyCss();

		var clean = (view.getGraph().getNumberOfNodes() == 0);
		if (clean)
			view.setLayout(jloda.phylogeny.layout.LayoutRootedPhylogeny.Layout.Rectangular);

		var originalNodes = IteratorUtils.asSet(view.getGraph().nodes());

		final var taxonGap = 20;
		final var vGap = 50.0;
		final var xMin = 50.0;
		final var minWidth = 300;

		double yMin;
		if (originalNodes.isEmpty())
			yMin = vGap;
		else {
			var world = window.getDrawView().getWorld();
			var contentBounds = world.getLayoutBounds();
			yMin = contentBounds.getHeight() + vGap;
		}

		var trees = new ArrayList<PhyloTree>();
		var totalLeaves = 0L;
		while (r.ready()) {
			var line = r.readLine();
			if (line == null)
				break;
			if (!line.isBlank() && line.trim().startsWith("(")) {
				var tree = new PhyloTree();
				(new NewickIO()).parseBracketNotation(tree, line, true, false);
				trees.add(tree);
				totalLeaves += tree.nodeStream().filter(Node::isLeaf).count();

			}
		}

		if (totalLeaves > 1000 && ProgramProperties.isDesktop()) { // todo: convert to internal dialog
			var message = "You are attempting to import %d %s with %d leaves"
					.formatted(trees.size(), trees.size() > 1 ? "phylogenies" : "phylogeny", totalLeaves);
			if (!Dialogs.askForConfirmation(window.getStage(), "Phylogeny import", message, "Proceed?")) {
				return Collections.emptyList();
			}
		}

		for (var tree : trees) {
				LSAUtils.setLSAChildrenAndTransfersMap(tree);

			try (NodeArray<Point2D> points = tree.newNodeArray()) {
				LayoutRootedPhylogeny.apply(tree, view.getLayout(), view.getScaling(), Averaging.LeafAverage, true, new Random(666), new HashMap<>(), points);
				var numberOfLeaves = tree.nodeStream().filter(Node::isLeaf).count();
				var phylogenyHeight = numberOfLeaves * taxonGap;
				var phylogenyWidth = Math.min(numberOfLeaves * taxonGap, Math.max(minWidth, view.getWidth() - xMin));

				LayoutRootedPhylogeny.scaleToBox(points, xMin, xMin + phylogenyWidth, yMin, yMin + phylogenyHeight);
				yMin += phylogenyHeight + vGap;
					DrawNetwork.apply(view, tree, points, view.getLayout());
				}
			}

		var newNodes = IteratorUtils.asSet(view.getGraph().nodes());
		newNodes.removeAll(originalNodes);

		if (!clean) {
			view.getNodeSelection().clearSelection();
			view.getEdgeSelection().clearSelection();
			for (var v : newNodes) {
				view.getNodeSelection().select(v);
				for (var e : v.outEdges())
					view.getEdgeSelection().select(e);
			}
		}
		Platform.runLater(() -> WindowNotifications.showInfo(window.getController().getCenterAnchorPane(), "Imported %d phylogenies".formatted(trees.size())));
		return newNodes;
	}

	/**
	 * import from a string into a given bounding box
	 *
	 * @param newick input line
	 * @param view   window
	 * @return set of new nodes
	 * @throws IOException
	 */
	public static Collection<Node> apply(String newick, DrawView view, double xMin, double yMin, double xMax, double yMax) throws IOException {
		view.applyCss();

		var originalNodes = IteratorUtils.asSet(view.getGraph().nodes());

		var tree = new PhyloTree();
		(new NewickIO()).parseBracketNotation(tree, newick, true, false);
		LSAUtils.setLSAChildrenAndTransfersMap(tree);

		try (var nodeAngleMap = tree.newNodeDoubleArray(); NodeArray<Point2D> nodePointMap = tree.newNodeArray()) {
			LayoutRootedPhylogeny.apply(tree, view.getLayout(), view.getScaling(), Averaging.LeafAverage, tree.getNumberOfTaxa() <= 100, new Random(666), nodeAngleMap, nodePointMap);
			LayoutRootedPhylogeny.scaleToBox(nodePointMap, xMin, xMax, yMin, yMax);
			DrawNetwork.apply(view, tree, nodePointMap, view.getLayout());
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
}

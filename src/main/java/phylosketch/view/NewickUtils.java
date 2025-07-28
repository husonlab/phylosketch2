/*
 * NewickUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.scene.shape.Shape;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.Counter;
import phylosketch.io.ReorderChildren;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * utils for generating a Newick string for the view
 * Daniel Huson, 3.2025
 */
public class NewickUtils {
	/**
	 * return the Newick string for selected or all graph components
	 *
	 * @param view the view
	 * @return Newick string
	 */
	public static String toBracketString(DrawView view) {
		return toBracketString(view, 10000000);
	}

	/**
	 * return the Newick string for selected or all graph components
	 *
	 * @param view      the view
	 * @param maxLength the max length of the string, will remove details to fit length
	 * @return Newick string, possibly abbreviated
	 */
	public static String toBracketString(DrawView view, int maxLength) {
		var newickIO = new NewickIO();
		var outputFormat = new NewickIO.OutputFormat(view.getGraph().hasEdgeWeights(), false, view.getGraph().hasEdgeConfidences(), view.getGraph().hasEdgeProbabilities(), false);

		var w = new StringWriter();
		for (var tree : extractAllTrees(view.getGraph())) {
			if (view.getNodeSelection().size() == 0 || tree.nodeStream().map(v -> (Shape) v.getData()).filter(Objects::nonNull).map(s -> (Node) s.getUserData()).anyMatch(view.getNodeSelection()::isSelected)) {
				var root = tree.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
				if (root.isPresent()) {
					tree.setRoot(root.get());
					tree.edgeStream().forEach(f -> tree.setReticulate(f, f.getTarget().getInDegree() > 1));

					ReorderChildren.apply(tree, v -> DrawView.getPoint((Node) v.getInfo()), ReorderChildren.SortBy.Location);

					try {
						var add = new StringWriter();
						newickIO.write(tree, add, outputFormat);
						add.write(";\n");
						if (w.toString().length() + add.toString().length() > maxLength) {
							return w.toString();
						} else w.write(add.toString());
					} catch (IOException ignored) {
					}
				}
			}
		}
		return w.toString();
	}

	/**
	 * maps all connected components to trees
	 *
	 * @param graph the graph
	 * @return the contained trees or rooted networks
	 */
	public static List<PhyloTree> extractAllTrees(PhyloTree graph) {
		var list = new ArrayList<PhyloTree>();

		try (var componentMap = graph.newNodeIntArray();
			 NodeArray<Node> srcTarMap = graph.newNodeArray()) {
			graph.computeConnectedComponents(componentMap);
			for (var component : new TreeSet<>(componentMap.values())) {
				var tree = new PhyloTree();
				var saveRoot = graph.getRoot();
				graph.setRoot(null);
				tree.copy(graph, srcTarMap, null);
				graph.setRoot(saveRoot);
				graph.nodeStream().filter(v -> !Objects.equals(componentMap.get(v), component)).map(srcTarMap::get).forEach(tree::deleteNode);

				try (var nodes = tree.newNodeSet()) {
					nodes.addAll(tree.nodes());
					srcTarMap.entrySet().stream().filter(entry -> nodes.contains(entry.getValue())).forEach(entry -> tree.setInfo(entry.getValue(), entry.getKey()));
				}

				var roots = tree.nodeStream().filter(v -> v.getInDegree() == 0).toList();
				if (roots.size() == 1) {
					tree.setRoot(roots.get(0));
					list.add(tree);
				} else if (roots.size() > 1) {
					var root = tree.newNode();
					for (var v : roots) {
						var e = tree.newEdge(root, v);
						tree.setWeight(e, 0);
					}
					tree.setRoot(root);
					list.add(tree);
				}
				var unnamed = new Counter(0);
				tree.postorderTraversal(v -> {
					if (v.isLeaf() && tree.getLabel(v) == null)
						tree.setLabel(v, "Unnamed-" + unnamed.incrementAndGet());
				});
				srcTarMap.clear();
			}
		}
		return list;
	}

}

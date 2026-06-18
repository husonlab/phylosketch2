/*
 * SampleTreesFromNetwork.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.networktrees;

import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import jloda.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class SampleTreesFromNetwork {
	/**
	 * for a given network, randomly extracts the given number of trees
	 *
	 * @param network       network
	 * @param numberOfTrees trees
	 * @return list of trees and edge mappings from tree edges to paths in network
	 */
	public static ArrayList<TreeAndEdgeMapping> apply(PhyloTree network, int numberOfTrees) {
		return apply(network, numberOfTrees, 666L);
	}

	public static ArrayList<TreeAndEdgeMapping> apply(PhyloTree network, int numberOfTrees, long randomSeed) {
		var random = new Random(randomSeed);

		var results = new ArrayList<TreeAndEdgeMapping>();
		for (var i = 0; i < numberOfTrees; i++) {
			var tree = new PhyloTree();
			var edgeToPathMap = new TreeMap<Edge, ArrayList<Edge>>();
			try (EdgeArray<Edge> oldEdge2NewEdge = network.newEdgeArray()) {
				tree.copy(network, null, oldEdge2NewEdge);
				for (var networkEdge : network.edges()) {
					var treeEdge = oldEdge2NewEdge.get(networkEdge);
					edgeToPathMap.put(treeEdge, new ArrayList<>(List.of(networkEdge)));
				}
			}
			for (var v : tree.nodeStream().filter(v -> v.getInDegree() > 1).toList()) {
				var edges = IteratorUtils.asList(v.inEdges());
				if (!edges.isEmpty()) {
					var keep = edges.get(edges.size() - 1);
					if (tree.hasEdgeProbabilities() && isApproximatelyOne(edges.stream().mapToDouble(tree::getProbability).sum())) {
						var x = random.nextDouble();
						var total = 0.0;
						for (var e : edges) {
							total += tree.getProbability(e);
							if (x <= total) {
								keep = e;
								break;
							}
						}
					} else if (edges.size() > 1) {
						keep = edges.get(random.nextInt(v.getInDegree()));
					}
					for (var e : edges) {
						if (e != keep) {
							edgeToPathMap.remove(e);
							tree.deleteEdge(e);
						}
					}
				}
			}
			var thruVertices = tree.nodeStream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1).toList();
			for (var v : thruVertices) {
				var inEdge = v.getFirstInEdge();
				var p = inEdge.getSource();
				var outEdge = v.getFirstOutEdge();
				var q = outEdge.getTarget();
				var newEdge = tree.newEdge(p, q);
				if (tree.hasEdgeWeights())
					tree.setWeight(newEdge, tree.getWeight(inEdge) + tree.getWeight(outEdge));
				if (tree.hasEdgeConfidences())
					tree.setConfidence(newEdge, 0.5 * (tree.getConfidence(inEdge) + tree.getConfidence(outEdge)));
				var list = new ArrayList<>(CollectionUtils.concatenate(edgeToPathMap.get(inEdge), edgeToPathMap.get(outEdge)));
				edgeToPathMap.put(newEdge, list);
				edgeToPathMap.remove(inEdge);
				edgeToPathMap.remove(outEdge);
				tree.deleteEdge(inEdge);
				tree.deleteEdge(outEdge);
			}
			tree.getReticulateEdges().clear();
			results.add(new TreeAndEdgeMapping(tree, edgeToPathMap));
		}
		return results;
	}

	public record TreeAndEdgeMapping(PhyloTree tree, Map<Edge, ArrayList<Edge>> edgeToPathMap) {
	}

	public static void main(String[] args) throws IOException {
		var network = new PhyloTree();
		network.parseBracketNotation("((A,(B,(C)#H1)),(#H1,(D,E)));", true);

		var newickIO = new NewickIO();


		{
			for (var e : network.edges()) {
				network.setLabel(e, "" + e.getId());
			}
			{
				System.err.println("network:");
				var w = new StringWriter();
				newickIO.write(network, w, false, true);
				System.err.println(w + ";");
			}
		}


		for (var pair : apply(network, 5, 666)) {
			var tree = pair.tree();
			for (var e : tree.edges()) {
				tree.setLabel(e, "" + e.getId());
			}
			System.err.println("tree:");
			var w = new StringWriter();
			newickIO.write(pair.tree(), w, false, true);
			System.err.println(w + ";");

			for (var e : tree.edges()) {
				System.err.println(e.getId() + " -> " + StringUtils.toString(pair.edgeToPathMap().get(e).stream().map(f -> f.getId()).toArray(), ","));
			}
		}
	}

	public static boolean isApproximatelyOne(double value) {
		double epsilon = 1e-9; // tolerance level
		return Math.abs(value - 1.0) < epsilon;
	}
}

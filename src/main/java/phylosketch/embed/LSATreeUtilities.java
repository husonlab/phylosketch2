/*
 * LSATreeUtilities.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.embed;

import jloda.graph.*;
import jloda.graph.algorithms.BiconnectedComponents;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LSA utilities
 * Daniel Huson, 7.2007
 */
public class LSATreeUtilities {
	/**
	 * given a reticulate network, returns a mapping of each node to a list of its children in the LSA getTree
	 */
	public static NodeArray<Node> computeLSAOrdering(PhyloTree tree) {
		NodeArray<Node> reticulation2LSA = new NodeArray<>(tree);
		computeLSAOrdering(tree, reticulation2LSA);
		return reticulation2LSA;
	}


	/**
	 * given a rooted network, returns a mapping of each node to a list of its children in the LSA getTree
	 *
	 * @param reticulation2LSA is returned here
	 */
	public static void computeLSAOrdering(PhyloTree tree, NodeArray<Node> reticulation2LSA) {
		computeLSAOrdering(tree, reticulation2LSA, tree.getLSAChildrenMap());
	}

	/**
	 * given a rooted network, returns a mapping of each node to a list of its children in the LSA getTree
	 *
	 * @param reticulation2LSA is returned here
	 */
	public static void computeLSAOrdering(PhyloTree tree, NodeArray<Node> reticulation2LSA, NodeArray<List<Node>> node2LSAChildren) {
		node2LSAChildren.clear();

		if (tree.getRoot() != null) {
			// first we compute the reticulate node to lsa node mapping:
			computeReticulation2LSA(tree, reticulation2LSA, null);

			for (var v : tree.nodes()) {
				var lsaChildren = v.outEdgesStream(false).filter(e -> !tree.isReticulateEdge(e)).map(Edge::getTarget).collect(Collectors.toList());
				node2LSAChildren.put(v, lsaChildren);
			}
			for (var v : tree.nodes()) {
				var lsa = reticulation2LSA.get(v);
				if (lsa != null)
					node2LSAChildren.get(lsa).add(v);
			}
		}
	}

	/**
	 * compute the reticulate-node to lsa-node mapping
	 */
	public static void computeReticulation2LSA(PhyloTree network, NodeArray<Node> reticulation2LSA, NodeDoubleArray reticulation2LSAEdgeLength) {
		reticulation2LSA.clear();
		var reticulations = network.nodeStream().filter(v -> v.getInDegree() > 1).toList();
		var components = BiconnectedComponents.apply(network);
		for (var component : components) {
			var intersection = CollectionUtils.intersection(component, reticulations);
			if (!intersection.isEmpty()) {
				var lsa = component.stream().filter(v -> v.getInDegree() == 0 || !component.containsAll(IteratorUtils.asSet(v.parents()))).findAny();
				lsa.ifPresent(node -> intersection.stream().filter(v -> v != node).forEach(v -> reticulation2LSA.put(v, node)));
			}
		}

		if (reticulation2LSAEdgeLength != null) {
			try (NodeArray<Set<Node>> node2ReticulatesBelow = network.newNodeArray()) {
				network.postorderTraversal(v -> {
					var set = node2ReticulatesBelow.computeIfAbsent(v, k -> new HashSet<>());
					if (v.getInDegree() > 1)
						set.add(v);
					for (var w : v.children()) {
						set.addAll(node2ReticulatesBelow.get(w));
					}
				});
				try (NodeArray<Map<Node, Double>> ret2Node2Length = network.newNodeArray(); var ret2length = network.newNodeDoubleArray()) {
					network.nodeStream().filter(v -> v.getInDegree() > 1).forEach(v -> ret2Node2Length.put(v, new HashMap<>()));
					computeReticulation2LSAEdgeLengthRec(network, network.getRoot(), reticulation2LSA, ret2length, ret2Node2Length, node2ReticulatesBelow, network.newNodeSet());
				}
			}
		}
	}

	/**
	 * recursively does the work
	 */
	private static void computeReticulation2LSAEdgeLengthRec(PhyloTree tree, Node v, NodeArray<Node> reticulation2LSA, NodeDoubleArray ret2length, NodeArray<Map<Node, Double>> ret2Node2Length, NodeArray<Set<Node>> node2ReticulatesBelow, NodeSet visited) {
		if (!visited.contains(v)) {
			visited.add(v);

			var reticulatesBelow = new HashSet<Node>();

			for (var f : v.outEdges()) {
				computeReticulation2LSAEdgeLengthRec(tree, f.getTarget(), reticulation2LSA, ret2length, ret2Node2Length, node2ReticulatesBelow, visited);
				reticulatesBelow.addAll(node2ReticulatesBelow.get(f.getTarget()));
			}

			reticulatesBelow.removeAll(node2ReticulatesBelow.get(v)); // because reticulations mentioned here don't have v as LSA

			for (var r : reticulatesBelow) {
				var node2Dist = ret2Node2Length.get(r);
				double length = 0.0;
				for (var f : v.outEdges()) {
					var w = f.getTarget();
					length += node2Dist.get(w);
					if (!tree.isReticulateEdge(f))
						length += tree.getWeight(f);
				}
				if (v.getOutDegree() > 0)
					length /= v.getOutDegree();
				node2Dist.put(v, length);
				if (reticulation2LSA.get(r) == v)
					ret2length.put(r, length);
			}
		}
	}

	/**
	 * given a rooted phylogenetic network, returns the LSA getTree
	 *
	 * @return LSA getTree
	 */
	public static PhyloTree computeLSA(PhyloTree network) {
		var tree = new PhyloTree(network);

		if (tree.getRoot() != null) {
			// first we compute the reticulate node to lsa node mapping:
			NodeArray<Node> reticulation2LSA = new NodeArray<>(tree);
			var reticulation2LSAEdgeLength = tree.newNodeDoubleArray();
			computeReticulation2LSA(tree, reticulation2LSA, reticulation2LSAEdgeLength);

			// check that all reticulation nodes have a LSA:
			for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
				if (v.getInDegree() >= 2) {
					Node lsa = reticulation2LSA.get(v);
					if (lsa == null)
						System.err.println("WARNING: no LSA found for node: " + v);
				}
			}

			var toDelete = new LinkedList<Edge>();
			for (var v = tree.getFirstNode(); v != null; v = v.getNext()) {
				var lsa = reticulation2LSA.get(v);

				if (lsa != null) {
					for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e))
						toDelete.add(e);
					Edge e = tree.newEdge(lsa, v);
					tree.setWeight(e, reticulation2LSAEdgeLength.get(v));
					// System.err.println("WEIGHT: " + (float) reticulation2LSAEdgeLength.get(v));
					// getTree.setLabel(v,getTree.getLabel(v)!=null?getTree.getLabel(v)+"/"+(float)getTree.getWeight(e):""+(float)getTree.getWeight(e));
				}
			}
			for (var e : toDelete)
				tree.deleteEdge(e);

			var changed = true;
			while (changed) {
				changed = false;
				var falseLeaves = new LinkedList<Node>();
				for (var v = tree.getFirstNode(); v != null; v = v.getNext()) {
					if (v.getInDegree() == 1 && v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).isEmpty()))
						falseLeaves.add(v);
				}
				if (!falseLeaves.isEmpty()) {
					for (var u : falseLeaves)
						tree.deleteNode(u);
					changed = true;
				}

				var divertices = new LinkedList<Node>();
				for (var v = tree.getFirstNode(); v != null; v = v.getNext()) {
					if (v.getInDegree() == 1 && v.getOutDegree() == 1 && v != tree.getRoot() && (tree.getLabel(v) == null || tree.getLabel(v).isEmpty()))
						divertices.add(v);
				}
				if (!divertices.isEmpty()) {
					for (var u : divertices)
						tree.delDivertex(u);
					changed = true;
				}
			}
		}

		// make sure special attribute is set correctly:
		for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
			boolean shouldBe = e.getTarget().getInDegree() > 1;
			if (shouldBe != tree.isReticulateEdge(e)) {
				System.err.println("WARNING: bad special state, fixing (to: " + shouldBe + ") for e=" + e);
				tree.setReticulate(e, shouldBe);
			}
		}
		// making sure leaves have labels:

		for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
			{
				if (v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).trim().isEmpty())) {
					System.err.println("WARNING: adding label to naked leaf: " + v);
					tree.setLabel(v, "V" + v.getId());
				}
			}
		}
		//System.err.println("getTree: " + getTree.toBracketString());
		return tree;
	}
}
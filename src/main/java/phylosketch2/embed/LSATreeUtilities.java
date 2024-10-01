/*
 * LSATreeUtilities.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch2.embed;

import jloda.graph.*;
import jloda.phylo.PhyloTree;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LSA utilities
 * Daniel Huson, 7.2007
 */
public class LSATreeUtilities {
	/**
	 * given a reticulate network, returns a mapping of each node to a list of its children in the LSA tree
	 */
	public static NodeArray<Node> computeLSAOrdering(PhyloTree tree) {
		NodeArray<Node> reticulation2LSA = new NodeArray<>(tree);
		computeLSAOrdering(tree, reticulation2LSA);
		return reticulation2LSA;
	}


	/**
	 * given a rooted network, returns a mapping of each node to a list of its children in the LSA tree
	 *
	 * @param reticulation2LSA is returned here
	 */
	public static void computeLSAOrdering(PhyloTree tree, NodeArray<Node> reticulation2LSA) {
		computeLSAOrdering(tree, reticulation2LSA, tree.getLSAChildrenMap());
	}

	/**
	 * given a rooted network, returns a mapping of each node to a list of its children in the LSA tree
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
	 * compute the reticulate node to lsa node mapping
	 */
	public static void computeReticulation2LSA(PhyloTree network, NodeArray<Node> reticulation2LSA, NodeDoubleArray reticulation2LSAEdgeLength) {
		reticulation2LSA.clear();
		var ret2PathSet = new NodeArray<BitSet>(network);
		var ret2Edge2PathSet = new NodeArray<Map<Edge, BitSet>>(network);
		var node2below = new NodeArray<Set<Node>>(network); // set of reticulation nodes below a given node

		computeReticulation2LSARec(network.getRoot(), ret2PathSet, ret2Edge2PathSet, node2below, reticulation2LSA);

		if (reticulation2LSAEdgeLength != null) {
			var ret2Node2Length = new NodeArray<Map<Node, Double>>(network);
			var ret2length = new NodeDoubleArray(network);

			network.nodeStream().filter(v -> v.getInDegree() > 1).forEach(v -> ret2Node2Length.put(v, new HashMap<>()));

			computeReticulation2LSAEdgeLengthRec(network, network.getRoot(), reticulation2LSA, ret2length, ret2Node2Length, node2below, network.newNodeSet());
		}

	}

	/**
	 * recursively compute the mapping of reticulate nodes to their lsa nodes
	 */
	private static void computeReticulation2LSARec(Node v, NodeArray<BitSet> ret2PathSet, NodeArray<Map<Edge, BitSet>> ret2Edge2PathSet, NodeArray<Set<Node>> node2below, NodeArray<Node> reticulation2LSA) {
		if (v.getInDegree() > 1) // this is a reticulate node, add paths to node and incoming edges
		{
			// setup new paths for this node:
			Map<Edge, BitSet> edge2PathSet = new HashMap<>();
			ret2Edge2PathSet.put(v, edge2PathSet);
			BitSet pathsForR = new BitSet();
			ret2PathSet.put(v, pathsForR);
			//  assign a different path number to each in-edge:
			int pathNum = 0;
			for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
				pathNum++;
				pathsForR.set(pathNum);
				BitSet pathsForEdge = new BitSet();
				pathsForEdge.set(pathNum);
				edge2PathSet.put(e, pathsForEdge);
			}
		}

		var reticulationsBelow = new HashSet<Node>(); // set of all reticulate nodes below v
		node2below.put(v, reticulationsBelow);

		// visit all children and determine all reticulations below this node
		for (var f : v.outEdges()) {
			var w = f.getTarget();
			if (node2below.get(w) == null) // if haven't processed child yet, do it:
				computeReticulation2LSARec(w, ret2PathSet, ret2Edge2PathSet, node2below, reticulation2LSA);
			reticulationsBelow.addAll(node2below.get(w));
			if (w.getInDegree() > 1)
				reticulationsBelow.add(w);
		}

		// check whether this is the lsa for any of the reticulations below v
		// look at all reticulations below v:
		var toDelete = new ArrayList<Node>();
		for (var r : reticulationsBelow) {
			// determine which paths from the reticulation lead to this node
			var edge2PathSet = ret2Edge2PathSet.get(r);
			var paths = new BitSet();
			for (Edge f : v.outEdges()) {
				var eSet = edge2PathSet.get(f);
				if (eSet != null)
					paths.or(eSet);
			}
			BitSet alive = ret2PathSet.get(r);
			if (paths.equals(alive)) // if the set of paths equals all alive paths, v is lsa of r
			{
				reticulation2LSA.put(r, v);
				toDelete.add(r); // don't need to consider this reticulation any more
			}
		}
		// don't need to consider reticulations for which lsa has been found:
		for (Node u : toDelete)
			reticulationsBelow.remove(u);

		// all paths are pulled up the first in-edge"
		if (v.getInDegree() >= 1) {
			for (Node r : reticulationsBelow) {
				// determine which paths from the reticulation lead to this node
				var edge2PathSet = ret2Edge2PathSet.get(r);

				var newSet = new BitSet();

				for (var e : v.outEdges()) {
					var pathSet = edge2PathSet.get(e);
					if (pathSet != null)
						newSet.or(pathSet);
				}
				edge2PathSet.put(v.getFirstInEdge(), newSet);
			}
		}
		// open new paths on all additional in-edges:
		if (v.getInDegree() >= 2) {
			for (Node r : reticulationsBelow) {
				var existingPathsForR = ret2PathSet.get(r);

				var edge2PathSet = ret2Edge2PathSet.get(r);
				// start with the second in edge:
				var first = true;
				for (var e : v.inEdges()) {
					if (first)
						first = false;
					else {
						var pathsForEdge = new BitSet();
						var pathNum = existingPathsForR.nextClearBit(1);
						existingPathsForR.set(pathNum);
						pathsForEdge.set(pathNum);
						edge2PathSet.put(e, pathsForEdge);
					}
				}
			}
		}
	}

	/**
	 * recursively does the work
	 */
	private static void computeReticulation2LSAEdgeLengthRec(PhyloTree tree, Node v, NodeArray<Node> reticulation2LSA, NodeDoubleArray ret2length, NodeArray<Map<Node, Double>> ret2Node2Length, NodeArray<Set<Node>> node2below, NodeSet visited) {
		if (!visited.contains(v)) {
			visited.add(v);

			var reticulationsBelow = new HashSet<Node>();

			for (var f : v.outEdges()) {
				computeReticulation2LSAEdgeLengthRec(tree, f.getTarget(), reticulation2LSA, ret2length, ret2Node2Length, node2below, visited);
				reticulationsBelow.addAll(node2below.get(f.getTarget()));
			}

			reticulationsBelow.removeAll(node2below.get(v)); // because reticulations mentioned here don't hve v as LSA

			for (Node r : reticulationsBelow) {
				var node2Dist = ret2Node2Length.get(r);
				double length = 0;
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
	 * given a rooted phylogenetic network, returns the LSA tree
	 *
	 * @return LSA tree
	 */
	public static PhyloTree computeLSA(PhyloTree network) {
		PhyloTree tree = (PhyloTree) network.clone();

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

			List<Edge> toDelete = new LinkedList<>();
			for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
				Node lsa = reticulation2LSA.get(v);

				if (lsa != null) {
					for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e))
						toDelete.add(e);
					Edge e = tree.newEdge(lsa, v);
					tree.setWeight(e, reticulation2LSAEdgeLength.get(v));
					// System.err.println("WEIGHT: " + (float) reticulation2LSAEdgeLength.get(v));
					// tree.setLabel(v,tree.getLabel(v)!=null?tree.getLabel(v)+"/"+(float)tree.getWeight(e):""+(float)tree.getWeight(e));
				}
			}
			for (Edge e : toDelete)
				tree.deleteEdge(e);

			boolean changed = true;
			while (changed) {
				changed = false;
				List<Node> falseLeaves = new LinkedList<>();
				for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
					if (v.getInDegree() == 1 && v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
						falseLeaves.add(v);
				}
				if (falseLeaves.size() > 0) {
					for (Node u : falseLeaves)
						tree.deleteNode(u);
					changed = true;
				}

				List<Node> divertices = new LinkedList<>();
				for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
					if (v.getInDegree() == 1 && v.getOutDegree() == 1 && v != tree.getRoot() && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
						divertices.add(v);
				}
				if (divertices.size() > 0) {
					for (Node u : divertices)
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
				if (v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).trim().length() == 0)) {
					System.err.println("WARNING: adding label to naked leaf: " + v);
					tree.setLabel(v, "V" + v.getId());
				}
			}
		}
		//System.err.println("tree: " + tree.toBracketString());
		return tree;
	}
}
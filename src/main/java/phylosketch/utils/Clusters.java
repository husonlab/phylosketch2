/*
 * Clusters.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.utils;

import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.PhyloTree;
import jloda.util.BitSetUtils;
import jloda.util.StringUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.TreeSet;

public class Clusters {
	public static void show(PhyloTree graph) {
		for (var component : ConnectedComponents.components(graph)) {
			var roots = component.stream().filter(v -> v.getInDegree() == 0).toList();
			if (roots.size() == 1) {
				var nodeClusterMap = new HashMap<Node, BitSet>();
				graph.postorderTraversal(roots.get(0), v -> {
					if (v.isLeaf())
						nodeClusterMap.put(v, BitSetUtils.asBitSet(v.getId()));
					else {
						var cluster = new BitSet();
						for (var w : v.children()) {
							cluster.or(nodeClusterMap.get(w));
						}
						nodeClusterMap.put(v, cluster);
					}
				});

				System.err.println("=============");
				var lines = new ArrayList<String>();
				for (var cluster : nodeClusterMap.values()) {
					var set = new TreeSet<String>();
					for (var id : BitSetUtils.members(cluster)) {
						set.add(graph.getLabel(graph.findNodeById(id)));
					}
					lines.add(StringUtils.toString(set, " "));
				}
				lines.sort(String::compareTo);
				System.err.println(StringUtils.toString(lines, "\n"));
			}
		}
	}
}

/*
 * ArticulationPoints.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.graph.NodeSet;

import java.util.HashMap;
import java.util.Map;

/**
 * compute articulation points using Tarjan's algorithm
 * With the help of ChatGPT
 * Daniel Huson, 11.2024
 */
public class ArticulationPoints {
	private NodeSet articulationPoints;
	private final Map<Node, Integer> disc;
	private final Map<Node, Integer> low;
	private final Map<Node, Node> parent;
	private int time;

	public static NodeSet apply(Graph graph) {
		return (new ArticulationPoints()).findArticulationPoints(graph);
	}

	private ArticulationPoints() {
		disc = new HashMap<>();
		low = new HashMap<>();
		parent = new HashMap<>();
		time = 0;
	}

	private NodeSet findArticulationPoints(Graph graph) {
		articulationPoints = graph.newNodeSet();
		for (Node node : graph.nodes()) {
			disc.put(node, -1);
			low.put(node, -1);
			parent.put(node, null);
		}

		for (var node : graph.nodes()) {
			if (disc.get(node) == -1) {
				dfs(node);
			}
		}

		return articulationPoints;
	}

	private void dfs(Node u) {
		disc.put(u, time);
		low.put(u, time);
		time++;

		int children = 0;

		for (var v : u.adjacentNodes()) {
			if (disc.get(v) == -1) {
				parent.put(v, u);
				children++;
				dfs(v);

				low.put(u, Math.min(low.get(u), low.get(v)));

				if ((parent.get(u) == null && children > 1) ||
					(parent.get(u) != null && low.get(v) >= disc.get(u))) {
					articulationPoints.add(u);
				}

			} else if (!v.equals(parent.get(u))) {
				low.put(u, Math.min(low.get(u), disc.get(v)));
			}
		}
	}
}
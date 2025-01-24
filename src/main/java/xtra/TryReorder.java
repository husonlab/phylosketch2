/*
 * TryReorder.java Copyright (C) 2025 Daniel H. Huson
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

package xtra;

import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;

import java.io.IOException;
import java.util.List;

public class TryReorder {
	public static void main(String[] args) throws IOException {
		var tree = new PhyloTree();
		tree.parseBracketNotation("(a,b,c);", true);
		var root = tree.getRoot();

		System.err.println("original:");
		for (var v : root.children()) {
			System.err.println(v + " " + tree.getLabel(v));
		}

		var list = IteratorUtils.asList(List.of(root.getFirstOutEdge(), root.getLastOutEdge()));
		var reverse = CollectionUtils.reverse(list);

		root.rearrangeAdjacentEdges(reverse);

		System.err.println("reverse:");
		for (var v : root.children()) {
			System.err.println(v + " " + tree.getLabel(v));
		}
	}
}

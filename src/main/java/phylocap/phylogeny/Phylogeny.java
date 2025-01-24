/*
 * Phylogeny.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.phylogeny;

import javafx.geometry.Point2D;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import phylocap.capture.Point;
import phylocap.capture.Segment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Phylogeny {
	private final PhyloTree tree;
	private final NodeArray<Point2D> points;
	private final EdgeArray<List<Point2D>> paths;

	public Phylogeny() {
		tree = new PhyloTree();
		points = tree.newNodeArray();
		paths = tree.newEdgeArray();
	}

	public void clear() {
		tree.clear();
	}

	public Node newNode(Point2D point) {
		var v = tree.newNode();
		points.put(v, point);
		return v;
	}

	public Node newNode(Point point) {
		return newNode(point.point2D());
	}

	public void setLocation(Node v, Point2D point) {
		points.put(v, point);
	}

	public void setLocation(Node v, Point point) {
		points.put(v, point.point2D());
	}

	public Point2D getLocation(Node v) {
		return points.get(v);
	}

	public Edge newEdge(Node v, Node w, Segment segment) {
		var e = tree.newEdge(v, w);
		paths.put(e, segment.point2Ds());
		return e;
	}

	public void setPath(Edge e, Collection<Point2D> path) {
		paths.put(e, new ArrayList<>(path));
	}

	public void setPath(Edge e, Segment segment) {
		paths.put(e, segment.point2Ds());
	}

	public List<Point2D> getPath(Edge e) {
		return paths.get(e);
	}

	public void setLabel(Node v, String label) {
		tree.setLabel(v, label);
	}

	public String getLabel(Node v) {
		return tree.getLabel(v);
	}

	public void reverse(Edge e) {
		e.reverse();
		var path = getPath(e);
		if (path != null) {
			setPath(e, CollectionUtils.reverse(path));
		}
	}

	public PhyloTree getTree() {
		return tree;
	}

	public Node createRoot(Point2D location) {
		var root = tree.newNode();
		setLocation(root, location);
		tree.setRoot(root);
		return root;
	}

	public Node getRoot() {
		return tree.getRoot();
	}
}

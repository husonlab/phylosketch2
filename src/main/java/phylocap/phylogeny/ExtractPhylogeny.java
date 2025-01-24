/*
 * ExtractPhylogeny.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import jloda.util.Pair;
import net.sourceforge.tess4j.Word;
import phylocap.capture.Segment;
import phylosketch.paths.PathUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExtractPhylogeny {

	public static void apply(Phylogeny phylogeny, Point2D rootLocation, List<Segment> segments, List<Word> words) {
		phylogeny.clear();

		if (rootLocation != null) {
			phylogeny.createRoot(rootLocation);

			if (segments.stream().noneMatch(s -> s.first().distance(rootLocation) <= 5 || s.last().distance(rootLocation) <= 5)) {
				var close = segments.stream().filter(s -> s.proximal(rootLocation, 9)).toList();
				if (!close.isEmpty()) {
					var parts = close.get(0).split(rootLocation);
					segments.removeAll(close);
					segments.addAll(parts);
				}
			}

			ArrayList<Segment> sorted;
			{
				var queue = new ArrayList<Pair<Double, Segment>>();
				for (var segment : segments) {
					//var firstDistance = segment.first().distance(rootLocation);
					var firstDistance = segment.first().x() - rootLocation.getX();
					// var lastDistance = segment.last().distance(rootLocation);
					var lastDistance = segment.last().x() - rootLocation.getX();
					if (firstDistance < lastDistance) {
						queue.add(new Pair<>(firstDistance, segment));
					} else {
						queue.add(new Pair<>(lastDistance, segment.reverse()));
					}
				}
				queue.sort(Comparator.comparingDouble(Pair::getFirst));
				sorted = new ArrayList<>(queue.stream().map(Pair::getSecond).toList());
			}

			var other1 = new ArrayList<Segment>();
			var other2 = new ArrayList<Segment>();
			var other3 = new ArrayList<Segment>();
			var other4 = new ArrayList<Segment>();
			for (var round = 0; round <= 4; round++) {
				var list = switch (round) {
					case 0 -> sorted;
					case 1 -> other1;
					case 2 -> other2;
					case 3 -> other3;
					default -> other3;
				};
				var again = switch (round) {
					case 0 -> other1;
					case 1 -> other2;
					case 2 -> other3;
					case 3 -> other4;
					default -> null;
				};
				for (var segment : list) {
					var connected = false;
					for (var v : phylogeny.getTree().nodes()) {
						var p = phylogeny.getLocation(v);
						if (segment.first().distance(p) <= 10) {
							var z = phylogeny.getTree().nodeStream().filter(u -> segment.last().distance(phylogeny.getLocation(u)) <= 10).findAny();
							if (z.isPresent() && v != z.get()) {
								if (!v.isAdjacent(z.get())) {
									phylogeny.newEdge(v, z.get(), segment);
								}
							} else {
								var w = phylogeny.newNode(segment.last());
								phylogeny.newEdge(v, w, segment);
							}
							connected = true;
							break;
						}
					}
					if (!connected) {
						if (round == 0) // try again later
							again.add(segment);
						else if (round == 1) // try again, reversed
							again.add(segment);
						else if (round == 2) // try again, reversed
							again.add(segment.reverse());
						else if (round == 3) // try again, reversed
							again.add(segment);
					}
				}
			}

			if (false)
				removeThruNodes(phylogeny);
			if (false)
				resolveCrossings(phylogeny);

			matchLabels(phylogeny, words);
		}
		System.err.printf("Phylogeny: %d nodes, %d edges%n", phylogeny.getTree().getNumberOfNodes(), phylogeny.getTree().getNumberOfEdges());
	}

	private static void matchLabels(Phylogeny phylogeny, List<Word> words) {
		var leaves = new ArrayList<Pair<Point2D, Node>>();
		for (var v : phylogeny.getTree().leaves()) {
			leaves.add(new Pair<>(phylogeny.getLocation(v), v));
		}

		var labels = new ArrayList<Pair<Point2D, String>>();
		for (var word : words) {
			var bbox = word.getBoundingBox();
			var point = new Point2D(bbox.getX() + 0.5 * bbox.getWidth(), bbox.getY() + 0.5 * bbox.getHeight());
			labels.add(new Pair<>(point, word.getText()));
		}

		while (!leaves.isEmpty() && !labels.isEmpty()) {
			var bestDistance = 500.0; // not too far away
			Pair<Point2D, Node> bestLeaf = null;
			Pair<Point2D, String> bestLabel = null;

			for (var a : leaves) {
				for (var b : labels) {
					var distance = a.getFirst().distance(b.getFirst());
					if (distance < bestDistance) {
						bestDistance = distance;
						bestLeaf = a;
						bestLabel = b;
					}
				}
			}
			if (bestLeaf != null & bestLabel != null) {
				phylogeny.setLabel(bestLeaf.getSecond(), bestLabel.getSecond());
				leaves.remove(bestLeaf);
				labels.remove(bestLabel);
			} else break;
		}
	}

	private static void removeThruNodes(Phylogeny phylogeny) {
		var tree = phylogeny.getTree();
		var thruNodes = tree.nodeStream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1 && tree.getLabel(v) == null).toList();
		for (var v : thruNodes) {
			var inEdge = v.getFirstInEdge();
			var outEdge = v.getFirstOutEdge();
			var e = tree.newEdge(inEdge.getSource(), outEdge.getTarget());
			phylogeny.setPath(e, CollectionUtils.concatenate(phylogeny.getPath(inEdge), phylogeny.getPath(outEdge)));
			tree.deleteNode(v);
		}
	}

	private static void resolveCrossings(Phylogeny phylogeny) {
		var tree = phylogeny.getTree();
		var crossings = tree.nodeStream().filter(v -> v.getInDegree() == 2 && v.getOutDegree() == 2 && tree.getLabel(v) == null).toList();

		for (var v : crossings) {
			var vPoint = phylogeny.getLocation(v);
			var inEdge1 = v.getFirstInEdge();
			var inPoint1 = getByRelativeIndex(phylogeny.getPath(inEdge1), -10);
			var inEdge2 = v.getLastInEdge();
			var inPoint2 = getByRelativeIndex(phylogeny.getPath(inEdge2), -10);
			var outEdge1 = v.getFirstOutEdge();
			var outPoint1 = getByRelativeIndex(phylogeny.getPath(outEdge1), 10);
			var outEdge2 = v.getLastOutEdge();
			var outPoint2 = getByRelativeIndex(phylogeny.getPath(outEdge2), 10);

			var angleIn1AngleOut1 = GeometryUtilsFX.computeObservedAngle(vPoint, inPoint1, outPoint1);
			var angleIn1AngleOut2 = GeometryUtilsFX.computeObservedAngle(vPoint, inPoint1, outPoint2);

			if (Math.abs(180 - angleIn1AngleOut1) < Math.abs(180 - angleIn1AngleOut2)) {
				var e1 = tree.newEdge(inEdge1.getSource(), outEdge1.getTarget());
				phylogeny.setPath(e1, CollectionUtils.concatenate(phylogeny.getPath(inEdge1), phylogeny.getPath(outEdge1)));
				var e2 = tree.newEdge(inEdge2.getSource(), outEdge2.getTarget());
				phylogeny.setPath(e2, CollectionUtils.concatenate(phylogeny.getPath(inEdge2), phylogeny.getPath(outEdge2)));
				tree.deleteNode(v);
			} else {
				var e1 = tree.newEdge(inEdge1.getSource(), outEdge2.getTarget());
				phylogeny.setPath(e1, CollectionUtils.concatenate(phylogeny.getPath(inEdge1), phylogeny.getPath(outEdge2)));
				var e2 = tree.newEdge(inEdge2.getSource(), outEdge1.getTarget());
				phylogeny.setPath(e2, CollectionUtils.concatenate(phylogeny.getPath(inEdge2), phylogeny.getPath(outEdge1)));
				tree.deleteNode(v);
			}
		}
	}

	/**
	 * get list item by relative index
	 *
	 * @param list  the list
	 * @param index if non-negative, returns list.get(index), else returns list.get(list.size()-index-1)
	 * @return element
	 */
	public static <V> V getByRelativeIndex(List<V> list, int index) {
		if (index < 0) {
			return list.get(Math.min(list.size() - 1, Math.max(0, list.size() - index - 1)));
		} else {
			return list.get(Math.max(0, Math.min(index, list.size() - 1)));
		}
	}

	public static List<? extends Shape> computeNodes(Phylogeny phylogeny) {
		return new ArrayList<>(phylogeny.getTree().nodeStream().map(v -> {
			var circle = new Circle(4);
			circle.setUserData(v);
			circle.setCenterX(phylogeny.getLocation(v).getX());
			circle.setCenterY(phylogeny.getLocation(v).getY());
			circle.setTranslateX(30 * Math.random() - 15);
			circle.setTranslateY(30 * Math.random() - 15);
			circle.setStroke(Color.DARKRED);
			circle.setFill(Color.RED);
			return circle;
		}).toList());
	}

	public static List<? extends Shape> computeEdges(Phylogeny phylogeny) {
		var paths = new ArrayList<Shape>();
		for (var e : phylogeny.getTree().edges()) {
			var points = phylogeny.getPath(e);
			var path = PathUtils.createPath(points, true);
			var color = new Color(1, Math.random(), Math.random(), 1);
			path.setStroke(color);
			path.setTranslateX(30 * Math.random() - 15);
			path.setTranslateY(30 * Math.random() - 15);
			path.setStrokeWidth(10 - 5 * Math.random());
			paths.add(path);
			var text = new Text("" + e.getId());
			text.setTranslateX(0.5 * (points.get(0).getX() + points.get(points.size() - 1).getX()));
			text.setTranslateY(0.5 * (points.get(0).getY() + points.get(points.size() - 1).getY()));
			text.setStroke(color);
			paths.add(text);
		}
		;
		return paths;
	}

	public static List<RichTextLabel> computeLabels(Phylogeny phylogeny) {
		return new ArrayList<>(phylogeny.getTree().nodeStream().filter(v -> v.isLeaf() && phylogeny.getTree().getLabel(v) != null)
				.map(v -> {
					var label = new RichTextLabel(phylogeny.getTree().getLabel(v));
					label.setFontSize(24);
					label.setTranslateX(phylogeny.getLocation(v).getX() + 10);
					label.setTranslateY(phylogeny.getLocation(v).getY() - 16);
					label.setTextFill(Color.RED);
					return label;
				}).toList());
	}
}

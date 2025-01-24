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

package phylosketch.capture;

import javafx.geometry.Point2D;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import jloda.util.Pair;
import jloda.util.SetUtils;
import net.sourceforge.tess4j.Word;
import phylocap.capture.Segment;
import phylosketch.commands.*;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;
import phylosketch.view.RootLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import static phylocap.capture.DrawUtils.createPath;

public class ExtractPhylogeny {

	public static void apply(DrawPane view, Point2D rootPosition, List<Segment> segments, List<Word> words) {
		var originalNodes = IteratorUtils.asList(view.getGraph().nodes());
		var originalEdges = IteratorUtils.asSet(view.getGraph().edges());

		RootLocation rootLocation = determineRootLocation(rootPosition, segments);

		BiFunction<Point2D, Point2D, Double> distance = switch (rootLocation) {
			case Left, Right -> (a, b) -> Math.abs(a.getX() - b.getX());
			case Top, Bottom -> (a, b) -> Math.abs(a.getY() - b.getY());
			case Center -> Point2D::distance;
		};

		var commands = new ArrayList<UndoableRedoableCommand>();

		if (rootPosition != null) {
			if (view.getGraph().nodeStream().noneMatch(v -> view.getPoint(v).distance(rootPosition) <= 3)) {
				if (!doThenAdd(new CreateNodeCommand(view, rootPosition), commands))
					return; // failed to add root node
			}

			if (segments.stream().noneMatch(s -> s.first().distance(rootPosition) <= 5 || s.last().distance(rootPosition) <= 5)) {
				var close = segments.stream().filter(s -> s.proximal(rootPosition, 9)).toList();
				if (!close.isEmpty()) {
					var parts = close.get(0).split(rootPosition);
					segments.remove(close.get(0));
					segments.addAll(parts);
				}
			}

			ArrayList<Segment> sorted;
			{
				var queue = new ArrayList<Pair<Double, Segment>>();
				for (var segment : segments) {
					//var firstDistance = segment.first().distance(rootLocation);
					var firstDistance = distance.apply(segment.first().point2D(), rootPosition);
					// var lastDistance = segment.last().distance(rootLocation);
					var lastDistance = distance.apply(segment.last().point2D(), rootPosition);
					if (firstDistance < lastDistance) {
						queue.add(new Pair<>(firstDistance, segment));
					} else {
						queue.add(new Pair<>(lastDistance, segment.reverse()));
					}
				}
				queue.sort(Comparator.comparingDouble(Pair::getFirst));
				sorted = new ArrayList<>(queue.stream().map(Pair::getSecond).toList());
			}

			if (true) {
				for (var segment : sorted) {
					var path = createPath(segment);
					doThenAdd(new DrawEdgeCommand(view, path), commands);
				}
			}

			if (false) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				doThenAdd(new RemoveThruNodesCommand(view, nodes), commands);
			}
			if (false) {
				// todo: make this into a command
				resolveCrossings(view);
			}
			if (true) {
				var edges = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().edges()), originalEdges);
				doThenAdd(new SmoothCommand(view, edges), commands);
				doThenAdd(new ShowArrowsCommand(view, edges, true), commands);
			}
			if (true) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				doThenAdd(new LayoutLabelsCommand(view, nodes), commands);
			}


			if (true) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				matchLabels(view, rootLocation, rootPosition, nodes, words, commands);
			}

			view.getUndoManager().add(new CompositeCommand("capture", commands.toArray(new UndoableRedoableCommand[0])));
		}
		System.err.printf("Phylogeny: %d nodes, %d edges%n", view.getGraph().getNumberOfNodes(), view.getGraph().getNumberOfEdges());
	}

	private static boolean doThenAdd(UndoableRedoableCommand command, List<UndoableRedoableCommand> commands) {
		if (command.isRedoable()) {
			command.redo();
			commands.add(command);
			return true;
		} else return false;
	}

	private static RootLocation determineRootLocation(Point2D rootPosition, List<Segment> segments) {
		var linesRight = segments.stream().filter(seg -> seg.first().x() > rootPosition.getX()).count();
		var linesLeft = segments.stream().filter(seg -> seg.first().x() < rootPosition.getX()).count();
		var linesAbove = segments.stream().filter(seg -> seg.first().y() > rootPosition.getY()).count();
		var linesBelow = segments.stream().filter(seg -> seg.first().y() > rootPosition.getY()).count();

		if (linesRight > linesLeft && linesRight > linesAbove && linesRight > linesBelow)
			return RootLocation.Left;
		else if (linesLeft > linesRight && linesLeft > linesAbove && linesLeft > linesBelow)
			return RootLocation.Right;
		else if (linesAbove > linesLeft && linesAbove > linesRight && linesAbove > linesBelow)
			return RootLocation.Bottom;
		else if (linesBelow > linesLeft && linesBelow > linesRight && linesBelow > linesAbove)
			return RootLocation.Top;
		else
			return RootLocation.Center;
	}

	private static void matchLabels(DrawPane view, RootLocation rootLocation, Point2D rootPosition, List<Node> nodes, List<Word> words, List<UndoableRedoableCommand> commands) {
		var leaves = new ArrayList<Pair<Point2D, Node>>();
		for (var v : nodes) {
			if (v.isLeaf()) {
				leaves.add(new Pair<>(view.getPoint(v), v));
			}
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
					switch (rootLocation) {
						case Left -> {
							if (b.getFirst().getX() < a.getFirst().getX()) continue;
						}
						case Right -> {
							if (b.getFirst().getX() > a.getFirst().getX()) continue;
						}
						case Top -> {
							if (b.getFirst().getY() < a.getFirst().getY()) continue;
						}
						case Bottom -> {
							if (b.getFirst().getY() > a.getFirst().getY()) continue;
						}
						case Center -> {
							if (rootPosition.distance(b.getFirst()) < rootPosition.distance(a.getFirst()))
								continue;
						}
					}
					;
					var distance = a.getFirst().distance(b.getFirst());
					if (distance < bestDistance) {
						bestDistance = distance;
						bestLeaf = a;
						bestLabel = b;
					}
				}
			}
			if (bestLeaf != null & bestLabel != null) {
				doThenAdd(new SetNodeLabelsCommand(view, List.of(bestLeaf.getSecond()), bestLabel.getSecond()), commands);
				leaves.remove(bestLeaf);
				labels.remove(bestLabel);
			} else break;
		}
	}

	private static void resolveCrossings(DrawPane view) {
		var tree = view.getGraph();
		var crossings = tree.nodeStream().filter(v -> v.getInDegree() == 2 && v.getOutDegree() == 2 && tree.getLabel(v) == null).toList();

		for (var v : crossings) {
			var vPoint = view.getPoint(v);
			var inEdge1 = v.getFirstInEdge();
			var inPoint1 = getByRelativeIndex(PathUtils.extractPoints(view.getPath(inEdge1)), -10);
			var inEdge2 = v.getLastInEdge();
			var inPoint2 = getByRelativeIndex(PathUtils.extractPoints(view.getPath(inEdge2)), -10);
			var outEdge1 = v.getFirstOutEdge();
			var outPoint1 = getByRelativeIndex(PathUtils.extractPoints(view.getPath(outEdge1)), 10);
			var outEdge2 = v.getLastOutEdge();
			var outPoint2 = getByRelativeIndex(PathUtils.extractPoints(view.getPath(outEdge2)), 10);

			var angleIn1AngleOut1 = GeometryUtilsFX.computeObservedAngle(vPoint, inPoint1, outPoint1);
			var angleIn1AngleOut2 = GeometryUtilsFX.computeObservedAngle(vPoint, inPoint1, outPoint2);

			if (Math.abs(180 - angleIn1AngleOut1) < Math.abs(180 - angleIn1AngleOut2)) {
				var e1 = tree.newEdge(inEdge1.getSource(), outEdge1.getTarget());
				view.getPath(e1).getElements().setAll(PathUtils.concatenate(view.getPath(inEdge1), view.getPath(outEdge1), true).getElements());
				var e2 = tree.newEdge(inEdge2.getSource(), outEdge2.getTarget());
				view.getPath(e2).getElements().setAll(PathUtils.concatenate(view.getPath(inEdge2), view.getPath(outEdge2), true).getElements());
				tree.deleteNode(v);
			} else {
				var e1 = tree.newEdge(inEdge1.getSource(), outEdge2.getTarget());
				view.getPath(e1).getElements().setAll(PathUtils.concatenate(view.getPath(inEdge1), view.getPath(outEdge2), true).getElements());
				var e2 = tree.newEdge(inEdge2.getSource(), outEdge1.getTarget());
				view.getPath(e2).getElements().setAll(PathUtils.concatenate(view.getPath(inEdge2), view.getPath(outEdge1), true).getElements());
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
}

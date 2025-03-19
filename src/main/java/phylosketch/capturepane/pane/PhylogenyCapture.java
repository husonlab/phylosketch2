/*
 * PhylogenyCapture.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.pane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import jloda.util.Pair;
import phylosketch.capturepane.capture.Segment;
import phylosketch.capturepane.capture.Word;
import phylosketch.commands.*;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import static phylosketch.capturepane.pane.DrawUtils.createPath;

/**
 * capture the phylogeny from paths and labels
 * Daniel Huson, 1.2025
 */
public class PhylogenyCapture {
	private final BooleanProperty removeThruNodes = new SimpleBooleanProperty(this, "removeThruNodes", false);
	private final BooleanProperty fixCrossingEdges = new SimpleBooleanProperty(this, "fixCrossingEdges", false);
	private final BooleanProperty layoutLabels = new SimpleBooleanProperty(this, "layoutLabels", true);
	private final BooleanProperty runReroot = new SimpleBooleanProperty(this, "runReroot", true);
	private final BooleanProperty showArrows = new SimpleBooleanProperty(this, "showArrows", true);
	private final BooleanProperty rescueOrphanLabels = new SimpleBooleanProperty(this, "rescueOrphanLabels", true);

	private final List<Property<?>> parameters = List.of(removeThruNodes, fixCrossingEdges, layoutLabels, runReroot, showArrows, rescueOrphanLabels);

	public UndoableRedoableCommand apply(DrawView view, Point2D rootLocation, List<Segment> segments, List<Word> words) {
		var originalNodes = IteratorUtils.asList(view.getGraph().nodes());
		var originalEdges = IteratorUtils.asSet(view.getGraph().edges());

		var rootPosition = determineRootPosition(rootLocation, segments);

		BiFunction<Point2D, Point2D, Double> distance = switch (rootPosition.side()) {
			case Left, Right -> (a, b) -> Math.abs(a.getX() - b.getX());
			case Top, Bottom -> (a, b) -> Math.abs(a.getY() - b.getY());
			case Center -> Point2D::distance;
		};

		var commands = new ArrayList<UndoableRedoableCommand>();

		Node root = null;

		if (rootLocation != null) {
			if (view.getGraph().nodeStream().noneMatch(v -> DrawView.getPoint(v).distance(rootLocation) <= 3)) {
				if (!doThenAdd(new CreateNodeCommand(view, rootLocation, null), commands))
					return null; // failed to add root node
				root = view.getGraph().getLastNode(); // root last to be created
			}

			if (segments.stream().noneMatch(s -> s.first().distance(rootLocation) <= 5 || s.last().distance(rootLocation) <= 5)) {
				var close = segments.stream().filter(s -> s.proximal(rootLocation, 9)).toList();
				if (!close.isEmpty()) {
					var parts = close.get(0).split(rootLocation);
					segments.remove(close.get(0));
					segments.addAll(parts);
				}
			}

			ArrayList<Segment> sorted;
			{
				var queue = new ArrayList<Pair<Double, Segment>>();
				for (var segment : segments) {
					//var firstDistance = segment.first().distance(rootLocation);
					var firstDistance = distance.apply(segment.first().point2D(), rootLocation);
					// var lastDistance = segment.last().distance(rootLocation);
					var lastDistance = distance.apply(segment.last().point2D(), rootLocation);
					if (firstDistance <= lastDistance) {
						queue.add(new Pair<>(firstDistance, segment));
					} else {
						queue.add(new Pair<>(lastDistance, segment.reverse()));
					}
				}
				queue.sort(Comparator.comparingDouble(Pair::getFirst));
				sorted = new ArrayList<>(queue.stream().map(Pair::getSecond).toList());
			}

			for (var segment : sorted) {
				var path = createPath(segment);
				doThenAdd(new DrawEdgeCommand(view, path), commands);
			}

			if (removeThruNodes.get()) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				doThenAdd(new RemoveThruNodesCommand(view, nodes), commands);
			}
			if (fixCrossingEdges.get()) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				doThenAdd(new FixCrossingEdgesCommand(view, nodes), commands);
			}

			{
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				matchLabels(view, rootPosition, nodes, words, rescueOrphanLabels.get(), commands);
			}

			if (layoutLabels.get()) {
				var nodes = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().nodes()), originalNodes);
				doThenAdd(new LayoutLabelsCommand(view, rootPosition, nodes), commands);
			}

			if (runReroot.get()) {
				if (root != null) {
					var reroot = new RerootCommand(view, root, null);
					doThenAdd(reroot, commands);
				}
			}

			if (showArrows.get()) {
				var edges = CollectionUtils.difference(IteratorUtils.asSet(view.getGraph().edges()), originalEdges);
				doThenAdd(new SmoothCommand(view, edges), commands);
				doThenAdd(new ShowArrowsCommand(view, edges, true), commands);
			}

		}
		System.err.printf("Phylogeny: %d nodes, %d edges%n", view.getGraph().getNumberOfNodes(), view.getGraph().getNumberOfEdges());

		return new CompositeCommand("capture", commands.toArray(new UndoableRedoableCommand[0]));
	}

	public List<Property<?>> getAll() {
		return parameters;
	}

	private static boolean doThenAdd(UndoableRedoableCommand command, List<UndoableRedoableCommand> commands) {
		if (command.isRedoable()) {
			command.redo();
			commands.add(command);
			return true;
		} else return false;
	}

	private static RootPosition determineRootPosition(Point2D rootLocation, List<Segment> segments) {
		var linesRight = segments.stream().filter(seg -> seg.first().x() > rootLocation.getX()).count();
		var linesLeft = segments.stream().filter(seg -> seg.first().x() < rootLocation.getX()).count();
		var linesAbove = segments.stream().filter(seg -> seg.first().y() > rootLocation.getY()).count();
		var linesBelow = segments.stream().filter(seg -> seg.first().y() > rootLocation.getY()).count();

		if (linesRight > linesLeft && linesRight > linesAbove && linesRight > linesBelow)
			return new RootPosition(RootPosition.Side.Left, rootLocation);
		else if (linesLeft > linesRight && linesLeft > linesAbove && linesLeft > linesBelow)
			return new RootPosition(RootPosition.Side.Right, rootLocation);
		else if (linesAbove > linesLeft && linesAbove > linesRight && linesAbove > linesBelow)
			return new RootPosition(RootPosition.Side.Bottom, rootLocation);
		else if (linesBelow > linesLeft && linesBelow > linesRight && linesBelow > linesAbove)
			return new RootPosition(RootPosition.Side.Top, rootLocation);
		else
			return new RootPosition(RootPosition.Side.Center, rootLocation);
	}

	private static void matchLabels(DrawView view, RootPosition rootPosition, List<Node> nodes, List<Word> words0, boolean rescueOrphans, List<UndoableRedoableCommand> commands) {
		var leaves = new ArrayList<Pair<Point2D, Node>>();
		for (var v : nodes) {
			if (v.isLeaf()) {
				leaves.add(new Pair<>(DrawView.getPoint(v), v));
			}
		}
		var words = new ArrayList<>(words0);

		var delta = 10.0;

		while (!leaves.isEmpty() && !words.isEmpty()) {
			var bestDistance = Double.MAX_VALUE;
			Pair<Point2D, Node> bestLeaf = null;
			Word bestWord = null;

			for (var leaf : leaves) {
				var leafPos = leaf.getFirst();
				for (var word : words) {
					var bbox = word.boundingBox();

					// make sure position is acceptable
					switch (rootPosition.side()) {
						case Left -> {
							if (bbox.getMinX() < leafPos.getX() - delta)
								continue;
							if (leafPos.getY() < bbox.getMinY() - delta || leafPos.getY() > bbox.getMaxY() + delta)
								continue;

						}
						case Right -> {
							if (bbox.getMinX() > leafPos.getX() + delta)
								continue;
							if (leafPos.getY() < bbox.getMinY() - delta || leafPos.getY() > bbox.getMaxY() + delta)
								continue;
						}
						case Bottom -> {
							if (bbox.getMinY() < leafPos.getY() - delta)
								continue;
							if (leafPos.getX() < bbox.getMinX() - delta || leafPos.getX() > bbox.getMaxX() + delta)
								continue;
						}
						case Top -> {
							if (bbox.getMinY() > leafPos.getY() + delta)
								continue;
							if (leafPos.getX() < bbox.getMinX() - delta || leafPos.getX() > bbox.getMaxX() + delta)
								continue;
						}
						case Center -> {
							if (rootPosition.location().distance(bbox.getCenterX(), bbox.getCenterY()) < rootPosition.location().distance(leafPos) - delta)
								continue;
						}
					}

					// see whether this is a better match
					var distance = switch (rootPosition.side()) {
						case Left -> leafPos.distance(bbox.getMinX(), bbox.getCenterY());
						case Right -> leafPos.distance(bbox.getMaxX(), bbox.getCenterY());
						case Bottom -> leafPos.distance(bbox.getCenterX(), bbox.getMaxY());
						case Top -> leafPos.distance(bbox.getCenterX(), bbox.getMinY());
						case Center -> leafPos.distance(bbox.getCenterX(), bbox.getCenterY());
					};
					if (distance < bestDistance) {
						bestDistance = distance;
						bestWord = word;
						bestLeaf = leaf;
					}
				}
			}
			if (bestLeaf != null) {
				leaves.remove(bestLeaf);
				words.remove(bestWord);
				doThenAdd(new SetNodeLabelsCommand(view, rootPosition, List.of(bestLeaf.getSecond()), bestWord.text()), commands);
			} else break;
		}
		if (rescueOrphans) {
			if (!words.isEmpty()) {
				for (var word : words) {
					var bbox = word.boundingBox();
					var location = switch (rootPosition.side()) {
						case Left -> new Point2D(bbox.getMinX() - 5, bbox.getCenterY());
						case Right -> new Point2D(bbox.getMaxX() + 5, bbox.getCenterY());
						case Bottom -> new Point2D(bbox.getCenterX() - 2, bbox.getMinY() - 5);
						case Top, Center -> new Point2D(bbox.getCenterX() - 2, bbox.getMaxY() + 5);
					};
					doThenAdd(new CreateNodeCommand(view, location, word.text()), commands);
				}
			}
		}
	}
}

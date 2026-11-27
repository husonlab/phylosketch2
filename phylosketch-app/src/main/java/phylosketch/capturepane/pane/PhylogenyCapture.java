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
import javafx.geometry.Rectangle2D;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import jloda.util.IteratorUtils;
import jloda.util.Pair;
import phylosketch.capturepane.capture.Segment;
import phylosketch.commands.*;
import phylosketch.ocr.OcrWord;
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
	private final BooleanProperty removeStrayLines = new SimpleBooleanProperty(this, "removeStrayLines", true);

	private final List<Property<?>> parameters = List.of(removeThruNodes, fixCrossingEdges, layoutLabels, runReroot, showArrows, rescueOrphanLabels, removeStrayLines);

	// segments hang together if an end of one lies within this distance of the other: what DrawEdgeCommand joins
	private static final double JOIN_DISTANCE = 10;
	// lines that do not hang together with the root are another tree if they have at least this many open ends...
	private static final int MIN_ENDS_OF_TREE = 3;
	// ... and are at least this large, as a proportion of the size of what hangs together with the root
	private static final double MIN_SIZE_OF_TREE = 0.25;

	public UndoableRedoableCommand apply(DrawView view, Point2D rootLocation, RootPosition.Side rootSide, List<Segment> segments0, List<OcrWord> words) {
		var segments = (removeStrayLines.get() && rootLocation != null ? removeStrayLines(segments0, rootLocation) : segments0);
		var originalNodes = IteratorUtils.asList(view.getGraph().nodes());
		var originalEdges = IteratorUtils.asSet(view.getGraph().edges());

		var rootPosition = new RootPosition(rootSide, rootLocation);

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
					var firstDistance = distance.apply(segment.first().point2D(), rootLocation);
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
				doThenAdd(new DrawEdgeCommand(view, path, null), commands);
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

	/**
	 * keeps the segments that hang together with the root, and of the others only those that look like a tree of
	 * their own: a scale bar, a bracket, the frame of a legend or what is left of a title is neither
	 *
	 * @param segments     all segments
	 * @param rootLocation the root
	 * @return the segments to make edges from
	 */
	static List<Segment> removeStrayLines(List<Segment> segments, Point2D rootLocation) {
		var n = segments.size();
		if (n < 2)
			return segments;
		var boxes = new ArrayList<Rectangle2D>(n);
		for (var segment : segments)
			boxes.add(segment.computeBoundingBox());

		// which segments hang together
		var group = new int[n];
		for (var i = 0; i < n; i++)
			group[i] = i;
		for (var i = 0; i < n; i++) {
			for (var j = i + 1; j < n; j++) {
				if (group[i] != group[j] && near(boxes.get(i), boxes.get(j)) && (touches(segments.get(i), segments.get(j)) || touches(segments.get(j), segments.get(i)))) {
					var from = group[j];
					var to = group[i];
					for (var k = 0; k < n; k++) {
						if (group[k] == from)
							group[k] = to;
					}
				}
			}
		}

		// the group of the root: that of the segment nearest to it
		var rootGroup = -1;
		var best = Double.MAX_VALUE;
		for (var i = 0; i < n; i++) {
			for (var p : segments.get(i).points()) {
				var distance = p.distance(rootLocation);
				if (distance < best) {
					best = distance;
					rootGroup = group[i];
				}
			}
		}
		var rootSize = size(segments, group, rootGroup);

		var result = new ArrayList<Segment>();
		for (var i = 0; i < n; i++) {
			var g = group[i];
			if (g == rootGroup || (countOpenEnds(segments, group, g) >= MIN_ENDS_OF_TREE && size(segments, group, g) >= MIN_SIZE_OF_TREE * rootSize))
				result.add(segments.get(i));
		}
		return result;
	}

	private static boolean near(Rectangle2D a, Rectangle2D b) {
		return a.getMinX() <= b.getMaxX() + JOIN_DISTANCE && b.getMinX() <= a.getMaxX() + JOIN_DISTANCE && a.getMinY() <= b.getMaxY() + JOIN_DISTANCE && b.getMinY() <= a.getMaxY() + JOIN_DISTANCE;
	}

	/**
	 * does an end of the first segment lie on the second, or near it?
	 */
	private static boolean touches(Segment a, Segment b) {
		for (var end : List.of(a.first(), a.last())) {
			for (var p : b.points()) {
				if (end.distance(p) <= JOIN_DISTANCE)
					return true;
			}
		}
		return false;
	}

	/**
	 * the diagonal of the bounding box of a group of segments
	 */
	private static double size(List<Segment> segments, int[] group, int g) {
		var minX = Double.MAX_VALUE;
		var minY = Double.MAX_VALUE;
		var maxX = -Double.MAX_VALUE;
		var maxY = -Double.MAX_VALUE;
		for (var i = 0; i < group.length; i++) {
			if (group[i] == g) {
				for (var p : segments.get(i).points()) {
					minX = Math.min(minX, p.x());
					maxX = Math.max(maxX, p.x());
					minY = Math.min(minY, p.y());
					maxY = Math.max(maxY, p.y());
				}
			}
		}
		return Math.hypot(maxX - minX, maxY - minY);
	}

	/**
	 * the number of ends of segments of a group at which no other segment of the group ends or passes
	 */
	private static int countOpenEnds(List<Segment> segments, int[] group, int g) {
		var count = 0;
		for (var i = 0; i < group.length; i++) {
			if (group[i] != g)
				continue;
			for (var end : List.of(segments.get(i).first(), segments.get(i).last())) {
				var open = true;
				for (var j = 0; j < group.length && open; j++) {
					if (j != i && group[j] == g) {
						for (var p : segments.get(j).points()) {
							if (end.distance(p) <= JOIN_DISTANCE) {
								open = false;
								break;
							}
						}
					}
				}
				if (open)
					count++;
			}
		}
		return count;
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
		var linesAbove = segments.stream().filter(seg -> seg.first().y() < rootLocation.getY()).count();
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

	private static void matchLabels(DrawView view, RootPosition rootPosition, List<Node> nodes, List<OcrWord> words0, boolean rescueOrphans, List<UndoableRedoableCommand> commands) {
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
			OcrWord bestWord = null;

			for (var leaf : leaves) {
				var leafPos = leaf.getFirst();
				for (var word : words) {
					var bbox = word.boundingBox();

					// make sure position is acceptable. In every case we test the edge of the label box
					// that is nearest to the leaf (the "attachment" edge) and require the leaf to lie within
					// the label's extent along the perpendicular axis. The four cases are exact mirror images.
					switch (rootPosition.side()) {
						case Left -> {
							// root left, leaf to the right of it, label to the right of the leaf: near edge is minX
							if (bbox.getMinX() < leafPos.getX() - delta)
								continue;
							if (leafPos.getY() < bbox.getMinY() - delta || leafPos.getY() > bbox.getMaxY() + delta)
								continue;

						}
						case Right -> {
							// root right, leaf to the left of it, label to the left of the leaf: near edge is maxX
							if (bbox.getMaxX() > leafPos.getX() + delta)
								continue;
							if (leafPos.getY() < bbox.getMinY() - delta || leafPos.getY() > bbox.getMaxY() + delta)
								continue;
						}
						case Bottom -> {
							// root bottom, leaf above it, label above the leaf: near edge is maxY
							if (bbox.getMaxY() > leafPos.getY() + delta)
								continue;
							if (leafPos.getX() < bbox.getMinX() - delta || leafPos.getX() > bbox.getMaxX() + delta)
								continue;
						}
						case Top -> {
							// root top, leaf below it, label below the leaf: near edge is minY
							if (bbox.getMinY() < leafPos.getY() - delta)
								continue;
							if (leafPos.getX() < bbox.getMinX() - delta || leafPos.getX() > bbox.getMaxX() + delta)
								continue;
						}
						case Center -> {
							if (rootPosition.location().distance(0.5 * (bbox.getMinX() + bbox.getMaxX()), 0.5 * (bbox.getMinY() + bbox.getMaxY())) < rootPosition.location().distance(leafPos) - delta)
								continue;
						}
					}

					// see whether this is a better match
					var distance = switch (rootPosition.side()) {
						case Left -> leafPos.distance(bbox.getMinX(), 0.5 * (bbox.getMinY() + bbox.getMaxY()));
						case Right -> leafPos.distance(bbox.getMaxX(), 0.5 * (bbox.getMinY() + bbox.getMaxY()));
						case Bottom -> leafPos.distance(0.5 * (bbox.getMinX() + bbox.getMaxX()), bbox.getMaxY());
						case Top -> leafPos.distance(0.5 * (bbox.getMinX() + bbox.getMaxX()), bbox.getMinY());
						case Center ->
								leafPos.distance(0.5 * (bbox.getMinX() + bbox.getMaxX()), 0.5 * (bbox.getMinY() + bbox.getMaxY()));
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
		// a label that is left over and stands right next to a node that is not a leaf is the label of that node: the
		// name of a clade. The node must be the only one that near
		{
			var inner = new ArrayList<Node>();
			for (var v : nodes) {
				if (!v.isLeaf() && (DrawView.getLabel(v) == null || DrawView.getLabel(v).getRawText().isBlank()))
					inner.add(v);
			}
			for (var word : new ArrayList<>(words)) {
				var bbox = word.boundingBox();
				Node best = null;
				var bestDistance = Double.MAX_VALUE;
				var secondDistance = Double.MAX_VALUE;
				for (var v : inner) {
					var p = DrawView.getPoint(v);
					var dx = Math.max(0, Math.max(bbox.getMinX() - p.getX(), p.getX() - bbox.getMaxX()));
					var dy = Math.max(0, Math.max(bbox.getMinY() - p.getY(), p.getY() - bbox.getMaxY()));
					var distance = Math.hypot(dx, dy);
					if (distance < bestDistance) {
						secondDistance = bestDistance;
						bestDistance = distance;
						best = v;
					} else if (distance < secondDistance)
						secondDistance = distance;
				}
				var reach = Math.max(8, 0.8 * Math.min(bbox.getWidth(), bbox.getHeight()));
				if (best != null && bestDistance <= reach && secondDistance > 1.5 * bestDistance + 3) {
					inner.remove(best);
					words.remove(word);
					doThenAdd(new SetNodeLabelsCommand(view, rootPosition, List.of(best), word.text()), commands);
				}
			}
		}
		if (rescueOrphans) {
			if (!words.isEmpty()) {
				for (var word : words) {
					var bbox = word.boundingBox();
					var location = switch (rootPosition.side()) {
						case Left -> new Point2D(bbox.getMinX() - 5, 0.5 * (bbox.getMinY() + bbox.getMaxY()));
						case Right -> new Point2D(bbox.getMaxX() + 5, 0.5 * (bbox.getMinY() + bbox.getMaxY()));
						case Bottom -> new Point2D(0.5 * (bbox.getMinX() + bbox.getMaxX()) - 2, bbox.getMinY() - 5);
						case Top, Center ->
								new Point2D(0.5 * (bbox.getMinX() + bbox.getMaxX()) - 2, bbox.getMaxY() + 5);
					};
					doThenAdd(new CreateNodeCommand(view, location, word.text()), commands);
				}
			}
		}
	}
}

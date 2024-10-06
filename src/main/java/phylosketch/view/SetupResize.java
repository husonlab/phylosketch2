/*
 * SetupResize.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.view;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * implements interactive resizing of selected sub-trees
 * Daniel Huson, 9.2024
 */
public class SetupResize {

	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	public static void apply(DrawPane view, BooleanProperty enableResize) {
		final var rectangle = new Rectangle();
		final var moveHandle = MaterialIcons.graphic(MaterialIcons.open_with);
		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		rectangle.setStyle("-fx-stroke-dash-array: 3 3;-fx-fill: transparent;-fx-stroke: gray;");

		final var nodePointMap = new HashMap<Integer, Point2D>();
		final var edgePointsMap = new HashMap<Integer, List<Point2D>>();

		var resizeGroup = new Group(rectangle, moveHandle, resizeHandle);

		InvalidationListener invalidationListener = e -> {
			if (view.getNodeSelection().size() > 1 && enableResize.get()) {
				updateSizeAndLocation(view, rectangle, moveHandle, resizeHandle);
				if (!view.getOtherGroup().getChildren().contains(resizeGroup)) {
					view.getOtherGroup().getChildren().add(resizeGroup);
				}
			} else {
				view.getOtherGroup().getChildren().remove(resizeGroup);
			}
		};

		view.getNodeSelection().getSelectedItems().addListener(invalidationListener);
		enableResize.addListener(invalidationListener);

		moveHandle.setOnMousePressed(me -> {
			mouseX = mouseDownX = me.getScreenX();
			mouseY = mouseDownY = me.getScreenY();
			me.consume();
		});

		moveHandle.setOnMouseDragged(me -> {
			var diff = view.screenToLocal(me.getScreenX(), me.getScreenY()).subtract(view.screenToLocal(mouseX, mouseY));
			NodeInteraction.moveNodes(view.getGraph(), view.getNodeSelection().getSelectedItems(), diff.getX(), diff.getY(), false);
			mouseX = me.getScreenX();
			mouseY = me.getScreenY();
			updateSizeAndLocation(view, rectangle, moveHandle, resizeHandle);
			me.consume();
		});

		moveHandle.setOnMouseReleased(me -> {
			view.getUndoManager().add(new UndoableRedoableCommand("move") {
				private final Point2D diff = view.screenToLocal(mouseX, mouseY).subtract(view.screenToLocal(mouseDownX, mouseDownY));
				private final List<Integer> nodeIds = view.getNodeSelection().getSelectedItems().stream().map(v -> v.getId()).toList();

				@Override
				public void undo() {
					var nodes = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					NodeInteraction.moveNodes(view.getGraph(), nodes, -diff.getX(), -diff.getY(), false);
				}

				@Override
				public void redo() {
					var nodes = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					NodeInteraction.moveNodes(view.getGraph(), nodes, diff.getX(), diff.getY(), false);
				}
			});
			me.consume();
		});

		resizeHandle.setOnMousePressed(me -> {
			mouseX = mouseDownX = me.getScreenX();
			mouseY = mouseDownY = me.getScreenY();
			nodePointMap.clear();
			edgePointsMap.clear();
			me.consume();
		});

		resizeHandle.setOnMouseDragged(me -> {
			var diff = view.screenToLocal(me.getScreenX(), me.getScreenY()).subtract(view.screenToLocal(mouseX, mouseY));
			if (rectangle.getWidth() + diff.getX() >= 20 && rectangle.getHeight() + diff.getY() >= 20) {

				if (nodePointMap.isEmpty()) {
					for (var v : view.getNodeSelection().getSelectedItems()) {
						if (v.getData() instanceof Shape shape) {
							nodePointMap.put(v.getId(), new Point2D(shape.getTranslateX(), shape.getTranslateY()));
						}
					}
					for (var e : getAdjacentEdges(view.getNodeSelection().getSelectedItems())) {
						if (e.getData() instanceof Path path) {
							edgePointsMap.put(e.getId(), PathUtils.extractPoints(path));
						}
					}
				}
				rectangle.setWidth(rectangle.getWidth() + diff.getX());
				rectangle.setHeight(rectangle.getHeight() + diff.getY());
				resizeHandle.setTranslateX(resizeHandle.getTranslateX() + diff.getX());
				resizeHandle.setTranslateY(resizeHandle.getTranslateY() + diff.getY());
				scale(view.getGraph(), view.getNodeSelection().getSelectedItems(), diff.getX(), diff.getY());
				mouseX = me.getScreenX();
				mouseY = me.getScreenY();
				me.consume();
			}
		});

		resizeHandle.setOnMouseReleased(me -> {
			PathNormalize.normalizeEdges(getAdjacentEdges(view.getNodeSelection().getSelectedItems()));

			view.getUndoManager().add(new UndoableRedoableCommand("resize") {
				private final Point2D diff = view.screenToLocal(mouseX, mouseY).subtract(view.screenToLocal(mouseDownX, mouseDownY));
				private final Map<Integer, Point2D> nodesPointMapFinal = new HashMap<>(nodePointMap);
				private final Map<Integer, List<Point2D>> edgesPointsMapFinal = new HashMap<>(edgePointsMap);

				@Override
				public void undo() {
					for (var entry : nodesPointMapFinal.entrySet()) {
						var v = view.getGraph().findNodeById(entry.getKey());
						if (v.getData() instanceof Shape shape) {
							shape.setTranslateX(entry.getValue().getX());
							shape.setTranslateY(entry.getValue().getY());
						}
					}
					for (var entry : edgesPointsMapFinal.entrySet()) {
						var e = view.getGraph().findEdgeById(entry.getKey());
						if (e.getData() instanceof Path path) {
							path.getElements().setAll(PathUtils.toPathElements(entry.getValue()));
						}
					}
					updateSizeAndLocation(view, rectangle, moveHandle, resizeHandle);
				}

				@Override
				public void redo() {
					var set = nodePointMap.keySet().stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					SetupResize.scale(view.getGraph(), set, diff.getX(), diff.getY());
					PathNormalize.normalizeEdges(getAdjacentEdges(set));
					updateSizeAndLocation(view, rectangle, moveHandle, resizeHandle);
				}
			});
			me.consume();
		});
	}

	private static void updateSizeAndLocation(DrawPane view, Rectangle rectangle, javafx.scene.Node moveHandle, javafx.scene.Node resizeHandle) {
		var bbox = computeBoundingBox(view.getNodeSelection().getSelectedItems());
		rectangle.setX(bbox.xMin() - 12);
		rectangle.setY(bbox.yMin() - 12);
		rectangle.setWidth(bbox.width() + 24);
		rectangle.setHeight(bbox.height() + 24);
		moveHandle.setTranslateX(rectangle.getX() - 16);
		moveHandle.setTranslateY(rectangle.getY() - 16);
		resizeHandle.setTranslateX(rectangle.getX() + rectangle.getWidth());
		resizeHandle.setTranslateY(rectangle.getY() + rectangle.getHeight());
	}

	public static void scale(PhyloTree graph, Collection<Node> nodes, double dx, double dy) {
		var boundingBox = computeBoundingBox(nodes);
		try (NodeArray<Point2D> nodeDiffMap = graph.newNodeArray()) {
			for (var v : nodes) {
				if (v.getData() instanceof Shape shape) {
					var x = shape.getTranslateX();
					shape.setTranslateX(boundingBox.width() <= 0 ? x : x + dx * (x - boundingBox.xMin()) / boundingBox.width());
					var y = shape.getTranslateY();
					shape.setTranslateY(boundingBox.height() <= 0 ? y : y + dy * (y - boundingBox.yMin()) / boundingBox.height());
					nodeDiffMap.put(v, new Point2D(shape.getTranslateX() - x, shape.getTranslateY() - y));
				}
			}
			for (var e : getAdjacentEdges(nodes)) {
				rescaleEdge(e, nodes);
			}
		}
	}

	private static void rescaleEdge(Edge e, Collection<Node> nodes) {
		if (e.getData() instanceof Path path) {
			var v = e.getSource();
			var w = e.getTarget();
			if (v.getData() instanceof Shape a && w.getData() instanceof Shape b) {
				var vPt = new Point2D(a.getTranslateX(), a.getTranslateY());
				var wPt = new Point2D(b.getTranslateX(), b.getTranslateY());
				var originalPoints = PathUtils.getPoints(path);
				var edgeStart = originalPoints.get(0);
				var edgeEnd = originalPoints.get(originalPoints.size() - 1);
				var oldBBox = new Box(List.of(edgeStart, edgeEnd));
				var newBBox = new Box(List.of(vPt, wPt));

				if (nodes.contains(v) && nodes.contains(w)) {
					var newPoints = new ArrayList<Point2D>();
					for (var p : originalPoints) {
						var x = (p.getX() - oldBBox.xMin()) / (Math.max(1, oldBBox.width())) * newBBox.width() + newBBox.xMin();
						var y = (p.getY() - oldBBox.yMin()) / (Math.max(1, oldBBox.height())) * newBBox.height() + newBBox.yMin();
						newPoints.add(new Point2D(x, y));
					}
					path.getElements().setAll(PathUtils.createPath(newPoints, false).getElements());
				} else if (nodes.contains(v)) {
					PathReshape.apply(path, 0, vPt.getX() - edgeStart.getX(), vPt.getY() - edgeStart.getY());
				} else if (nodes.contains(w)) {
					PathReshape.apply(path, originalPoints.size() - 1, wPt.getX() - edgeEnd.getX(), wPt.getY() - edgeEnd.getY());
				}
			}
		}
	}

	public static Collection<Edge> getAdjacentEdges(Collection<Node> nodes) {
		var edges = new HashSet<Edge>();
		for (var v : nodes) {
			edges.addAll(IteratorUtils.asList(v.adjacentEdges()));
		}
		return edges;
	}

	public static Box computeBoundingBox(Collection<Node> nodes) {
		var xMin = nodes.stream().filter(v -> v.getData() instanceof Shape).mapToDouble(v -> ((Shape) v.getData()).getTranslateX()).min().orElse(0.0);
		var xMax = nodes.stream().filter(v -> v.getData() instanceof Shape).mapToDouble(v -> ((Shape) v.getData()).getTranslateX()).max().orElse(0.0);
		var yMin = nodes.stream().filter(v -> v.getData() instanceof Shape).mapToDouble(v -> ((Shape) v.getData()).getTranslateY()).min().orElse(0.0);
		var yMax = nodes.stream().filter(v -> v.getData() instanceof Shape).mapToDouble(v -> ((Shape) v.getData()).getTranslateY()).max().orElse(0.0);
		return new Box(xMin, yMin, xMax, yMax);
	}

	public record Box(double xMin, double yMin, double xMax, double yMax) {
		public Box(Collection<Point2D> points) {
			this(points.stream().mapToDouble(Point2D::getX).min().orElse(0.0),
					points.stream().mapToDouble(Point2D::getY).min().orElse(0.0),
					points.stream().mapToDouble(Point2D::getX).max().orElse(0.0),
					points.stream().mapToDouble(Point2D::getY).max().orElse(0.0));
		}

		public double width() {
			return xMax - xMin;
		}

		public double height() {
			return yMax - yMin;
		}
	}
}

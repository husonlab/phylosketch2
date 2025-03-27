/*
 * SetupResize.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.IteratorUtils;
import phylosketch.commands.MoveNodesCommand;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.utils.ScrollPaneUtils;

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

	public static void apply(DrawView view, BooleanProperty resizeMode) {
		final var rectangle = new Rectangle();
		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		rectangle.setStyle("-fx-stroke-dash-array: 3 3;-fx-fill: transparent;-fx-stroke: gray;");

		final var nodePointMap = new HashMap<Integer, Point2D>();
		final var edgePointsMap = new HashMap<Integer, List<Point2D>>();

		var resizeGroup = new Group(rectangle, resizeHandle);

		InvalidationListener invalidationListener = e -> {
			if (view.getNodeSelection().size() > 1 && resizeMode.get()) {
				updateSizeAndLocation(view, rectangle, resizeHandle);
				if (!view.getOtherGroup().getChildren().contains(resizeGroup)) {
					view.getOtherGroup().getChildren().add(resizeGroup);
				}
			} else {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(view, () -> view.getOtherGroup().getChildren().remove(resizeGroup));
			}
		};

		view.getNodeSelection().getSelectedItems().addListener(invalidationListener);
		resizeMode.addListener(invalidationListener);

		resizeHandle.setOnMouseClicked(Event::consume);
		rectangle.setOnMouseClicked(Event::consume);

		rectangle.setOnContextMenuRequested(a -> {
			var resizeItem = new CheckMenuItem("Resize Mode");
			resizeItem.setSelected(resizeMode.get());
			resizeItem.setOnAction(d -> resizeMode.set(!resizeMode.get()));
			var contextMenu = new ContextMenu(resizeItem);
			contextMenu.show(rectangle, a.getScreenX(), a.getScreenY());
			a.consume();
		});

		rectangle.setOnMousePressed(me -> {
			mouseX = mouseDownX = me.getScreenX();
			mouseY = mouseDownY = me.getScreenY();
			me.consume();
		});


		rectangle.setOnMouseDragged(me -> {
			var diff = view.screenToLocal(me.getScreenX(), me.getScreenY()).subtract(view.screenToLocal(mouseX, mouseY));
			MoveNodesCommand.moveNodesAndEdges(view, view.getNodeSelection().getSelectedItems(), diff.getX(), diff.getY(), false);
			mouseX = me.getScreenX();
			mouseY = me.getScreenY();
			updateSizeAndLocation(view, rectangle, resizeHandle);
			me.consume();
		});

		rectangle.setOnMouseReleased(me -> {
			view.getUndoManager().add(new UndoableRedoableCommand("move") {
				private final Point2D diff = view.screenToLocal(mouseX, mouseY).subtract(view.screenToLocal(mouseDownX, mouseDownY));
				private final List<Integer> nodeIds = view.getNodeSelection().getSelectedItems().stream().map(v -> v.getId()).toList();

				@Override
				public void undo() {
					var nodes = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					MoveNodesCommand.moveNodesAndEdges(view, nodes, -diff.getX(), -diff.getY(), false);
				}

				@Override
				public void redo() {
					var nodes = nodeIds.stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					MoveNodesCommand.moveNodesAndEdges(view, nodes, diff.getX(), diff.getY(), false);
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
				scale(view, view.getNodeSelection().getSelectedItems(), diff.getX(), diff.getY());
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
					updateSizeAndLocation(view, rectangle, resizeHandle);
				}

				@Override
				public void redo() {
					var set = nodePointMap.keySet().stream().map(id -> view.getGraph().findNodeById(id)).collect(Collectors.toSet());
					SetupResize.scale(view, set, diff.getX(), diff.getY());
					PathNormalize.normalizeEdges(getAdjacentEdges(set));
					updateSizeAndLocation(view, rectangle, resizeHandle);
				}
			});
			me.consume();
		});
	}

	private static void updateSizeAndLocation(DrawView view, Rectangle rectangle, javafx.scene.Node resizeHandle) {
		var bbox = computeBoundingBox(view.getNodeSelection().getSelectedItems());
		rectangle.setX(bbox.xMin() - 12);
		rectangle.setY(bbox.yMin() - 12);
		rectangle.setWidth(bbox.width() + 24);
		rectangle.setHeight(bbox.height() + 24);
		resizeHandle.setTranslateX(rectangle.getX() + rectangle.getWidth());
		resizeHandle.setTranslateY(rectangle.getY() + rectangle.getHeight());
	}

	public static void scale(DrawView view, Collection<Node> nodes, double dx, double dy) {
		if (dx != 0 || dy != 0) {
			var oldBBox = computeBoundingBox(nodes);
			var newBBox = new Box(oldBBox.xMin(), oldBBox.yMin(), oldBBox.xMax() + dx, oldBBox.yMax() + dy);
			var xFactor = (oldBBox.width() > 0 ? newBBox.width() / oldBBox.width() : 1.0);
			var yFactor = (oldBBox.height() > 0 ? newBBox.height() / oldBBox.height() : 1.0);

			for (var v : nodes) {
				var x = DrawView.getX(v);
				var y = DrawView.getY(v);
				var newX = oldBBox.width() <= 0 ? x : (x - oldBBox.xMin) * xFactor + newBBox.xMin();
				var newY = oldBBox.height() <= 0 ? y : (y - oldBBox.yMax) * yFactor + newBBox.yMax();
				view.setLocation(v, new Point2D(newX, newY));
			}
			for (var e : getAdjacentEdges(nodes)) {
				rescaleEdge(e, nodes, oldBBox, newBBox);
			}
		}
	}

	private static void rescaleEdge(Edge e, Collection<Node> nodes, Box oldBBox, Box newBBox) {
		var v = e.getSource();
		var vPt = DrawView.getPoint(v);
		var w = e.getTarget();
		var wPt = DrawView.getPoint(w);
		var originalPoints = DrawView.getPoints(e);
		var edgeStart = originalPoints.get(0);
		var edgeEnd = originalPoints.get(originalPoints.size() - 1);
		var path = DrawView.getPath(e);

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

/*
 * RotateCommand.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.commands;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.*;

/**
 * Rotate command
 * Daniel Huson, 2024
 */
public class RotateCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> nodeOldPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeOldPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeMidPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeMidPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeNewPointsMap = new HashMap<>();

	private boolean undoLabelCommand;

	public RotateCommand(DrawView view, Collection<Node> nodes, boolean positiveRotation) {
		super("rotate");

		var angle = (positiveRotation ? 90 : -90);

		var x = nodes.stream().map(DrawView::getPoint).mapToDouble(Point2D::getX).average().orElse(0.0);
		var y = nodes.stream().map(DrawView::getPoint).mapToDouble(Point2D::getY).average().orElse(0.0);
		var center = new Point2D(x, y);
		for (var v : nodes) {
			var point = DrawView.getPoint(v);
			nodeOldPointMap.put(v.getId(), point);
			var mid = GeometryUtilsFX.rotateAbout(point, 0.5 * angle, center);
			nodeMidPointMap.put(v.getId(), mid);
			var rotated = GeometryUtilsFX.rotateAbout(point, angle, center);
			nodeNewPointMap.put(v.getId(), rotated);
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var points = DrawView.getPoints(e);
				edgeOldPointsMap.put(e.getId(), points);
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					var mid = new ArrayList<Point2D>();
					for (var point : points) {
						mid.add(GeometryUtilsFX.rotateAbout(point, 0.5 * angle, center));
					}
					edgeMidPointsMap.put(e.getId(), mid);
					var rotated = new ArrayList<Point2D>();
					for (var point : points) {
						rotated.add(GeometryUtilsFX.rotateAbout(point, angle, center));
					}
					edgeNewPointsMap.put(e.getId(), rotated);
				} else if (nodes.contains(e.getSource())) {
					var path = PathUtils.createPath(points, false);
					var id = 0;
					var diff = GeometryUtilsFX.rotateAbout(points.get(id), angle, center).subtract(points.get(id));
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					edgeNewPointsMap.put(e.getId(), PathUtils.getPoints(path));

				} else if (nodes.contains(e.getTarget())) {
					var path = PathUtils.createPath(points, false);
					var id = path.getElements().size() - 1;
					var diff = GeometryUtilsFX.rotateAbout(points.get(id), angle, center).subtract(points.get(id));
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					edgeNewPointsMap.put(e.getId(), PathUtils.getPoints(path));
				}
			}
		}

		undo = () -> {
			var first = new PauseTransition(Duration.seconds(0.1));
			first.setOnFinished(a -> {
				nodeMidPointMap.forEach((key, value) -> {
					if (view.getGraph().findNodeById(key).getData() instanceof Shape shape) {
						shape.setTranslateX(value.getX());
						shape.setTranslateY(value.getY());
					}
				});
				edgeMidPointsMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					DrawView.getPath(e).getElements().setAll(PathUtils.createPath(value, false).getElements());
				});
			});
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				nodeOldPointMap.forEach((key, value) -> {
					if (view.getGraph().findNodeById(key).getData() instanceof Shape shape) {
						shape.setTranslateX(value.getX());
						shape.setTranslateY(value.getY());
					}
				});
				edgeOldPointsMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					DrawView.getPath(e).getElements().setAll(PathUtils.createPath(value, false).getElements());
				});
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(a -> {
				if (undoLabelCommand)
					view.getUndoManager().undo();
			});
			sequential.play();
		};

		redo = () -> {
			var first = new PauseTransition(Duration.seconds(0.1));
			first.setOnFinished(a -> {
				for (var entry : nodeMidPointMap.entrySet()) {
					if (view.getGraph().findNodeById(entry.getKey()).getData() instanceof Shape shape) {
						shape.setTranslateX(entry.getValue().getX());
						shape.setTranslateY(entry.getValue().getY());
					}
				}
				for (var entry : edgeMidPointsMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					DrawView.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var sequential = getSequentialTransition(view, first);
			sequential.setOnFinished(a -> {
				var command = new LayoutLabelsCommand(view, null, nodes);
				if (command.isUndoable() && command.isRedoable()) {
					undoLabelCommand = true;
					view.getUndoManager().doAndAdd(command);
				} else undoLabelCommand = false;
			});
			sequential.play();
		};
	}

	private SequentialTransition getSequentialTransition(DrawView view, PauseTransition first) {
		var second = new PauseTransition(Duration.seconds(0.1));
		second.setOnFinished(a -> {
			nodeNewPointMap.forEach((key, value) -> {
				var v = view.getGraph().findNodeById(key);
				if (v.getData() instanceof Shape shape) {
					shape.setTranslateX(value.getX());
					shape.setTranslateY(value.getY());
				}
			});
			edgeNewPointsMap.forEach((key, value) -> {
				var e = view.getGraph().findEdgeById(key);
				DrawView.getPath(e).getElements().setAll(PathUtils.createPath(value, false).getElements());
			});
		});
		return new SequentialTransition(first, second);
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}
}

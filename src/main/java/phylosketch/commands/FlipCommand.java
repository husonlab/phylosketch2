/*
 * RotateCommand.java Copyright (C) 2024 Daniel H. Huson
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
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * the flip coordinates command
 * Daniel Huson, 9.2024
 */
public class FlipCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> nodeOldPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeOldPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeMidPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeMidPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeNewPointsMap = new HashMap<>();

	/**
	 * flip all or currently selected nodes
	 *
	 * @param view       the window
	 * @param horizontal flip horizontally, if true, otherwise vertically
	 */
	public FlipCommand(DrawPane view, boolean horizontal) {
		super("flip");

		var nodes = view.getSelectedOrAllNodes();

		var layoutCommmand = new LayoutLabelsCommand(view, nodes);

		var x = nodes.stream().map(view::getPoint).mapToDouble(Point2D::getX).average().orElse(0.0);
		var y = nodes.stream().map(view::getPoint).mapToDouble(Point2D::getY).average().orElse(0.0);

		for (var v : nodes) {
			var point = view.getPoint(v);
			nodeOldPointMap.put(v.getId(), point);
			var mid = new Point2D(horizontal ? x : point.getX(), horizontal ? point.getY() : y);
			nodeMidPointMap.put(v.getId(), mid);
			var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
			nodeNewPointMap.put(v.getId(), flipped);
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var points = view.getPoints(e);
				edgeOldPointsMap.put(e.getId(), points);
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					var midPoints = new ArrayList<Point2D>();
					for (var point : points) {
						midPoints.add(new Point2D(horizontal ? x : point.getX(), horizontal ? point.getY() : y));
					}
					edgeMidPointsMap.put(e.getId(), midPoints);
					var flippedPoints = new ArrayList<Point2D>();
					for (var point : points) {
						var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
						flippedPoints.add(flipped);
					}
					edgeNewPointsMap.put(e.getId(), flippedPoints);
				} else if (nodes.contains(e.getSource())) {
					var path = PathUtils.createPath(points, false);
					var id = 0;
					var point = points.get(id);
					var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
					var diff = flipped.subtract(points.get(id));
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					edgeNewPointsMap.put(e.getId(), PathUtils.getPoints(path));

				} else if (nodes.contains(e.getTarget())) {
					var path = PathUtils.createPath(points, false);
					var id = path.getElements().size() - 1;
					var point = points.get(id);
					var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
					var diff = flipped.subtract(points.get(id));
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					edgeNewPointsMap.put(e.getId(), PathUtils.getPoints(path));
				}
			}
		}

		undo = () -> {
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
					view.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				for (var entry : nodeOldPointMap.entrySet()) {
					if (view.getGraph().findNodeById(entry.getKey()).getData() instanceof Shape shape) {
						shape.setTranslateX(entry.getValue().getX());
						shape.setTranslateY(entry.getValue().getY());
					}
				}
				for (var entry : edgeOldPointsMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					view.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(a -> layoutCommmand.undo());
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
					view.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				for (var entry : nodeNewPointMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v.getData() instanceof Shape shape) {
						shape.setTranslateX(entry.getValue().getX());
						shape.setTranslateY(entry.getValue().getY());
					}
				}
				for (var entry : edgeNewPointsMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					view.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(a -> layoutCommmand.redo());
			sequential.play();
		};
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

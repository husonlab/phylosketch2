/*
 * FlipCommand.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.BooleanProperty;
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
 * the flip coordinates command
 * Daniel Huson, 9.2024
 */
public class FlipCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> nodeOldPointMap = new HashMap<>();
	private final Map<Integer, Point2D> labelOldPointMap = new HashMap<>();
	private final Map<Integer, Double> labelOldAngleMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeOldPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeMidPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeMidPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, Point2D> labelNewPointMap = new HashMap<>();
	private final Map<Integer, Double> labelNewAngleMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeNewPointsMap = new HashMap<>();

	/**
	 * flip all or currently selected nodes
	 *
	 * @param view       the window
	 * @param horizontal flip horizontally, if true, otherwise vertically
	 */
	public FlipCommand(DrawView view, Collection<Node> nodes, boolean horizontal, BooleanProperty isRunning) {
		super("flip");

		if (nodes.size() <= 1) {
			undo = redo = null;
			return;
		}

		var oldHorizontalLabels = view.isHorizontalLabels();
		var newHorizontalLabels = oldHorizontalLabels || nodes.size() < view.getGraph().getNumberOfNodes();

		var layoutCommmand = new LayoutLabelsCommand(view, null, nodes);

		var x = nodes.stream().map(DrawView::getPoint).mapToDouble(Point2D::getX).average().orElse(0.0);
		var y = nodes.stream().map(DrawView::getPoint).mapToDouble(Point2D::getY).average().orElse(0.0);
		var center = new Point2D(x, y);
		for (var v : nodes) {
			var point = DrawView.getPoint(v);
			nodeOldPointMap.put(v.getId(), point);
			var mid = new Point2D(horizontal ? x : point.getX(), horizontal ? point.getY() : y);
			nodeMidPointMap.put(v.getId(), mid);
			var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
			nodeNewPointMap.put(v.getId(), flipped);

			var label = DrawView.getLabel(v);
			if (label != null && !label.getRawText().isBlank()) {
				var labelAngle = label.getRotate();
				var labelPoint = new Point2D(label.getLayoutX(), label.getLayoutY());
				labelOldPointMap.put(v.getId(), labelPoint);
				labelOldAngleMap.put(v.getId(), labelAngle);

				double angle;
				if (horizontal) {
					angle = labelRotationForHorizontalFlip(labelAngle);
				} else {
					angle = labelRotationForVerticalFlip(labelAngle);
				}

				if (newHorizontalLabels) {
					var other = center;
					if (v.getParent() != null) {
						var p = view.getLocation(v.getParent());
						if (point.distance(p) >= 0.1)
							other = p;
					}
					var currentAngle = GeometryUtilsFX.computeAngle(point.subtract(other));
					var shift = GeometryUtilsFX.translateByAngle(new Point2D(0, 0), currentAngle + angle, 30).subtract(0.5 * label.getWidth(), 0.5 * label.getHeight());
					labelNewAngleMap.put(v.getId(), 0.0);
					labelNewPointMap.put(v.getId(), shift);
				} else { // todo: this needs fixing
					var newLabelAngle = GeometryUtilsFX.modulo360(angle + labelAngle);
					labelNewAngleMap.put(v.getId(), newLabelAngle);
					var labelNewPoint = GeometryUtilsFX.translateByAngle(0, 0, newLabelAngle, 10);

					//var shift = GeometryUtilsFX.rotateAbout(new Point2D(0.5 * label.getWidth() + 10, 0), newLabelAngle, new Point2D(0, 0));
					//var labelNewPoint = new Point2D(-0.5 * label.getWidth(), -0.5 * label.getHeight()).add(shift);
					labelNewPointMap.put(v.getId(), labelNewPoint);
				}
			}
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var points = DrawView.getPoints(e);
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
			view.setHorizontalLabels(oldHorizontalLabels);
			isRunning.set(true);
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
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				for (var entry : nodeOldPointMap.entrySet()) {
					if (view.getGraph().findNodeById(entry.getKey()).getData() instanceof Shape shape) {
						shape.setTranslateX(entry.getValue().getX());
						shape.setTranslateY(entry.getValue().getY());
					}
				}
				labelOldPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					var label = DrawView.getLabel(v);
					label.setLayoutX(value.getX());
					label.setLayoutY(value.getY());
					if (!view.isHorizontalLabels()) {
						label.setRotate(GeometryUtilsFX.modulo360(labelOldAngleMap.get(key)));
						label.ensureUpright();
					}
				});
				for (var entry : edgeOldPointsMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					DrawView.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(a -> {
				layoutCommmand.undo();
				isRunning.set(false);
			});
			sequential.play();
		};

		redo = () -> {
			view.setHorizontalLabels(newHorizontalLabels);
			isRunning.set(true);
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
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				for (var entry : nodeNewPointMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v.getData() instanceof Shape shape) {
						shape.setTranslateX(entry.getValue().getX());
						shape.setTranslateY(entry.getValue().getY());
					}
				}
				labelNewPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					var label = DrawView.getLabel(v);
					label.setLayoutX(value.getX());
					label.setLayoutY(value.getY());
					label.setRotate(GeometryUtilsFX.modulo360(labelNewAngleMap.get(key)));
					label.ensureUpright();
				});

				for (var entry : edgeNewPointsMap.entrySet()) {
					var e = view.getGraph().findEdgeById(entry.getKey());
					DrawView.getPath(e).getElements().setAll(PathUtils.createPath(entry.getValue(), false).getElements());
				}
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(a -> {
				layoutCommmand.redo();
				isRunning.set(false);
			});
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

	static double labelRotationForHorizontalFlip(double A) {
		var B = (180 - A) % 360;
		if (B < 0) B += 360;
		var delta = B - A;

		// Normalize to (-180, 180]
		delta = ((delta + 180) % 360) - 180;

		// If rotating would cross the horizontal axis, go the other way
		if ((A >= 0 && A <= 180 && delta < 0) ||
			(A > 180 && delta > 0)) {
			delta = (delta > 0) ? delta - 360 : delta + 360;
		}
		return delta;
	}

	static double labelRotationForVerticalFlip(double A) {
		// normalize A to [0, 360)
		A = ((A % 360) + 360) % 360;

		// vertical flip across x-axis
		var B = (360 - A) % 360;

		// shortest signed rotation from A to B in (-180, 180]
		var delta = B - A;
		delta = ((delta + 180) % 360) - 180;

		return delta;
	}

}

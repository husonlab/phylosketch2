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
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeMidPointMap = new HashMap<>();
	private final Map<Integer, EdgePath> midEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, Point2D> labelNewPointMap = new HashMap<>();
	private final Map<Integer, Double> labelNewAngleMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

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
					//var labelNewPoint = new Point2D(-0.5 * label.width(), -0.5 * label.getHeight()).add(shift);
					labelNewPointMap.put(v.getId(), labelNewPoint);
				}
			}
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var edgePath = DrawView.getPath(e);
				oldEdgeMap.put(e.getId(), edgePath.copy());
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					if (horizontal) {
						var min = PathUtils.getPoints(edgePath).stream().mapToDouble(Point2D::getY).min().orElse(0.0);
						var max = PathUtils.getPoints(edgePath).stream().mapToDouble(Point2D::getY).max().orElse(0.0);
						midEdgeMap.put(e.getId(), new EdgePath(new Point2D(x, min), new Point2D(x, max)));
					} else {
						var min = PathUtils.getPoints(edgePath).stream().mapToDouble(Point2D::getX).min().orElse(0.0);
						var max = PathUtils.getPoints(edgePath).stream().mapToDouble(Point2D::getX).max().orElse(0.0);
						midEdgeMap.put(e.getId(), new EdgePath(new Point2D(min, y), new Point2D(max, y)));
					}
					var flipped = edgePath.flip(center, horizontal);
					newEdgeMap.put(e.getId(), flipped);
				} else if (nodes.contains(e.getSource())) {
					var path = edgePath.copy();
					path.changeToFreeform();
					var id = 0;
					var point = PathUtils.getCoordinates(path.getElements().get(id));
					var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
					var diff = flipped.subtract(point);
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					newEdgeMap.put(e.getId(), path);

				} else if (nodes.contains(e.getTarget())) {
					var path = edgePath.copy();
					path.changeToFreeform();
					var id = path.getElements().size() - 1;
					var point = PathUtils.getCoordinates(path.getElements().get(id));
					var flipped = new Point2D(horizontal ? x - (point.getX() - x) : point.getX(), horizontal ? point.getY() : y - (point.getY() - y));
					var diff = flipped.subtract(point);
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					newEdgeMap.put(e.getId(), path);
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
				midEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
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
				oldEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
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
				midEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
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
				newEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
			});
			var sequential = new SequentialTransition(first, second);
			sequential.setOnFinished(e -> isRunning.set(false));
			sequential.play();
		};
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
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

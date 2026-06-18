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
 * Rotate command
 * Daniel Huson, 2024
 */
public class RotateCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> nodeOldPointMap = new HashMap<>();
	private final Map<Integer, Point2D> labelOldPointMap = new HashMap<>();
	private final Map<Integer, Double> labelOldAngleMap = new HashMap<>();
	private final Map<Integer, EdgePath> oldEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeMidPointMap = new HashMap<>();
	private final Map<Integer, EdgePath> midEdgeMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, Double> labelNewAngleMap = new HashMap<>();
	private final Map<Integer, Point2D> labelNewPointMap = new HashMap<>();
	private final Map<Integer, EdgePath> newEdgeMap = new HashMap<>();

	public RotateCommand(DrawView view, Collection<Node> nodes, boolean positiveRotation, BooleanProperty isRunning) {
		super("rotate");

		if (nodes.size() <= 1) {
			undo = redo = null;
			return;
		}

		var angle = (positiveRotation ? 90 : -90);

		var oldHorizontalLabels = view.isHorizontalLabels();
		var newHorizontalLabels = oldHorizontalLabels || nodes.size() < view.getGraph().getNumberOfNodes();

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
			var label = DrawView.getLabel(v);
			if (label != null && !label.getRawText().isBlank()) {
				var labelAngle = label.getRotate();
				var labelPoint = new Point2D(label.getLayoutX(), label.getLayoutY());
				labelOldPointMap.put(v.getId(), labelPoint);
				labelOldAngleMap.put(v.getId(), labelAngle);
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
				} else {
					var newLabelAngle = GeometryUtilsFX.modulo360(angle + labelAngle);
					labelNewAngleMap.put(v.getId(), newLabelAngle);
					var shift = GeometryUtilsFX.rotateAbout(new Point2D(0.5 * label.getWidth() + 10, 0), newLabelAngle, new Point2D(0, 0));
					var labelNewPoint = new Point2D(-0.5 * label.getWidth(), -0.5 * label.getHeight()).add(shift);
					labelNewPointMap.put(v.getId(), labelNewPoint);
				}
			}
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var edgePath = DrawView.getPath(e);
				oldEdgeMap.put(e.getId(), edgePath.copy());
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
					var mid = edgePath.rotate(center, 0.5 * angle);
					midEdgeMap.put(e.getId(), mid);
					var rotated = edgePath.rotate(center, angle);
					newEdgeMap.put(e.getId(), rotated);
				} else if (nodes.contains(e.getSource())) {
					var path = edgePath.copy();
					path.changeToFreeform();
					var id = 0;
					var point = PathUtils.getCoordinates(path.getElements().get(id));
					var diff = GeometryUtilsFX.rotateAbout(point, angle, center).subtract(point);
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					newEdgeMap.put(e.getId(), path);

				} else if (nodes.contains(e.getTarget())) {
					var path = edgePath.copy();
					path.changeToFreeform();
					var id = path.getElements().size() - 1;
					var point = PathUtils.getCoordinates(path.getElements().get(id));
					var diff = GeometryUtilsFX.rotateAbout(point, angle, center).subtract(point);
					PathReshape.apply(path, id, diff.getX(), diff.getY());
					newEdgeMap.put(e.getId(), path);
				}
			}
		}

		undo = () -> {
			view.setHorizontalLabels(oldHorizontalLabels);
			var first = new PauseTransition(Duration.seconds(0.1));
			first.setOnFinished(a -> {
				nodeMidPointMap.forEach((key, value) -> {
					if (view.getGraph().findNodeById(key).getData() instanceof Shape shape) {
						shape.setTranslateX(value.getX());
						shape.setTranslateY(value.getY());
					}
				});
				midEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
			});
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				nodeOldPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					var shape = DrawView.getShape(v);
					shape.setTranslateX(value.getX());
					shape.setTranslateY(value.getY());
				});
				labelOldPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					var label = DrawView.getLabel(v);
					label.setLayoutX(value.getX());
					label.setLayoutY(value.getY());
					label.setRotate(labelOldAngleMap.get(key));
					label.ensureUpright();
				});
				oldEdgeMap.forEach((key, value) -> {
					var e = view.getGraph().findEdgeById(key);
					var path = DrawView.getPath(e);
					path.set(value.getElements(), value.getType());
				});
			});
			if (!isRunning.get()) {
				isRunning.set(true);
				var sequential = new SequentialTransition(first, second);
				sequential.setOnFinished(e -> isRunning.set(false));
				sequential.play();
			}
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
				labelNewPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					var label = DrawView.getLabel(v);
					label.setLayoutX(value.getX());
					label.setLayoutY(value.getY());
					label.setRotate(labelNewAngleMap.get(key));
					label.ensureUpright();
				});
			});
			var second = new PauseTransition(Duration.seconds(0.1));
			second.setOnFinished(a -> {
				nodeNewPointMap.forEach((key, value) -> {
					var v = view.getGraph().findNodeById(key);
					if (v.getData() instanceof Shape shape) {
						shape.setTranslateX(value.getX());
						shape.setTranslateY(value.getY());
					}
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
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}
}

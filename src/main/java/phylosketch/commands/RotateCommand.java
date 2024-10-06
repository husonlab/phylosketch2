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

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import phylosketch.paths.PathReshape;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RotateCommand extends UndoableRedoableCommand {
	private final Map<Integer, Point2D> nodeOldPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeOldPointsMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewPointMap = new HashMap<>();
	private final Map<Integer, List<Point2D>> edgeNewPointsMap = new HashMap<>();
	private final Runnable undo;
	private final Runnable redo;

	public RotateCommand(DrawPane view, boolean positiveRotation) {
		super("rotate");

		var angle = (positiveRotation ? 90 : -90);

		var nodes = view.getSelectedOrAllNodes();

		var x = view.getNodeSelection().getSelectedItems().stream().map(view::getPoint).mapToDouble(Point2D::getX).average().orElse(0.0);
		var y = view.getNodeSelection().getSelectedItems().stream().map(view::getPoint).mapToDouble(Point2D::getY).average().orElse(0.0);
		var center = new Point2D(x, y);
		for (var v : nodes) {
			var point = view.getPoint(v);
			nodeOldPointMap.put(v.getId(), point);
			var rotated = GeometryUtilsFX.rotateAbout(point, angle, center);
			nodeNewPointMap.put(v.getId(), rotated);
		}
		for (var e : view.getGraph().edges()) {
			if (nodes.contains(e.getSource()) || nodes.contains(e.getTarget())) {
				var points = view.getPoints(e);
				edgeOldPointsMap.put(e.getId(), points);
				if (nodes.contains(e.getSource()) && nodes.contains(e.getTarget())) {
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
		};

		redo = () -> {
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

/*
 * FixCrossingEdgesCommand.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.scene.shape.Path;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathSmoother;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * remove all or selected "true nodes" (di-vertices) from graph
 * Daniel Huson, 9.2024
 */
public class FixCrossingEdgesCommand extends UndoableRedoableCommand {
	private Runnable undo;
	private Runnable redo;

	private final CompositeCommand compositeCommand = new CompositeCommand("fix crossing edges");

	private int vId;

	/**
	 * constructor
	 *
	 * @param view
	 */
	public FixCrossingEdgesCommand(DrawPane view, Node v0) {
		super("fix crossing edges");

		if (v0.getInDegree() == 2 && v0.getOutDegree() == 2 && view.getLabel(v0).getRawText().isBlank()) {
			vId = v0.getId();


			undo = compositeCommand::undo;

			redo = () -> {
				compositeCommand.clear();

				var v = view.getGraph().findNodeById(vId);
				var vPoint = view.getPoint(v);
				var inEdge1 = v.getFirstInEdge();
				var inPoint1 = getPointAwayFromEnd(view.getPath(inEdge1), 5);
				var inEdge2 = v.getLastInEdge();
				var inPoint2 = getPointAwayFromEnd(view.getPath(inEdge2), 5);
				var outEdge1 = v.getFirstOutEdge();
				var outPoint1 = getPointAwayFromStart(view.getPath(outEdge1), 5);
				var outEdge2 = v.getLastOutEdge();
				var outPoint2 = getPointAwayFromStart(view.getPath(outEdge2), 5);


				var commands = new ArrayList<UndoableRedoableCommand>();

				if (isConvex(inPoint1, inPoint2, outPoint1, outPoint2)) {
					var path1 = PathUtils.concatenate(view.getPath(inEdge1), view.getPath(outEdge1), true);
					PathSmoother.apply(path1, 10);
					commands.add(new DrawEdgeCommand(view, path1));
					var path2 = PathUtils.concatenate(view.getPath(inEdge2), view.getPath(outEdge2), true);
					PathSmoother.apply(path2, 10);
					commands.add(new DrawEdgeCommand(view, path2));
					commands.add(new DeleteCommand(view, List.of(v), Collections.emptyList()));
				} else {
					var path1 = PathUtils.concatenate(view.getPath(inEdge1), view.getPath(outEdge2), true);
					PathSmoother.apply(path1, 10);
					commands.add(new DrawEdgeCommand(view, path1));
					var path2 = PathUtils.concatenate(view.getPath(inEdge2), view.getPath(outEdge1), true);
					PathSmoother.apply(path2, 10);
					commands.add(new DrawEdgeCommand(view, path2));
					commands.add(new DeleteCommand(view, List.of(v), Collections.emptyList()));
				}
				compositeCommand.add(commands.toArray(new UndoableRedoableCommand[0]));
				compositeCommand.redo();
			};
		}
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

	private static Point2D getPointAwayFromStart(Path path, double minDistance) {
		var points = PathUtils.extractPoints(path);
		var start = points.get(0);
		for (var point : points) {
			if (point.distance(start) > minDistance) {
				return point;
			}
		}
		return points.get(points.size() - 1);
	}

	private static Point2D getPointAwayFromEnd(Path path, double minDistance) {
		var points = CollectionUtils.reverse(PathUtils.extractPoints(path));
		var start = points.get(0);
		for (var point : points) {
			if (point.distance(start) > minDistance) {
				return point;
			}
		}
		return points.get(points.size() - 1);
	}

	public static UndoableRedoableCommand create(DrawPane view, Collection<Node> nodes) {
		var commands = nodes.stream().filter(v -> v.getInDegree() == 2 && v.getOutDegree() == 2 && view.getLabel(v).getRawText().isBlank())
				.map(v -> new FixCrossingEdgesCommand(view, v)).toList();
		if (commands.isEmpty()) {
			return UndoableRedoableCommand.create("null", null, null);
		} else {
			return new CompositeCommand("fix crossing edges", commands.toArray(new UndoableRedoableCommand[0]));
		}
	}

	public static boolean isConvex(Point2D a, Point2D b, Point2D c, Point2D d) {
		// Compute cross products
		double cross1 = crossProduct(a, b, c);
		double cross2 = crossProduct(b, c, d);
		double cross3 = crossProduct(c, d, a);
		double cross4 = crossProduct(d, a, b);

		// Check if all cross products have the same sign
		return (cross1 > 0 && cross2 > 0 && cross3 > 0 && cross4 > 0) ||
			   (cross1 < 0 && cross2 < 0 && cross3 < 0 && cross4 < 0);
	}

	private static double crossProduct(Point2D p1, Point2D p2, Point2D p3) {
		// Compute cross product of vector p1->p2 and p2->p3
		return (p2.getX() - p1.getX()) * (p3.getY() - p2.getY()) -
			   (p2.getY() - p1.getY()) * (p3.getX() - p2.getX());
	}
}

/*
 * InsertTwoNodesInEdgeCommand.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.scene.paint.Color;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * insert two nodes into an edge
 * Daniel Huson, 12.2024
 */
public class InsertTwoNodesInEdgeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private int sourceId = -1;
	private int targetId = -1;

	private int startEdgeId = -1;
	private int middleEdgeId = -1;
	private int endEdgeId = -1;
	private int node1Id = -1;
	private int node2Id = -1;

	private final DeleteCommand deleteEdgeCommand;

	private final Color stroke;
	private final double strokeWidth;
	private final List<Double> dashArray = new ArrayList<>();
	private final boolean arrow;

	public InsertTwoNodesInEdgeCommand(DrawPane view, Edge e, Point2D location1, Point2D location2) {
		super("insert nodes");

		sourceId = e.getSource().getId();
		targetId = e.getTarget().getId();

		var path = view.getPath(e);
		stroke = (Color) path.getStroke();
		strokeWidth = path.getStrokeWidth();
		dashArray.addAll(path.getStrokeDashArray());
		arrow = view.isShowArrow(e);

		var split = PathUtils.split(path, location1, location2);

		deleteEdgeCommand = new DeleteCommand(view, Collections.emptyList(), List.of(e));

		undo = () -> {
			deleteEdgeCommand.undo();
			view.deleteNode(view.getGraph().findNodeById(node1Id));
			view.deleteNode(view.getGraph().findNodeById(node2Id));
		};

		redo = () -> {
			deleteEdgeCommand.redo();

			var v = view.createNode(location1, node1Id);
			node1Id = v.getId();

			var w = view.createNode(location2, node2Id);
			node2Id = w.getId();

			var source = view.getGraph().findNodeById(sourceId);
			var target = view.getGraph().findNodeById(targetId);

			var startPath = PathUtils.createPath(split.get(0), true);
			var startEdge = view.createEdge(source, v, startPath, startEdgeId);
			startEdgeId = startEdge.getId();
			startPath.applyCss();
			startPath.setStroke(stroke);
			startPath.setStrokeWidth(strokeWidth);
			startPath.getStrokeDashArray().setAll(dashArray);
			view.setShowArrow(startEdge, arrow);

			var middlePath = PathUtils.createPath(split.get(1), true);
			var middleEdge = view.createEdge(v, w, middlePath, middleEdgeId);
			middleEdgeId = middleEdge.getId();
			middlePath.applyCss();
			middlePath.setStroke(stroke);
			middlePath.setStrokeWidth(strokeWidth);
			middlePath.getStrokeDashArray().setAll(dashArray);
			view.setShowArrow(middleEdge, arrow);

			var endPath = PathUtils.createPath(split.get(2), true);
			var endEdge = view.createEdge(w, target, endPath, endEdgeId);
			endEdgeId = endEdge.getId();
			endPath.applyCss();
			endPath.setStroke(stroke);
			endPath.setStrokeWidth(strokeWidth);
			endPath.getStrokeDashArray().setAll(dashArray);
			view.setShowArrow(endEdge, arrow);
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

	public int getNode1Id() {
		return node1Id;
	}

	public int getNode2Id() {
		return node2Id;
	}
}

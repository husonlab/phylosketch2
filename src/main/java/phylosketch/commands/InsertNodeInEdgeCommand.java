/*
 * InsertNodeInEdgeCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.fx.window.MainWindowManager;
import jloda.graph.Edge;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * insert a node into an edge
 * Daniel Huson, 12.2024
 */
public class InsertNodeInEdgeCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private int sourceId = -1;
	private int targetId = -1;

	private int startEdgeId = -1;
	private int endEdgeId = -1;
	private int nodeId = -1;

	private final List<List<Point2D>> split;

	private final DeleteCommand deleteEdgeCommand;

	private final Color stroke;
	private final double strokeWidth;
	private final List<Double> dashArray = new ArrayList<>();
	private final boolean arrow;

	public InsertNodeInEdgeCommand(DrawView view, Edge e, Point2D location) {
		super("insert node");

		sourceId = e.getSource().getId();
		targetId = e.getTarget().getId();

		var edgePath = DrawView.getPath(e);
		var workingPath = edgePath.copyToFreeform();
		stroke = (Color) edgePath.getStroke();
		strokeWidth = edgePath.getStrokeWidth();
		dashArray.addAll(edgePath.getStrokeDashArray());
		arrow = view.isShowArrow(e);

		split = PathUtils.split(workingPath, location);

		deleteEdgeCommand = new DeleteCommand(view, Collections.emptyList(), List.of(e));

		undo = () -> {
			deleteEdgeCommand.undo();
			view.deleteNode(view.getGraph().findNodeById(nodeId));
		};

		redo = () -> {
			deleteEdgeCommand.redo();

			var v = view.createNode(location, nodeId);
			nodeId = v.getId();

			var source = view.getGraph().findNodeById(sourceId);
			var target = view.getGraph().findNodeById(targetId);

			var startPath = PathUtils.createPath(split.get(0), true);
			var startEdge = view.createEdge(source, v, startPath, startEdgeId);
			startEdgeId = startEdge.getId();

			startPath.applyCss();
			startPath.setStrokeWidth(strokeWidth);
			startPath.getStrokeDashArray().setAll(dashArray);
			if (!MainWindowManager.isUseDarkTheme() && stroke != Color.BLACK || MainWindowManager.isUseDarkTheme() && stroke != Color.WHITE)
				startPath.setStroke(stroke);
			view.setShowArrow(startEdge, arrow);

			var endPath = PathUtils.createPath(split.get(1), true);
			var endEdge = view.createEdge(v, target, endPath, endEdgeId);
			endEdgeId = endEdge.getId();

			endPath.applyCss();
			endPath.setStrokeWidth(strokeWidth);
			endPath.getStrokeDashArray().setAll(dashArray);
			if (!MainWindowManager.isUseDarkTheme() && stroke != Color.BLACK || MainWindowManager.isUseDarkTheme() && stroke != Color.WHITE)
				endPath.setStroke(stroke);
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

	public int getNodeId() {
		return nodeId;
	}

	public int getStartEdgeId() {
		return startEdgeId;
	}

	public int getEndEdgeId() {
		return endEdgeId;
	}
}

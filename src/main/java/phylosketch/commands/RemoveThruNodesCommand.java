/*
 * RemoveThruNodesCommand.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.scene.shape.Shape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * remove all or selected di-vertices from graph
 * Daniel Huson, 9.2024
 */
public class RemoveThruNodesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final List<Data> dataList = new ArrayList<>();
	private final Map<Integer, Integer> nodeNewEdgeMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view
	 */
	public RemoveThruNodesCommand(DrawPane view) {
		super("remove di-vertices");

		for (var v : view.getNodeSelection().getSelectedItems()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1 && view.getLabel(v).getRawText().isBlank()) {
				var path1 = PathUtils.extractPoints((Path) v.getFirstInEdge().getData());
				var path2 = PathUtils.extractPoints((Path) v.getFirstOutEdge().getData());
				dataList.add(new Data(v.getParent().getId(), v.getFirstInEdge().getId(), path1, v.getFirstOutEdge().getTarget().getId(),
						v.getFirstOutEdge().getId(), path2, v.getId(), view.getPoint(v), (Shape) v.getData()));
			}
		}

		undo = () -> {
			var graph = view.getGraph();
			for (var id : nodeNewEdgeMap.values()) {
				graph.deleteEdge(graph.findEdgeById(id));
			}
			for (var data : dataList) {
				data.nodeShape.setTranslateX(data.nodeLocation.getX());
				data.nodeShape.setTranslateY(data.nodeLocation.getY());
				var v = view.createNode(data.nodeShape, null, data.nodeId);
				view.createEdge(graph.findNodeById(data.parentId), v, PathUtils.createPath(data.inPoints, false), data.inEdgeId);
				view.createEdge(v, graph.findNodeById(data.childId), PathUtils.createPath(data.outPoints, false), data.outEdgeId);
			}
		};

		redo = () -> {
			view.getNodeSelection().clearSelection();
			view.getEdgeSelection().clearSelection();
			var graph = view.getGraph();
			for (var data : dataList) {
				view.deleteNode(graph.findNodeById(data.nodeId));
				var merge = CollectionUtils.concatenate(data.inPoints.subList(0, data.inPoints.size()), data.outPoints);
				Edge e;
				if (!nodeNewEdgeMap.containsKey(data.nodeId)) {
					e = view.createEdge(graph.findNodeById(data.parentId), graph.findNodeById(data.childId), PathUtils.createPath(merge, false));
					nodeNewEdgeMap.put(data.nodeId, e.getId());
				} else {
					e = view.createEdge(graph.findNodeById(data.parentId), graph.findNodeById(data.childId), PathUtils.createPath(merge, false),
							nodeNewEdgeMap.get(data.nodeId));
				}
				view.getEdgeSelection().select(e);
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

	public record Data(int parentId, int inEdgeId, List<Point2D> inPoints, int childId, int outEdgeId,
					   List<Point2D> outPoints,
					   int nodeId, Point2D nodeLocation, Shape nodeShape) {
	}
}

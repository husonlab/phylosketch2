/*
 * DeleteNodesEdgesCommand.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch2.commands;

import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.IteratorUtils;
import phylosketch2.view.DrawPane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * delete nodes and/or edges command
 * Daniel Huson, 9.2024
 */
public class DeleteNodesEdgesCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public DeleteNodesEdgesCommand(DrawPane drawPane, Collection<Node> nodes, Collection<Edge> edges) {
		super("delete");

		 var nodeDataList = new ArrayList<NodeData>();
		 var edgeDataList = new ArrayList<EdgeData>();

		 var edgeSet = new HashSet<>(edges);

		for (var v : nodes) {
			nodeDataList.add(new NodeData(v));
			edgeSet.addAll(IteratorUtils.asList(v.adjacentEdges()));
		}

		for (var e : edgeSet) {
			edgeDataList.add(new EdgeData(e));
		}

		undo = () -> {
			drawPane.getNodeSelection().clearSelection();
			drawPane.getEdgeSelection().clearSelection();

			for (var data : nodeDataList) {
				var v=drawPane.createNode(data.getShape(),data.getLabel(),data.getNodeId());
				drawPane.getNodeSelection().select(v);
			}
			for (var data : edgeDataList) {
				var v = drawPane.getGraph().findNodeById(data.getSourceId());
				var w = drawPane.getGraph().findNodeById(data.getTargetId());
				var e = drawPane.createEdge(v,w,data.getPath(),data.getEdgeId());
				drawPane.getEdgeSelection().select(e);
			}
		};

		redo = () -> {
			for (var data : edgeDataList) {
				var e = drawPane.getGraph().findEdgeById(data.getEdgeId());
				drawPane.deleteEdge(e);
			}
			for (var data : nodeDataList) {
				var v = drawPane.getGraph().findNodeById(data.getNodeId());
				if(v!=null)
					drawPane.deleteNode(v);
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

	static class NodeData {
		private final int nodeId;
		private final Shape shape;
		private final RichTextLabel label;

		public NodeData(Node v) {
			nodeId=v.getId();
			if(v.getData() instanceof Shape vShape) {
				shape = vShape;
			} else{
				shape=null;
			}
			if(v.getInfo() instanceof RichTextLabel vLabel) {
				label=vLabel;
			}
			else {
				label=null;
			}
		}

		public int getNodeId() {
			return nodeId;
		}

		public Shape getShape() {
			return shape;
		}

		public RichTextLabel getLabel() {
			return label;
		}
	}

	static class EdgeData {
		private final int sourceId;
		private final int targetId;
		private final int edgeId;
		private final Path path;

		public EdgeData(Edge edge) {
			sourceId=edge.getSource().getId();
			targetId=edge.getTarget().getId();
			edgeId=edge.getId();
			if(edge.getData() instanceof Path ePath) {
				path=ePath;
			}
			else {
				path=null;
			}
		}

		public int getSourceId() {
			return sourceId;
		}

		public int getTargetId() {
			return targetId;
		}

		public int getEdgeId() {
			return edgeId;
		}

		public Path getPath() {
			return path;
		}
	}
}
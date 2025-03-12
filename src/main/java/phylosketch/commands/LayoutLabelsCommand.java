/*
 * LayoutLabelsCommand.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.geometry.Point2D;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.CollectionUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.Collection;
import java.util.HashMap;

/**
 * layout labels command
 * Daniel Huson, 2024
 */
public class LayoutLabelsCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public LayoutLabelsCommand(DrawView view, RootPosition rootPosition, Collection<Node> nodes) {
		super("layout labels");

		var nodeOldLayoutMap = new HashMap<Integer, Point2D>();
		var nodeNewLayoutMap = new HashMap<Integer, Point2D>();


		for (var v : nodes) {
			if (v.getInfo() instanceof RichTextLabel label) {
				nodeOldLayoutMap.put(v.getId(), new Point2D(label.getLayoutX(), label.getLayoutY()));
			}
		}

		if (!nodeOldLayoutMap.isEmpty()) {
			undo = () -> {
				for (var entry : nodeOldLayoutMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v != null) {
						var label = DrawView.getLabel(v);
						label.setLayoutX(entry.getValue().getX());
						label.setLayoutY(entry.getValue().getY());
					}
				}
			};
			redo = () -> {
				if (nodeNewLayoutMap.isEmpty()) {
					var nodeRootLocationMap = new HashMap<Node, RootPosition>();
					if (rootPosition == null) {
						var components = ConnectedComponents.components(view.getGraph());
						for (var component : components) {
							var intersection = CollectionUtils.intersection(component, nodes);
							if (!intersection.isEmpty()) {
								var rootLocation = RootPosition.compute(component);
								for (var v : intersection) {
									nodeRootLocationMap.put(v, rootLocation);
								}
							}
						}
					}
					for (var v : nodes) {
						var label = DrawView.getLabel(v);
						var layout = computeLabelLayout(nodeRootLocationMap.getOrDefault(v, rootPosition), v, label);
						nodeNewLayoutMap.put(v.getId(), layout);
					}
				}
				for (var entry : nodeNewLayoutMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v != null) {
						var label = DrawView.getLabel(v);
						label.setLayoutX(entry.getValue().getX());
						label.setLayoutY(entry.getValue().getY());
					}
				}
			};
		} else {
			undo = null;
			redo = null;
		}
	}

	/**
	 * compute label layout coordinates based on root location
	 *
	 * @param rootPosition root location
	 * @param label        label
	 * @return layout coordinates (relative to node position)
	 */
	public static Point2D computeLabelLayout(RootPosition rootPosition, Node v, RichTextLabel label) {
		label.applyCss();
		return switch (rootPosition.side()) {
			case Top -> new Point2D(-Math.max(10, label.getWidth() / 2), 5);
			case Bottom -> new Point2D(-Math.max(10, label.getWidth() / 2), -5 - Math.max(14, label.getHeight()));
			case Right -> new Point2D(-label.getWidth() - 5, -Math.max(7, label.getHeight() / 2));
			case Left -> new Point2D(10, -Math.max(7, label.getHeight() / 2));
			case Center -> {
				var points = v.adjacentEdgesStream(false)
						.map(e -> (v == e.getTarget() ? PathUtils.getPointAwayFromEnd(DrawView.getPath(e), 5) : PathUtils.getPointAwayFromStart(DrawView.getPath(e), 5)))
						.toList();
				var referenceLocation = (points.isEmpty() ? rootPosition.location() :
						new Point2D(points.stream().mapToDouble(Point2D::getX).average().orElse(0.0), points.stream().mapToDouble(Point2D::getY).average().orElse(0.0)));
				var direction = DrawView.getPoint(v).subtract(referenceLocation);
				var offset = direction.multiply(20 / Math.max(1, direction.magnitude()));
				offset.subtract(0.5 * label.getWidth(), 0.5 * label.getHeight());
				yield offset;
			}
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
		Platform.runLater(undo);
	}

	@Override
	public void redo() {
		Platform.runLater(redo);
	}
}

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
import phylosketch.view.DrawPane;
import phylosketch.view.RootLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * layout labels command
 * Daniel Huson, 2024
 */
public class LayoutLabelsCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	private final Map<Integer, Point2D> nodeOldLayoutMap = new HashMap<>();
	private final Map<Integer, Point2D> nodeNewLayoutMap = new HashMap<>();

	public LayoutLabelsCommand(DrawPane view, Collection<Node> nodes) {
		super("layout labels");

		for (var v : nodes) {
			if (v.getInfo() instanceof RichTextLabel label) {
				nodeOldLayoutMap.put(v.getId(), new Point2D(label.getLayoutX(), label.getLayoutY()));
			}
		}

		var nodeRootLocationMap = new HashMap<Node, RootLocation>();
		{
			var components = ConnectedComponents.components(view.getGraph());
			for (var component : components) {
				var intersection = CollectionUtils.intersection(component, nodes);
				if (!intersection.isEmpty()) {
					var rootLocation = RootLocation.compute(component);
					for (var v : intersection) {
						nodeRootLocationMap.put(v, rootLocation);
					}
				}
			}
		}
		for (var v : nodes) {
			var label = view.getLabel(v);
			var layout = computeLabelLayout(nodeRootLocationMap.get(v), label);
			nodeNewLayoutMap.put(v.getId(), layout);
		}

		if (nodeOldLayoutMap.isEmpty()) {
			undo = null;
		} else {
			undo = () -> {
				for (var entry : nodeOldLayoutMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v != null) {
						var label = view.getLabel(v);
						label.setLayoutX(entry.getValue().getX());
						label.setLayoutY(entry.getValue().getY());
					}
				}
			};
		}
		if (nodeNewLayoutMap.isEmpty()) {
			redo = null;
		} else {
			redo = () -> {
				for (var entry : nodeNewLayoutMap.entrySet()) {
					var v = view.getGraph().findNodeById(entry.getKey());
					if (v != null) {
						var label = view.getLabel(v);
						label.setLayoutX(entry.getValue().getX());
						label.setLayoutY(entry.getValue().getY());
					}
				}
			};
		}
	}

	/**
	 * compute label layout coordinates based on root location
	 *
	 * @param rootLocation root location
	 * @param label        label
	 * @return layout coordinates (relative to node position)
	 * todo: add support for central root
	 */
	public static Point2D computeLabelLayout(RootLocation rootLocation, RichTextLabel label) {
		label.applyCss();
		return switch (rootLocation) {
			case Top, Center -> new Point2D(-Math.max(10, label.getWidth() / 2), 5);
			case Bottom -> new Point2D(-Math.max(10, label.getWidth() / 2), -5 - Math.max(14, label.getHeight()));
			case Right -> new Point2D(-label.getWidth() - 5, -Math.max(7, label.getHeight() / 2));
			case Left -> new Point2D(10, -Math.max(7, label.getHeight() / 2));
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

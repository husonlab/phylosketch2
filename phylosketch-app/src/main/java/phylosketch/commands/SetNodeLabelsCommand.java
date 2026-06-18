/*
 * SetNodeLabelsCommand.java Copyright (C) 2025 Daniel H. Huson
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
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.CollectionUtils;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;
import phylosketch.view.TraversalByLayout;

import java.util.*;
import java.util.stream.Collectors;

/**
 * set the node labels command
 * Daniel Huson, 11.2024
 */
public class SetNodeLabelsCommand extends UndoableRedoableCommand {
	public enum How {
		ABC, abc, t1t2t3, x1x2x3, none;

		public static How which(String name) {
			return switch (name.charAt(0)) {
				case 'a' -> abc;
				case 't' -> t1t2t3;
				case 'x' -> x1x2x3;
				case 'A' -> ABC;
				default -> none;
			};
		}
	}

	public enum Use {
		leaves, internal, all;

		public static Use which(String name) {
			return switch (Character.toLowerCase(name.charAt(0))) {
				case 'l' -> leaves;
				case 'i' -> internal;
				default -> all;
			};
		}
	}

	private Runnable undo;
	private Runnable redo;

	private final Map<Integer, String> oldMap = new HashMap<>();
	private final Map<Integer, Point2D> oldLocationMap = new HashMap<>();

	private String newLabel;
	private final Map<Integer, String> newMap = new HashMap<>();
	private final Map<Integer, Point2D> newLocationMap = new HashMap<>();

	/**
	 * constructor
	 *
	 * @param view     the window
	 * @param nodes    the nodes
	 * @param newLabel the new label to be applied to all given nodes
	 */
	public SetNodeLabelsCommand(DrawView view, RootPosition rootPosition, Collection<Node> nodes, String newLabel) {
		super("set node labels");

		var nodeRootMap = new HashMap<Node, RootPosition>();
		if (rootPosition == null) {
			var seen = new HashSet<Node>();
			for (var v : nodes) {
				if (!seen.contains(v)) {
					var component = ConnectedComponents.component(v);
					var rootPosition1 = RootPosition.compute(component);
					for (var w : component) {
						nodeRootMap.put(w, rootPosition1);
					}
					seen.addAll(component);
				}
			}
		}

		for (var v : nodes) {
			var label = DrawView.getLabel(v);
			oldMap.put(v.getId(), label.getText());
			oldLocationMap.put(v.getId(), new Point2D(label.getLayoutX(), label.getLayoutY()));
			var labelLayout = LayoutLabelsCommand.computeLabelLayout(nodeRootMap.getOrDefault(v, rootPosition), v, label);
			newMap.put(v.getId(), newLabel);
			newLocationMap.put(v.getId(), new Point2D(labelLayout.getX(), labelLayout.getY()));
		}
		if (!oldMap.isEmpty()) {
			undo = () -> {
				for (var id : oldMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = DrawView.getLabel(v);
					label.setText(oldMap.get(id));
					label.setLayoutX(oldLocationMap.get(id).getX());
					label.setLayoutY(oldLocationMap.get(id).getY());
					view.getGraph().setLabel(v, DrawView.getLabel(v).getRawText());
				}
			};
			redo = () -> {
				for (var id : newLocationMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = DrawView.getLabel(v);
					label.setText(newMap.get(id));
					label.setLayoutX(newLocationMap.get(id).getX());
					label.setLayoutY(newLocationMap.get(id).getY());
					view.getGraph().setLabel(v, DrawView.getLabel(v).getRawText());
				}
			};
		}
	}

	/**
	 * label nodes using the how and use directives
	 *
	 * @param view
	 * @param howString
	 * @param useString
	 * @param unique
	 */
	public SetNodeLabelsCommand(DrawView view, String howString, String useString, boolean unique) {
		super("set node labels");

		var how = How.which(howString);
		var use = Use.which(useString);

		var nodes = view.getNodeSelection().getSelectedItems().stream().
				filter(v -> switch (use) {
					case leaves -> v.isLeaf();
					case internal -> !v.isLeaf();
					case all -> true;
				}).collect(Collectors.toSet());

		if (nodes.isEmpty()) {
			view.getGraph().nodeStream().filter(v -> switch (use) {
				case leaves -> v.isLeaf();
				case internal -> !v.isLeaf();
				case all -> true;
			}).forEach(nodes::add);
		}

		if (!nodes.isEmpty()) {
			var nodeRootMap = new HashMap<Node, RootPosition>();
			{
				var seen = new HashSet<Node>();
				for (var v : nodes) {
					if (!seen.contains(v)) {
						var component = ConnectedComponents.component(v);
						var rootPosition = RootPosition.compute(component);
						for (var w : component) {
							nodeRootMap.put(w, rootPosition);
						}
						seen.addAll(component);
					}
				}
			}

			for (var v : nodes) {
				var label = DrawView.getLabel(v);
				oldMap.put(v.getId(), label.getText());
				oldLocationMap.put(v.getId(), new Point2D(label.getLayoutX(), label.getLayoutY()));

				var labelLayout = LayoutLabelsCommand.computeLabelLayout(nodeRootMap.get(v), v, label);
				newLocationMap.put(v.getId(), new Point2D(labelLayout.getX(), labelLayout.getY()));
			}
			var seen = new HashSet<String>();
			if (unique) {
				seen.addAll(view.getSelectedOrAllNodes().stream().filter(v -> !nodes.contains(v)).map(v -> DrawView.getLabel(v).getRawText()).filter(s -> !s.isBlank()).toList());
			}
			computeNewLabels(view, nodes, how, seen, newMap);

			undo = () -> {
				for (var id : oldMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = DrawView.getLabel(v);
					label.setText(oldMap.get(id));
					label.setLayoutX(oldLocationMap.get(id).getX());
					label.setLayoutY(oldLocationMap.get(id).getY());
					view.getGraph().setLabel(v, DrawView.getLabel(v).getRawText());
				}
			};
			redo = () -> {
				for (var id : newLocationMap.keySet()) {
					var v = view.getGraph().findNodeById(id);
					var label = DrawView.getLabel(v);
					label.setText(newMap.get(id));
					label.setLayoutX(newLocationMap.get(id).getX());
					label.setLayoutY(newLocationMap.get(id).getY());
					view.getGraph().setLabel(v, DrawView.getLabel(v).getRawText());
				}
			};
		}
	}

	private void computeNewLabels(DrawView view, Collection<Node> nodes, How how, Set<String> seen, Map<Integer, String> map) {
		for (var component : ConnectedComponents.components(view.getGraph())) {
			if (CollectionUtils.intersects(component, nodes)) {
				var rootLocation = RootPosition.compute(component);
				TraversalByLayout.apply(component.stream().filter(v -> v.getInDegree() == 0).toList(), nodes, rootLocation, v -> newMap.put(v.getId(), createLabel(how, seen)));
			}
		}
	}

	private String createLabel(How how, Set<String> seen) {
		return switch (how) {
			case ABC -> {
				var id = 0;
				var label = "A";
				while (seen.contains(label)) {
					id++;
					int letter = ('A' + (id % 26));
					int number = id / 26;
					label = (char) letter + (number > 0 ? "_" + number : "");
				}
				seen.add(label);
				yield label;
			}
			case abc -> {
				var id = 0;
				String label = "a";
				while (seen.contains(label)) {
					id++;
					var letter = ('a' + (id % 26));
					var number = id / 26;
					label = (char) letter + (number > 0 ? "_" + number : "");
				}
				seen.add(label);
				yield label;
			}
			case t1t2t3 -> {
				var id = 1;
				var label = "t" + id;
				while (seen.contains(label)) {
					id++;
					label = "t" + id;

				}
				seen.add(label);
				yield label;
			}
			case x1x2x3 -> {
				var id = 1;
				var label = "x" + id;
				while (seen.contains(label)) {
					id++;
					label = "x" + id;

				}
				seen.add(label);
				yield label;
			}
			case none -> "";
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
}

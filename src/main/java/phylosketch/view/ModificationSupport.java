/*
 * ModificationSupport.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.MenuItem;
import jloda.fx.util.RunAfterAWhile;
import jloda.graph.Edge;
import phylosketch.commands.*;
import phylosketch.window.MainWindowController;

import java.util.stream.Collectors;

public class ModificationSupport {

	public static ModificationSupport setup(DrawView view, MainWindowController controller) {
		return new ModificationSupport(view, controller);
	}

	private ModificationSupport(DrawView view, MainWindowController controller) {
		var currentMendItem = new SimpleObjectProperty<MenuItem>(this, "currentMendItem", null);

		controller.getMergeNodesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new MergeNodesCommand(view, view.getNodeSelection().getSelectedItems())));
		controller.getMergeNodesMenuItem().setDisable(true);

		controller.getDeleteThruNodesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new RemoveThruNodesCommand(view, view.getSelectedOrAllNodes())));
		controller.getDeleteThruNodesMenuItem().setDisable(true);

		controller.getReverseEdgesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new ReverseEdgesCommand(view, view.getEdgeSelection().getSelectedItems())));
		controller.getReverseEdgesMenuItem().setDisable(true);

		controller.getCrossEdgesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new FixCrossingEdgesCommand(view, view.getSelectedOrAllNodes())));
		controller.getCrossEdgesMenuItem().setDisable(true);

		controller.getDeclareRootMenuItem().setOnAction(a -> {
			var e = (view.getEdgeSelection().size() == 1 ? view.getEdgeSelection().getSelectedItem() : null);
			var v = (e == null && view.getNodeSelection().size() == 1 ? view.getNodeSelection().getSelectedItem() : null);
			if (e != null || v != null)
				view.getUndoManager().doAndAdd(new RerootCommand(view, v, e));
		});
		controller.getDeclareRootMenuItem().setDisable(true);

		controller.getDeclareTransferAcceptorMenuItem().setOnAction(a -> view.getUndoManager().doAndAdd(new DeclareTransferAcceptorEdgesCommand(view, view.getEdgeSelection().getSelectedItems())));
		controller.getDeclareTransferAcceptorMenuItem().setDisable(true);

		controller.getApplyModificationMenuItem().textProperty().bind(Bindings.createStringBinding(() -> currentMendItem.get() != null ? "Apply " + currentMendItem.get().getText() : "Apply", currentMendItem));
		controller.getApplyModificationMenuItem().setOnAction(e -> {
			if (currentMendItem.get() != null)
				currentMendItem.get().fire();
		});
		controller.getApplyModificationMenuItem().disableProperty().bind(currentMendItem.isNull());

		controller.getInduceMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new InduceCommand(view, view.getNodeSelection().getSelectedItems())));
		controller.getInduceMenuItem().setDisable(true);

		var invalidationListener = (InvalidationListener) e ->
				RunAfterAWhile.applyInFXThread(ModificationSupport.this, () -> {
					controller.getInduceMenuItem().setDisable(view.getNodeSelection().size() < 2 || view.getNodeSelection().size() == view.getGraph().getNumberOfNodes());

					currentMendItem.set(null);

					controller.getDeleteThruNodesMenuItem().setDisable(true);
					controller.getCrossEdgesMenuItem().setDisable(true);
					controller.getMergeNodesMenuItem().setDisable(true);
					controller.getDeclareRootMenuItem().setDisable(true);
					controller.getDeclareTransferAcceptorMenuItem().setDisable(true);
					controller.getReverseEdgesMenuItem().setDisable(true);

					if (view.getMode() != DrawView.Mode.Edit)
						return;


					if (view.getEdgeSelection().size() > 0 && view.getNodeSelection().size() == 0) {
						if (view.getEdgeSelection().size() == view.getEdgeSelection().getSelectedItems().stream().map(Edge::getTarget).filter(v -> v.getInDegree() > 1).collect(Collectors.toSet()).size()) {
							controller.getDeclareTransferAcceptorMenuItem().setDisable(false);
							if (currentMendItem.get() == null)
								currentMendItem.set(controller.getDeclareTransferAcceptorMenuItem());
						}

						controller.getReverseEdgesMenuItem().setDisable(false);
						if (currentMendItem.get() == null)
							currentMendItem.set(controller.getReverseEdgesMenuItem());
					}

					for (var v : view.getSelectedOrAllNodes()) {
						if (v.getInDegree() == 1 && v.getOutDegree() == 1 && DrawView.getLabel(v).getRawText().isBlank()) {
							controller.getDeleteThruNodesMenuItem().setDisable(false);
							if (currentMendItem.get() == null)
								currentMendItem.set(controller.getDeleteThruNodesMenuItem());
						}
						if (v.getInDegree() == 2 && v.getOutDegree() == 2 && DrawView.getLabel(v).getRawText().isBlank()) {
							controller.getCrossEdgesMenuItem().setDisable(false);
							if (currentMendItem.get() == null)
								currentMendItem.set(controller.getCrossEdgesMenuItem());
						}
						if (!controller.getCrossEdgesMenuItem().isDisable() && !controller.getCrossEdgesMenuItem().isDisable())
							break;
					}
					if (view.getNodeSelection().size() >= 2) {
						controller.getMergeNodesMenuItem().setDisable(false);
						if (currentMendItem.get() == null)
							currentMendItem.set(controller.getMergeNodesMenuItem());
					}

					if (view.getNodeSelection().size() == 1) {
						var v = view.getNodeSelection().getSelectedItem();
						while (true) {
							if (v.getInDegree() == 1)
								v = v.getParent();
							else {
								if (v.getInDegree() == 0) {
									controller.getDeclareRootMenuItem().setDisable(false);
									if (currentMendItem.get() == null)
										currentMendItem.set(controller.getDeclareRootMenuItem());
								}
								return;
							}
						}
					} else if (view.getEdgeSelection().size() == 1) {
						var f = view.getEdgeSelection().getSelectedItem();
						if (f.nodes().containsAll(view.getNodeSelection().getSelectedItems())) {
							var v = f.getTarget();

							while (true) {
								if (v.getInDegree() == 1)
									v = v.getParent();
								else {
									if (v.getInDegree() == 0) {
										controller.getDeclareRootMenuItem().setDisable(false);
										if (currentMendItem.get() == null)
											currentMendItem.set(controller.getDeclareRootMenuItem());
									}
									return;
								}
							}
						}
					}
				});

		view.getNodeSelection().getSelectedItems().addListener(invalidationListener);
		view.getEdgeSelection().getSelectedItems().addListener(invalidationListener);
		view.getGraphFX().lastUpdateProperty().addListener(invalidationListener);
		view.modeProperty().addListener(invalidationListener);
	}
}

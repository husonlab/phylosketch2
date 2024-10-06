/*
 * NodeInteraction.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Shape;
import jloda.graph.Graph;
import phylosketch.main.PhyloSketch;

import java.util.HashSet;
import java.util.Set;

/**
 * node interaction
 * Daniel Huson, 9.2024
 */
public class NodeInteraction {
	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	public static void setup(DrawPane view, Runnable runDoubleClickSelection) {

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (javafx.scene.Node n : c.getAddedSubList()) {
						if (n instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
							shape.setOnMouseClicked(me -> {
								if (me.isStillSincePress()) {
									if (me.getClickCount() == 1) {
										if (!me.isAltDown() && PhyloSketch.isDesktop()) {
											if (!me.isShiftDown()) {
												view.getNodeSelection().clearSelection();
												view.getEdgeSelection().clearSelection();
											}
										}
										view.getNodeSelection().toggleSelection(v);
										me.consume();
									} else if (me.getClickCount() == 2) {
										Platform.runLater(runDoubleClickSelection);
										me.consume();
									}
								}
							});

							shape.setOnMousePressed(me -> {
								if (view.getMode() == DrawPane.Mode.Move && !view.getNodeSelection().isSelected(v)) {
									if (PhyloSketch.isDesktop() && !me.isShiftDown()) {
										view.getNodeSelection().clearSelection();
										view.getEdgeSelection().clearSelection();
									}
									view.getNodeSelection().select(v);
								}
								if ((view.getMode() == DrawPane.Mode.Edit || view.getMode() == DrawPane.Mode.Move)
									&& view.getNodeSelection().isSelected(v)) {
									mouseDownX = me.getScreenX();
									mouseDownY = me.getScreenY();
									mouseX = mouseDownX;
									mouseY = mouseDownY;
									me.consume();
								}
							});

							shape.setOnMouseDragged(me -> {
								if ((view.getMode() == DrawPane.Mode.Edit || view.getMode() == DrawPane.Mode.Move) && view.getNodeSelection().isSelected(v)) {
									var previous = view.screenToLocal(mouseX, mouseY);
									var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									moveNodes(view.getGraph(), view.getNodeSelection().getSelectedItems(), d.getX(), d.getY(), false);
									mouseX = me.getScreenX();
									mouseY = me.getScreenY();
									me.consume();
								}
							});

							shape.setOnMouseReleased(me -> {
								if ((view.getMode() == DrawPane.Mode.Edit || view.getMode() == DrawPane.Mode.Move) && view.getNodeSelection().isSelected(v)) {
									var nodes = new HashSet<>(view.getNodeSelection().getSelectedItems());
									var previous = view.screenToLocal(mouseDownX, mouseDownY);
									var location = view.screenToLocal(mouseX, mouseY);
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									view.getUndoManager().add("move nodes", () -> {
										moveNodes(view.getGraph(), nodes, -d.getX(), -d.getY(), true);
									}, () -> moveNodes(view.getGraph(), nodes, d.getX(), d.getY(), true));
									me.consume();
								}
							});

							shape.setOnMouseEntered(me -> {
								shape.setStrokeWidth(shape.getStrokeWidth() + 4);
							});

							shape.setOnMouseExited(me -> {
								shape.setStrokeWidth(shape.getStrokeWidth() - 4);
							});
						}
					}
				}
			}
		});
	}

	public static void moveNodes(Graph graph, Set<jloda.graph.Node> nodes, double dx, double dy, boolean normalizePaths) {
		for (var v : nodes) {
			if (v.getData() instanceof Shape shape) {
				shape.setTranslateX(shape.getTranslateX() + dx);
				shape.setTranslateY(shape.getTranslateY() + dy);
			}
		}
		MoveEdges.apply(graph, nodes, dx, dy, normalizePaths);
	}

}

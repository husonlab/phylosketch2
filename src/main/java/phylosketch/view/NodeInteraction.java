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
import phylosketch.commands.MoveNodesCommand;
import phylosketch.main.PhyloSketch;

import java.util.HashSet;

/**
 * node interaction
 * Daniel Huson, 9.2024
 */
public class NodeInteraction {
	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	/**
	 * setup node interactions
	 * Note that creation of new nodes is setup in PaneInteraction
	 *
	 * @param view
	 * @param runDoubleClickSelection
	 */
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
										view.getNodeSelection().select(v);
										Platform.runLater(runDoubleClickSelection);
									}
								}
								me.consume();
							});

							shape.setOnMousePressed(me -> {
								if (view.getMode() == DrawPane.Mode.Move) {
									mouseDownX = me.getScreenX();
									mouseDownY = me.getScreenY();
									mouseX = mouseDownX;
									mouseY = mouseDownY;
									me.consume();
								}
							});

							shape.setOnMouseDragged(me -> {
								if (view.getMode() == DrawPane.Mode.Move) {
									if (!view.getNodeSelection().isSelected(v)) {
										if (PhyloSketch.isDesktop() && !me.isShiftDown()) {
											view.getNodeSelection().clearSelection();
											view.getEdgeSelection().clearSelection();
										}
										view.getNodeSelection().select(v);
									}

									var previous = view.screenToLocal(mouseX, mouseY);
									var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									MoveNodesCommand.moveNodesAndEdges(view.getGraph(), view.getNodeSelection().getSelectedItems(), d.getX(), d.getY(), false);
									mouseX = me.getScreenX();
									mouseY = me.getScreenY();

									me.consume();
								}
							});

							shape.setOnMouseReleased(me -> {
								if (view.getMode() == DrawPane.Mode.Move) {
									var nodes = new HashSet<>(view.getNodeSelection().getSelectedItems());
									var previous = view.screenToLocal(mouseDownX, mouseDownY);
									var location = view.screenToLocal(mouseX, mouseY);
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									view.getUndoManager().add(new MoveNodesCommand(view, nodes, d.getX(), d.getY()));
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

}

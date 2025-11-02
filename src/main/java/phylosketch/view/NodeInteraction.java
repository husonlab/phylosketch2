/*
 * NodeInteraction.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.paint.Color;
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

	private static boolean inMove = false;

	/**
	 * setup node interactions
	 * Note that creation of new nodes is setup in PaneInteraction
	 *
	 * @param view
	 * @param runSelectionButton
	 */
	public static void setup(DrawView view, BooleanProperty resizeMode, DragLineBoxSupport dragLineBoxSupport, Runnable runSelectionButton) {
		var hDragLine = dragLineBoxSupport.hDragLine();
		var vDragLine = dragLineBoxSupport.vDragLine();
		var box = dragLineBoxSupport.box();

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (javafx.scene.Node n : c.getAddedSubList()) {
						if (n instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
							if (false) { // todo: don't want this
								shape.setOnContextMenuRequested(a -> {
									if (view.getNodeSelection().isSelected(v) && view.getMode() == DrawView.Mode.Move) {
										var resizeItem = new CheckMenuItem("Resize Mode");
										resizeItem.setSelected(resizeMode.get());
										resizeItem.setOnAction(d -> resizeMode.set(!resizeMode.get()));
										var contextMenu = new ContextMenu(resizeItem);
										contextMenu.show(shape, a.getScreenX(), a.getScreenY());
									}
									a.consume();
								});
							}

							shape.setOnMouseClicked(me -> {
								if (me.isStillSincePress() && !me.isControlDown()) {
									if (PhyloSketch.isDesktop() && me.isShiftDown()) {
										view.getNodeSelection().toggleSelection(v);
									} else if (!view.getNodeSelection().isSelected(v)) {
										if (PhyloSketch.isDesktop()) {
											view.getEdgeSelection().clearSelection();
											view.getNodeSelection().clearSelection();
										}
										view.getNodeSelection().select(v);
									} else {
										if (false) Platform.runLater(runSelectionButton);
									}
								}
								me.consume();
							});

							shape.setOnMousePressed(me -> {
								inMove = (view.getMode() == DrawView.Mode.Move) || (view.getMode() == DrawView.Mode.Sketch && me.isShiftDown());
								if (inMove) {
									mouseDownX = me.getSceneX();
									mouseDownY = me.getSceneY();
									mouseX = mouseDownX;
									mouseY = mouseDownY;
									me.consume();
								}
							});

							shape.setOnMouseDragged(me -> {
								if (inMove) {
									if (!view.getNodeSelection().isSelected(v)) {
										if (PhyloSketch.isDesktop() && !me.isShiftDown()) {
											view.getNodeSelection().clearSelection();
											view.getEdgeSelection().clearSelection();
										}
										view.getNodeSelection().select(v);
									}

									var previous = view.sceneToLocal(mouseX, mouseY);
									var location = view.sceneToLocal(me.getSceneX(), me.getSceneY());
									if (location.getX() >= box.getX() && location.getY() >= box.getY()) {
										var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
										MoveNodesCommand.moveNodesAndEdges(view, view.getNodeSelection().getSelectedItems(), d.getX(), d.getY(), false);
									}
									mouseX = me.getSceneX();
									mouseY = me.getSceneY();

									if (true) {
										var hasX = view.getGraph().nodeStream().mapToDouble(u -> DrawView.getPoint(u).getX()).anyMatch(x -> Math.abs(location.getX() - x) <= 1);
										var hasY = view.getGraph().nodeStream().mapToDouble(u -> DrawView.getPoint(u).getY()).anyMatch(y -> Math.abs(location.getY() - y) <= 1);
										if (hasX) {
											if (!view.getOtherGroup().getChildren().contains(hDragLine))
												view.getOtherGroup().getChildren().add(hDragLine);
										} else
											view.getOtherGroup().getChildren().remove(hDragLine);

										if (hasY) {
											if (!view.getOtherGroup().getChildren().contains(vDragLine))
												view.getOtherGroup().getChildren().add(vDragLine);
										} else
											view.getOtherGroup().getChildren().remove(vDragLine);
									}
									if (false)
									me.consume();
								}
							});

							shape.setOnMouseReleased(me -> {
								if (inMove) {
									var nodes = new HashSet<>(view.getNodeSelection().getSelectedItems());
									var previous = view.screenToLocal(mouseDownX, mouseDownY);
									var location = view.screenToLocal(mouseX, mouseY);
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									view.getUndoManager().add(new MoveNodesCommand(view, nodes, d.getX(), d.getY()));
									me.consume();
									inMove = false;
								}
								view.getOtherGroup().getChildren().remove(hDragLine);
								view.getOtherGroup().getChildren().remove(vDragLine);
							});

							shape.setOnMouseEntered(me -> {
								var color = (shape.getFill() != null ? shape.getFill() : (shape.getStroke() != null ? shape.getStroke() : Color.GRAY));
								var hoverShadow = new HoverShadow(color, 5);
								hoverShadow.setOffsetY(0);
								if (shape.getEffect() != null) {
									hoverShadow.setInput(shape.getEffect());
								}
								shape.setEffect(hoverShadow);
							});

							shape.setOnMouseExited(me -> {
								if (shape.getEffect() instanceof HoverShadow hoverShadow && hoverShadow.getInput() != null) {
									shape.setEffect(hoverShadow.getInput());
								} else {
									shape.setEffect(null);
								}
							});
						}
					}
				}
			}
		});
	}
}

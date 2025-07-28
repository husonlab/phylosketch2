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
import javafx.scene.shape.Line;
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

	private static final Line lineX = createDragLine(true);
	private static final Line lineY = createDragLine(false);

	/**
	 * setup node interactions
	 * Note that creation of new nodes is setup in PaneInteraction
	 *
	 * @param view
	 * @param runSelectionButton
	 */
	public static void setup(DrawView view, BooleanProperty resizeMode, Runnable runSelectionButton) {
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
								inMove = (view.getMode() == DrawView.Mode.Move) || (view.getMode() == DrawView.Mode.Edit && me.isShiftDown());
								if (inMove) {
									mouseDownX = me.getScreenX();
									mouseDownY = me.getScreenY();
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

									var previous = view.screenToLocal(mouseX, mouseY);
									var location = view.screenToLocal(me.getScreenX(), me.getScreenY());
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									MoveNodesCommand.moveNodesAndEdges(view, view.getNodeSelection().getSelectedItems(), d.getX(), d.getY(), false);
									mouseX = me.getScreenX();
									mouseY = me.getScreenY();

									view.getOtherGroup().getChildren().remove(lineX);
									view.getOtherGroup().getChildren().remove(lineY);

									for (var q : view.getNodeSelection().getSelectedItems()) {
										var qPoint = DrawView.getPoint(q);
										var hasX = view.getGraph().nodeStream().filter(u -> !view.getNodeSelection().isSelected(u)).mapToDouble(u -> DrawView.getPoint(u).getX()).anyMatch(x -> Math.abs(qPoint.getX() - x) < 2);
										if (hasX) {
											if (!view.getOtherGroup().getChildren().contains(lineX))
												view.getOtherGroup().getChildren().add(lineX);
											lineX.setTranslateX(qPoint.getX());
											lineX.setTranslateY(qPoint.getY());
										} else
											view.getOtherGroup().getChildren().remove(lineX);

										var hasY = view.getGraph().nodeStream().filter(u -> !view.getNodeSelection().isSelected(u)).mapToDouble(u -> DrawView.getPoint(u).getY()).anyMatch(y -> Math.abs(qPoint.getY() - y) < 2);

										if (hasY) {
											if (!view.getOtherGroup().getChildren().contains(lineY))
												view.getOtherGroup().getChildren().add(lineY);
											lineY.setTranslateX(qPoint.getX());
											lineY.setTranslateY(qPoint.getY());
										} else
											view.getOtherGroup().getChildren().remove(lineY);
									}
									me.consume();
								}
							});

							shape.setOnMouseReleased(me -> {
								view.getOtherGroup().getChildren().remove(lineX);
								view.getOtherGroup().getChildren().remove(lineY);

								if (inMove) {
									var nodes = new HashSet<>(view.getNodeSelection().getSelectedItems());
									var previous = view.screenToLocal(mouseDownX, mouseDownY);
									var location = view.screenToLocal(mouseX, mouseY);
									var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
									view.getUndoManager().add(new MoveNodesCommand(view, nodes, d.getX(), d.getY()));
									me.consume();
									inMove = false;
								}
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

	public static Line createDragLine(boolean horizontal) {
		var line = (horizontal ? new Line(0, -50, 0, 50) : new Line(-50, 0, 50, 0));
		line.setId("drag-line");
		line.getStrokeDashArray().setAll(3.0, 3.0);
		line.setStroke(Color.LIGHTGRAY);
		return line;
	}
}

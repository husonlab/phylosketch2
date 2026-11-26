/*
 * SetupNodeInteraction.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import jloda.fx.util.ProgramProperties;
import jloda.util.Single;
import phylosketch.commands.MoveNodesEdgesCommand;

import java.util.ArrayList;

/**
 * node interaction
 * Daniel Huson, 9.2024
 */
public class SetupNodeInteraction {
	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	public static boolean inMove = false;
	private static boolean moveStarted = false; // has the drag left the dead zone around the press?

	private static MoveNodesEdgesCommand moveNodesEdgesCommand;

	private static final double LIFT_SCALE = 1.8; // how much a node grows while it is lifted (see HoldToMove)

	/**
	 * setup node interactions
	 * Note that creation of new nodes is setup in SetupPaneInteraction
	 *
	 * @param view
	 * @param grid while it is on, dragged nodes move in grid steps
	 */
	public static void apply(DrawView view, BooleanProperty resizeMode, DragLineBoxSupport dragLineBoxSupport, ReadOnlyBooleanProperty multiTouch, NodeGrid grid) {
		var hDragLine = dragLineBoxSupport.hDragLine();
		var vDragLine = dragLineBoxSupport.vDragLine();
		var box = dragLineBoxSupport.box();

		var nodesToDrag = new ArrayList<jloda.graph.Node>();
		// where the mouse would have taken the grabbed node, were it not for the grid
		var dragPoint = new Single<Point2D>();

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (javafx.scene.Node n : c.getAddedSubList()) {
						if (n instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
							if (!ProgramProperties.isDesktop())
								HoldToMove.install(shape);

							shape.setOnMouseClicked(me -> {
								if (!multiTouch.get() && me.isStillSincePress() && !me.isControlDown()) {
									if (ProgramProperties.isDesktop() && me.isShiftDown()) {
										view.getNodeSelection().toggleSelection(v);
									} else if (!view.getNodeSelection().isSelected(v)) {
										if (ProgramProperties.isDesktop()) {
											view.getEdgeSelection().clearSelection();
											view.getNodeSelection().clearSelection();
										}
										view.getNodeSelection().select(v);
									}
								}
								me.consume();
							});

							shape.setOnMousePressed(me -> {
								nodesToDrag.clear();
								if (!multiTouch.get()) {
									if (!view.getNodeSelection().isSelected(v)) {
										nodesToDrag.add(v);
									} else {
										nodesToDrag.addAll(view.getNodeSelection().getSelectedItems());
									}

									// In Move mode a drag moves the node. In Sketch mode a drag on a node draws an edge, unless shift is
									// down. On a touch screen, the node is moved in Sketch mode by holding it until it is lifted and
									// then dragging it (see HoldToMove), so that the mode need not be changed to move something
									inMove = (view.getMode() == DrawView.Mode.Move)
											 || (view.getMode() == DrawView.Mode.Sketch && me.isShiftDown());
									moveStarted = false;
									mouseDownX = me.getSceneX();
									mouseDownY = me.getSceneY();
									mouseX = mouseDownX;
									mouseY = mouseDownY;
									if (inMove) {
										SetupPaneInteraction.inRubberBandSelection.set(false); // moving a node never selects
										moveNodesEdgesCommand = new MoveNodesEdgesCommand(view, nodesToDrag, null);
										dragPoint.set(view.getLocation(v));
										me.consume();
									} else if (!ProgramProperties.isDesktop() && view.getMode() == DrawView.Mode.Sketch) {
										HoldToMove.press(shape, me, () -> !multiTouch.get() && v.getOwner() != null, () -> {
											// the press has also started an edge in the pane, which is given up
											SetupPaneInteraction.cancelDrawingEdge(view);
											SetupPaneInteraction.inRubberBandSelection.set(false);
											inMove = true;
											moveStarted = false;
											moveNodesEdgesCommand = new MoveNodesEdgesCommand(view, nodesToDrag, null);
											dragPoint.set(view.getLocation(v));
											shape.setScaleX(LIFT_SCALE);
											shape.setScaleY(LIFT_SCALE);
											HoldToMove.showCaption(nodesToDrag.size() == 1 ? "Move node" : "Move %d nodes".formatted(nodesToDrag.size()));
										}, () -> {
											shape.setScaleX(1);
											shape.setScaleY(1);
										});
									}
								}
							});

							shape.setOnMouseDragged(me -> {
								HoldToMove.drag(me);
								if (!multiTouch.get() && inMove) {
									if (!moveStarted) {
										// on a touch screen a tap rarely stays put: the nodes only start to move once the finger has
										// left the area of a tap, and they then catch up with it
										if (ProgramProperties.isDesktop() || Math.hypot(me.getSceneX() - mouseDownX, me.getSceneY() - mouseDownY) > SetupPaneInteraction.TAP_TOLERANCE) {
											moveStarted = true;
											HoldToMove.setMoved();
										} else {
											me.consume();
											return;
										}
									}
									if (false && !view.getNodeSelection().isSelected(v)) {
										if (ProgramProperties.isDesktop() && !me.isShiftDown()) {
											view.getNodeSelection().clearSelection();
											view.getEdgeSelection().clearSelection();
										}
										view.getNodeSelection().select(v);
									}

									var previous = view.sceneToLocal(mouseX, mouseY);
									var location = view.sceneToLocal(me.getSceneX(), me.getSceneY());
									if (location.getX() >= box.getX() && location.getY() >= box.getY()) {
										var d = new Point2D(location.getX() - previous.getX(), location.getY() - previous.getY());
										if (grid.isSnap()) {
											// in grid steps: the grabbed node goes to the grid point nearest to where the mouse would
											// have taken it, and the other dragged nodes keep their offsets to it
											dragPoint.set(dragPoint.get().add(d));
											d = grid.snap(dragPoint.get()).subtract(view.getLocation(v));
											if (d.getX() != 0 || d.getY() != 0)
												moveNodesEdgesCommand.moveNodesAndEdges(d.getX(), d.getY());
										} else {
											// todo: show node moving:
											moveNodesEdgesCommand.moveNodesAndEdges(d.getX(), d.getY());
										}
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
								}
							});

							shape.setOnMouseReleased(me -> {
								if (inMove) {
									if (!multiTouch.get()) {
										if (moveStarted && moveNodesEdgesCommand.isUndoable())
											view.getUndoManager().add(moveNodesEdgesCommand);
									}
									me.consume();
									inMove = false;
									moveNodesEdgesCommand = null;
								}
								HoldToMove.release(shape);
								view.getOtherGroup().getChildren().remove(hDragLine);
								view.getOtherGroup().getChildren().remove(vDragLine);
							});

							shape.setOnMouseEntered(me -> {
								// a finger does not hover: on a touch screen the shadow would come up on the press and flicker
								// as a dragged node catches up with the finger
								if (!ProgramProperties.isDesktop())
									return;
								var color = (shape.getFill() != null ? shape.getFill() : (shape.getStroke() != null ? shape.getStroke() : Color.GRAY));
								var hoverShadow = new HoverShadow(color, 5);
								hoverShadow.setOffsetY(0);
								if (shape.getEffect() != null) {
									hoverShadow.setInput(shape.getEffect());
								}
								shape.setEffect(hoverShadow);
							});

							shape.setOnMouseExited(me -> {
								if (!ProgramProperties.isDesktop())
									return;
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

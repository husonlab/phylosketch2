/*
 * SetupEdgeInteraction.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.shape.PathElement;
import jloda.fx.util.ProgramProperties;
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;

import java.util.ArrayList;
import java.util.List;

import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * edge interaction
 * Daniel Huson, 9.2024
 */
public class SetupEdgeInteraction {
	private static boolean inMove;
	private static boolean moveStarted;
	private static double mouseDownX;
	private static double mouseDownY;

	private static int pathIndex;
	private static List<PathElement> originalElements;
	private static EdgePath.Type originalType;

	private static double mouseX;
	private static double mouseY;

	private static boolean insideEdge;

	private static final double LIFT_WIDTH = 2; // how much wider an edge is drawn while it is lifted (see HoldToMove)

	/**
	 * setup edge interactions
	 * Note that interactive creation of new edges ist setup in SetupPaneInteraction
	 *
	 * @param view
	 */
	public static void apply(DrawView view, BooleanProperty resizeMode, BooleanProperty multiTouch) {
		view.getEdgesGroup().getChildren().addListener((ListChangeListener<? super Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (Node n : c.getAddedSubList()) {
						if (n instanceof EdgePath path && path.getUserData() instanceof jloda.graph.Edge e) {
							if (!ProgramProperties.isDesktop())
								HoldToMove.install(path);

							path.setOnContextMenuRequested(a -> {
								if (!multiTouch.get() && view.getEdgeSelection().isSelected(e)
										&& (view.getMode() == DrawView.Mode.Move || !ProgramProperties.isDesktop() && view.getMode() == DrawView.Mode.Sketch)) {
									var resizeItem = new CheckMenuItem("Resize Mode");
									resizeItem.setSelected(resizeMode.get());
									resizeItem.setOnAction(d -> resizeMode.set(!resizeMode.get()));
									var contextMenu = new ContextMenu(resizeItem);
									contextMenu.show(path, a.getScreenX(), a.getScreenY());
								}
								a.consume();
							});

							path.setOnMouseClicked(me -> {
								if (!multiTouch.get()) {
									var local = path.sceneToLocal(me.getSceneX(), me.getSceneY());
									if (path.isPointOnStroke(local.getX(), local.getY())) {
										if (me.isStillSincePress() && !me.isControlDown()) {
											if (ProgramProperties.isDesktop() && me.isShiftDown()) {
												view.getEdgeSelection().toggleSelection(e);
											} else if (!view.getEdgeSelection().isSelected(e)) {
												if (ProgramProperties.isDesktop()) {
													view.getEdgeSelection().clearSelection();
													view.getNodeSelection().clearSelection();
												}
												view.getEdgeSelection().select(e);
											}
										}
									}
									me.consume();
								}
							});

							path.setOnMousePressed(me -> {
								if (!multiTouch.get()) {
									var local = path.sceneToLocal(me.getSceneX(), me.getSceneY());
									if (path.isPointOnStroke(local.getX(), local.getY())) {
										// In Move mode a drag reshapes the edge. In Sketch mode a drag on an edge draws a new edge from there,
										// unless shift is down. On a touch screen, the edge is reshaped in Sketch mode by holding it until
										// it is lifted and then dragging it (see HoldToMove), as a node is moved
										inMove = (view.getMode() == DrawView.Mode.Move)
												 || (view.getMode() == DrawView.Mode.Sketch && me.isShiftDown());
										moveStarted = false;
										if (!inMove && !ProgramProperties.isDesktop() && view.getMode() == DrawView.Mode.Sketch) {
											var sceneX = me.getSceneX();
											var sceneY = me.getSceneY();
											var strokeWidth = path.getStrokeWidth();
											HoldToMove.press(path, me, () -> !multiTouch.get() && e.getOwner() != null, () -> {
												// the press has also started an edge in the pane, which is given up
												SetupPaneInteraction.cancelDrawingEdge(view);
												SetupPaneInteraction.inRubberBandSelection.set(false);
												inMove = true;
												moveStarted = false;
												pathIndex = findIndex(path, local);
												originalElements = new ArrayList<>(path.getElements());
												originalType = path.getType();
												mouseX = mouseDownX = sceneX;
												mouseY = mouseDownY = sceneY;
												path.setStrokeWidth(strokeWidth + LIFT_WIDTH);
												HoldToMove.showCaption("Reshape edge");
											}, () -> path.setStrokeWidth(strokeWidth));
										}

										if (inMove) {
											pathIndex = findIndex(path, local);
											originalElements = new ArrayList<>(path.getElements());
											originalType = path.getType();
											mouseX = mouseDownX = me.getSceneX();
											mouseY = mouseDownY = me.getSceneY();
											SetupPaneInteraction.inRubberBandSelection.set(false); // reshaping an edge never selects
											me.consume();
										} else
											pathIndex = -1;
									} else inMove = false;
								} else inMove = false;
							});
							path.setOnMouseDragged(me -> {
								HoldToMove.drag(me);
								if (!multiTouch.get() && inMove) {
									if (!moveStarted) {
										// on a touch screen a tap rarely stays put: the edge is only reshaped once the finger has left the
										// area of a tap
										if (ProgramProperties.isDesktop() || Math.hypot(me.getSceneX() - mouseDownX, me.getSceneY() - mouseDownY) > SetupPaneInteraction.TAP_TOLERANCE) {
											moveStarted = true;
											HoldToMove.setMoved();
										} else {
											me.consume();
											return;
										}
									}
									if (!view.getEdgeSelection().isSelected(e)) {
										if (ProgramProperties.isDesktop() && !me.isShiftDown()) {
											view.getNodeSelection().clearSelection();
											view.getEdgeSelection().clearSelection();
										}
										view.getEdgeSelection().select(e);
									}

									if (pathIndex != -1) {
										if (path.getType() != EdgePath.Type.Freeform) {
											path.changeToFreeform();
										}
										var local = view.sceneToLocal(me.getSceneX(), me.getSceneY());
										var d = local.subtract(view.sceneToLocal(mouseX, mouseY));
										PathReshape.apply(path, pathIndex, d.getX(), d.getY());
										mouseX = me.getSceneX();
										mouseY = me.getSceneY();
									}
									me.consume();
								}
							});
							path.setOnMouseReleased(me -> {
								if (!multiTouch.get() && inMove) {
									if (pathIndex != -1 && moveStarted && !me.isStillSincePress()) {
										var theOriginalElements = originalElements;
										var refinedElements = PathNormalize.apply(path, 2, 5);
										path.getElements().setAll(refinedElements);

										view.getUndoManager().add("reshape",
												() -> path.set(theOriginalElements, originalType),
												() -> path.getElements().setAll(refinedElements));
									}
									me.consume();
									inMove = false;
								}
								HoldToMove.release(path);
							});
							path.setOnMouseEntered(me -> {
								if (!ProgramProperties.isDesktop())
									return; // a finger does not hover
								var local = path.sceneToLocal(me.getSceneX(), me.getSceneY());
								if (path.isPointOnStroke(local.getX(), local.getY())) {
									var hoverShadow = new HoverShadow(path.getStroke(), 3);
									if (path.getEffect() != null) {
										hoverShadow.setInput(path.getEffect());
									}
									path.setEffect(hoverShadow);
									insideEdge = true;
								} else insideEdge = false;
							});
							path.setOnMouseExited(me -> {
								if (insideEdge) {
									if (path.getEffect() instanceof HoverShadow hoverShadow && hoverShadow.getInput() != null) {
										path.setEffect(hoverShadow.getInput());
									} else {
										path.setEffect(null);
									}
									insideEdge = false;
								}
							});
						}
					}
				}
			}
		});
	}

	private static int findIndex(EdgePath path, Point2D local) {
		var bestDistance = 10.0;
		int index = -1;

		var tmp = path.copy();
		tmp.changeToFreeform();

		var elements = tmp.getElements();
		for (int i = 1; i + 1 < elements.size(); i++) {  // can't be first or last
			var element = elements.get(i);
			var coordinates = getCoordinates(element);
			if (coordinates.distance(local) < bestDistance) {
				bestDistance = coordinates.distance(local);
				index = i;
			}
		}
		return index;
	}
}

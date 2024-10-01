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
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathNormalize;
import phylosketch.paths.PathReshape;

import java.util.List;

import static phylosketch.paths.PathUtils.copy;
import static phylosketch.paths.PathUtils.getCoordinates;

/**
 * edge interaction
 * Daniel Huson, 9.2024
 */
public class EdgeInteraction {
	private static double mouseDownX;
	private static double mouseDownY;

	private static int pathIndex;
	private static List<PathElement> originalElements;

	private static double mouseX;
	private static double mouseY;

	public static void setup(DrawPane view, Runnable runDoubleClickSelection) {
		view.getEdgesGroup().getChildren().addListener((ListChangeListener<? super Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (Node n : c.getAddedSubList()) {
						if (n instanceof Path path && path.getUserData() instanceof jloda.graph.Edge e) {
							path.setOnMouseClicked(me -> {
								if (me.isStillSincePress()) {
									if (me.getClickCount() == 1) {
										if (!me.isAltDown() && PhyloSketch.isDesktop()) {
											if (!me.isShiftDown()) {
												view.getNodeSelection().clearSelection();
												view.getEdgeSelection().clearSelection();
											}
											view.getEdgeSelection().toggleSelection(e);
											if (!me.isControlDown()) {
												view.getNodeSelection().setSelected(e.getSource(), view.getEdgeSelection().isSelected(e));
												view.getNodeSelection().setSelected(e.getTarget(), view.getEdgeSelection().isSelected(e));
											}
										}
										me.consume();
									} else if (me.getClickCount() == 2) {
										Platform.runLater(runDoubleClickSelection);
										me.consume();
									}
								}
							});

							path.setOnMousePressed(me -> {
								if (view.getMode() == DrawPane.Mode.Move && !view.getEdgeSelection().isSelected(e)) {
									if (PhyloSketch.isDesktop() && !me.isShiftDown()) {
										view.getNodeSelection().clearSelection();
										view.getEdgeSelection().clearSelection();
									}
									view.getEdgeSelection().select(e);
								}
								if ((view.getMode() == DrawPane.Mode.Edit || view.getMode() == DrawPane.Mode.Move)
									&& view.getEdgeSelection().isSelected(e)) {
									mouseDownX = me.getScreenX();
									mouseDownY = me.getScreenY();
									var local = view.screenToLocal(mouseDownX, mouseDownY);
									pathIndex = findIndex(path, local);
									originalElements = copy(path.getElements());
									mouseX = me.getScreenX();
									mouseY = me.getScreenY();
									me.consume();
								} else
									pathIndex = -1;
							});
							path.setOnMouseDragged(me -> {
								if (pathIndex != -1) {
									var local = view.screenToLocal(me.getScreenX(), me.getScreenY());
									var d = local.subtract(view.screenToLocal(mouseX, mouseY));
									PathReshape.apply(path, pathIndex, d.getX(), d.getY());
									mouseX = me.getScreenX();
									mouseY = me.getScreenY();
									me.consume();
								}
							});
							path.setOnMouseReleased(me -> {
								if (pathIndex != -1 && !me.isStillSincePress()) {
									var theOriginalElements = originalElements;
									var refinedElements = PathNormalize.apply(path, 2, 5);
									path.getElements().setAll(refinedElements);

									view.getUndoManager().add("reshape",
											() -> path.getElements().setAll(theOriginalElements),
											() -> path.getElements().setAll(refinedElements));
									me.consume();
								}
							});
							path.setOnMouseEntered(me -> {
								path.setStrokeWidth(path.getStrokeWidth() + 4);
							});
							path.setOnMouseExited(me -> {
								path.setStrokeWidth(path.getStrokeWidth() - 4);
							});
						}
					}
				}
			}
		});
	}

	private static int findIndex(Path path, Point2D local) {
		var bestDistance = 10.0;
		int index = -1;

		ObservableList<PathElement> elements = path.getElements();
		for (int i = 0; i < elements.size(); i++) {
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

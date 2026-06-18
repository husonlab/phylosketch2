/*
 * SetupResize.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.shape.Rectangle;
import jloda.fx.icons.MaterialIcons;
import phylosketch.commands.MoveNodesEdgesCommand;
import phylosketch.commands.ScaleNodesEdgesCommand;
import phylosketch.paths.BBox;
import phylosketch.utils.ScrollPaneUtils;

/**
 * implements interactive resizing of selected subtrees
 * Daniel Huson, 9.2024
 */
public class SetupResize {

	private static double mouseDownX;
	private static double mouseDownY;

	private static double mouseX;
	private static double mouseY;

	private static MoveNodesEdgesCommand moveNodesEdgesCommand;
	private static ScaleNodesEdgesCommand scaleNodesEdgesCommand;

	public static void apply(DrawView view, BooleanProperty resizeMode, ReadOnlyBooleanProperty multiTouch) {
		final var rectangle = new Rectangle();
		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");

		rectangle.setStyle("-fx-stroke-dash-array: 3 3;-fx-fill: transparent;-fx-stroke: gray;");

		var resizeGroup = new Group(rectangle, resizeHandle);

		InvalidationListener updateRectangle = e -> {
			if (view.getNodeSelection().size() > 1 && resizeMode.get()) {
				updateSizeAndLocation(view, rectangle, resizeHandle);
				if (!view.getOtherGroup().getChildren().contains(resizeGroup)) {
					view.getOtherGroup().getChildren().add(resizeGroup);
				}
			} else {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(view, () -> view.getOtherGroup().getChildren().remove(resizeGroup));
			}
		};

		view.getNodeSelection().getSelectedItems().addListener(updateRectangle);
		resizeMode.addListener(updateRectangle);

		resizeHandle.setOnMouseClicked(Event::consume);
		rectangle.setOnMouseClicked(Event::consume);

		rectangle.setOnContextMenuRequested(a -> {
			if (!multiTouch.get()) {
				var resizeItem = new CheckMenuItem("Resize Mode");
				resizeItem.setSelected(resizeMode.get());
				resizeItem.setOnAction(d -> resizeMode.set(!resizeMode.get()));
				var contextMenu = new ContextMenu(resizeItem);
				contextMenu.show(rectangle, a.getScreenX(), a.getScreenY());
				a.consume();
			}
		});

		rectangle.setOnMousePressed(me -> {
			if (!multiTouch.get()) {
				mouseX = mouseDownX = me.getScreenX();
				mouseY = mouseDownY = me.getScreenY();
				moveNodesEdgesCommand = new MoveNodesEdgesCommand(view, view.getNodeSelection().getSelectedItems(), () -> updateRectangle.invalidated(null));
				me.consume();
			}
		});


		rectangle.setOnMouseDragged(me -> {
			if (!multiTouch.get()) {
				// todo: show nodes moving
				var diff = view.screenToLocal(me.getScreenX(), me.getScreenY()).subtract(view.screenToLocal(mouseX, mouseY));
				moveNodesEdgesCommand.moveNodesAndEdges(diff.getX(), diff.getY());
				mouseX = me.getScreenX();
				mouseY = me.getScreenY();
				updateSizeAndLocation(view, rectangle, resizeHandle);
				me.consume();
			}
		});

		rectangle.setOnMouseReleased(me -> {
			if (!multiTouch.get()) {
				if (moveNodesEdgesCommand.isUndoable())
					view.getUndoManager().add(moveNodesEdgesCommand);
				moveNodesEdgesCommand = null;
				me.consume();
			}
		});

		resizeHandle.setOnMousePressed(me -> {
			if (!multiTouch.get()) {
				mouseX = mouseDownX = me.getScreenX();
				mouseY = mouseDownY = me.getScreenY();
				scaleNodesEdgesCommand = new ScaleNodesEdgesCommand(view, view.getNodeSelection().getSelectedItems(), () -> updateRectangle.invalidated(null));
				me.consume();
			}
		});

		resizeHandle.setOnMouseDragged(me -> {
			if (!multiTouch.get()) {
				var diff = view.screenToLocal(me.getScreenX(), me.getScreenY()).subtract(view.screenToLocal(mouseX, mouseY));
				if (me.isShiftDown()) {
					var min = Math.min(diff.getX(), diff.getY());
					diff = new Point2D(min, min);
				}

				if (rectangle.getWidth() + diff.getX() >= 20 && rectangle.getHeight() + diff.getY() >= 20) {
					scaleNodesEdgesCommand.scaleNodesAndEdges(diff.getX(), diff.getY());
					rectangle.setWidth(rectangle.getWidth() + diff.getX());
					rectangle.setHeight(rectangle.getHeight() + diff.getY());
					resizeHandle.setTranslateX(resizeHandle.getTranslateX() + diff.getX());
					resizeHandle.setTranslateY(resizeHandle.getTranslateY() + diff.getY());
					mouseX = me.getScreenX();
					mouseY = me.getScreenY();
					me.consume();
				}
			}
		});

		resizeHandle.setOnMouseReleased(me -> {
			if (!multiTouch.get()) {
				if (scaleNodesEdgesCommand.isUndoable()) {
					view.getUndoManager().add(scaleNodesEdgesCommand);
				}
				scaleNodesEdgesCommand = null;
				me.consume();
			}
		});
	}

	private static void updateSizeAndLocation(DrawView view, Rectangle rectangle, javafx.scene.Node resizeHandle) {
		var bbox = BBox.compute(view.getNodeSelection().getSelectedItems());
		rectangle.setX(bbox.xMin() - 12);
		rectangle.setY(bbox.yMin() - 12);
		rectangle.setWidth(bbox.width() + 24);
		rectangle.setHeight(bbox.height() + 24);
		resizeHandle.setTranslateX(rectangle.getX() + rectangle.getWidth());
		resizeHandle.setTranslateY(rectangle.getY() + rectangle.getHeight());
	}
}

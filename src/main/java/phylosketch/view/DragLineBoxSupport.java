/*
 * DragLineBoxSupport.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import jloda.fx.util.RunAfterAWhile;
import phylosketch.capturepane.pane.CapturePane;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathUtils;

/**
 * sets up drag lines (for showing when there is alignment with other nodes) and the bounding box that is required
 * to ensure that we don't fly off to the left or up when drawing or dragging
 *
 * @param hDragLine
 * @param vDragLine
 * @param box       Daniel Huson, 10.2025
 */
public record DragLineBoxSupport(Line hDragLine, Line vDragLine, Rectangle box) {
	private final static double DRAG_LINE_SIZE = 50;
	private final static double MARGIN = PhyloSketch.isDesktop() ? 200 : 100;

	/**
	 * sets up drag lines (for showing when there is alignment with other nodes) and the bounding box that is required
	 * to ensure that we don't fly off to the left or up when drawing or dragging
	 *
	 * @param view the view
	 * @return the drag lines and boc
	 */
	public static DragLineBoxSupport setup(DrawView view, CapturePane capturePane) {
		var box = new Rectangle(-MARGIN, -MARGIN, 2 * MARGIN, 2 * MARGIN);
		box.setMouseTransparent(true);
		box.setFill(Color.TRANSPARENT);
		box.setStroke(Color.TRANSPARENT);
		view.getChildren().add(box);

		Runnable updateBox = () -> {
			var minX = Double.MAX_VALUE;
			var maxX = Double.MIN_VALUE;
			var minY = Double.MAX_VALUE;
			var maxY = Double.MIN_VALUE;

			for (var v : view.getGraph().nodes()) {
				var local = view.getLocation(v);
				minX = Math.min(minX, local.getX());
				maxX = Math.max(maxX, local.getX());
				minY = Math.min(minY, local.getY());
				maxY = Math.max(maxY, local.getY());
			}

			if (capturePane.isShowCapture()) {
				var sceneBox = capturePane.getMainPane().localToScene(capturePane.getMainPane().getBoundsInLocal());
				var local = view.sceneToLocal(sceneBox);
				minX = Math.min(minX, local.getMinX());
				maxX = Math.max(maxX, local.getMaxX());
				minY = Math.min(minY, local.getMinY());
				maxY = Math.max(maxY, local.getMaxX());
			}

			for (var f : view.getGraph().edges()) {
				var path = (Path) f.getData();
				for (var local : PathUtils.getPoints(path)) {
					minX = Math.min(minX, local.getX());
					maxX = Math.max(maxX, local.getX());
					minY = Math.min(minY, local.getY());
					maxY = Math.max(maxY, local.getY());
				}
			}

			box.setX(minX - MARGIN);
			box.setY(minY - MARGIN);
			box.setWidth(maxX - minX + 2 * MARGIN);
			box.setHeight(maxY - minY + 2 * MARGIN);
			view.setPrefWidth(box.getWidth());
			view.setPrefHeight(box.getHeight());
		};

		view.getUndoManager().undoStackSizeProperty().addListener(e -> RunAfterAWhile.apply(updateBox, updateBox));
		view.getNodesGroup().getChildren().addListener((InvalidationListener) e -> RunAfterAWhile.apply(updateBox, updateBox));

		var hDragLine = createDragLine(true);
		hDragLine.setId("h-drag-line");
		hDragLine.setMouseTransparent(true);
		hDragLine.setStroke(Color.GRAY);

		var vDragLine = createDragLine(false);
		vDragLine.setId("v-drag-line");
		vDragLine.setMouseTransparent(true);
		vDragLine.setStroke(Color.GRAY);

		EventHandler<MouseEvent> movedOrDraggedHandler = me -> {
			var localMouseLocation = view.sceneToLocal(me.getSceneX(), me.getSceneY());
			hDragLine.setTranslateX(Math.max(localMouseLocation.getX(), box.getX() + DRAG_LINE_SIZE));
			hDragLine.setTranslateY(Math.max(localMouseLocation.getY(), box.getY() + DRAG_LINE_SIZE));
			vDragLine.setTranslateX(Math.max(localMouseLocation.getX(), box.getX() + DRAG_LINE_SIZE));
			vDragLine.setTranslateY(Math.max(localMouseLocation.getY(), box.getY() + DRAG_LINE_SIZE));
		};

		view.addEventHandler(MouseEvent.MOUSE_MOVED, movedOrDraggedHandler);
		view.addEventHandler(MouseEvent.MOUSE_DRAGGED, movedOrDraggedHandler);

		return new DragLineBoxSupport(hDragLine, vDragLine, box);

	}

	public static Line createDragLine(boolean horizontal) {
		var line = (horizontal ? new Line(0, -DRAG_LINE_SIZE, 0, DRAG_LINE_SIZE) : new Line(-DRAG_LINE_SIZE, 0, DRAG_LINE_SIZE, 0));
		line.setId("drag-line");
		line.getStrokeDashArray().setAll(3.0, 3.0);
		line.setStroke(Color.LIGHTGRAY);
		return line;
	}
}

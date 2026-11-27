/*
 * RootTool.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.pane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import phylosketch.view.RootPosition;

/**
 * the root marker: marks the root of the drawing in the picture and shows which way the drawing grows
 * <p>
 * The marker is a ring around the root with an arrow that points the way the drawing grows, or four arrows if it
 * grows from the center. The ring is filled when the marker is on a line. The origin of the marker is the root, and
 * the marker is moved by its translation. It is drawn in the zoomed view, so that it stays on the root, but keeps
 * its size on the screen whatever the zoom: zoomed out far enough to see a large picture, a marker that shrank
 * with the picture could no longer be seen, let alone touched. It can be pressed in an area the size of a finger.
 * Dragging moves it, a click turns it to the next side. Neither is a step in the undo list: where the marker is
 * says how to capture, it is not part of the document.
 * Daniel Huson, 1.2025, 11.2026
 */
public class RootTool extends Group {
	private static final double RING_RADIUS = 8;
	private static final double TOUCH_RADIUS = 22;
	private static final double ARROW_START = 14;
	private static final double ARROW_END = 38;
	private static final double ARROW_HEAD = 8;

	private static final String OUTLINE = "-fx-stroke: white; -fx-fill: white;";
	private static final String ACCENT = "-fx-stroke: -fx-accent; -fx-fill: -fx-accent;";

	private final ObjectProperty<RootPosition.Side> rootSide = new SimpleObjectProperty<>(this, "rootSide", null);
	private final BooleanProperty onLine = new SimpleBooleanProperty(this, "onLine", false);

	private final Scale scale = new Scale(1, 1, 0, 0);

	private Runnable onDropped;

	private double mouseX;
	private double mouseY;
	private double oldTranslateX;
	private double oldTranslateY;
	private boolean dragged;

	/**
	 * constructs the root marker
	 */
	public RootTool() {
		getTransforms().add(scale);

		var touchArea = new Circle(TOUCH_RADIUS);
		touchArea.setStyle("-fx-fill: transparent;");

		var halo = new Circle(RING_RADIUS);
		halo.setStyle("-fx-stroke: white; -fx-fill: transparent; -fx-stroke-width: 7;");
		var ring = new Circle(RING_RADIUS);
		ring.setStyle("-fx-stroke: -fx-accent; -fx-fill: transparent; -fx-stroke-width: 3;");
		var disc = new Circle(RING_RADIUS);
		disc.setStyle("-fx-fill: -fx-accent; -fx-stroke: transparent;");
		disc.visibleProperty().bind(onLine);
		var center = new Circle(2);
		center.setStyle("-fx-fill: white; -fx-stroke: transparent;");
		center.visibleProperty().bind(onLine);

		// an arrow for each way that the drawing can grow, shown for the side that the root is on, all four for the center
		var right = createArrow(1, 0);
		right.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Left).or(rootSide.isEqualTo(RootPosition.Side.Center)));
		var left = createArrow(-1, 0);
		left.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Right).or(rootSide.isEqualTo(RootPosition.Side.Center)));
		var down = createArrow(0, 1);
		down.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Top).or(rootSide.isEqualTo(RootPosition.Side.Center)));
		var up = createArrow(0, -1);
		up.visibleProperty().bind(rootSide.isEqualTo(RootPosition.Side.Bottom).or(rootSide.isEqualTo(RootPosition.Side.Center)));

		getChildren().setAll(touchArea, right, left, down, up, halo, ring, disc, center);

		Tooltip.install(this, new Tooltip("Root marker: drag it onto the root of the tree. Click it to change the direction in which the tree grows"));
		setCursor(Cursor.OPEN_HAND);

		rootSide.set(RootPosition.Side.Left);

		setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			oldTranslateX = getTranslateX();
			oldTranslateY = getTranslateY();
			dragged = false;
			e.consume();
		});

		setOnMouseDragged(e -> {
			if (getParent() != null) {
				var prev = getParent().screenToLocal(mouseX, mouseY);
				var loc = getParent().screenToLocal(e.getScreenX(), e.getScreenY());
				if (prev != null && loc != null) {
					setTranslateX(oldTranslateX + loc.getX() - prev.getX());
					setTranslateY(oldTranslateY + loc.getY() - prev.getY());
					dragged = true;
				}
			}
			e.consume();
		});

		setOnMouseReleased(e -> {
			if (dragged) {
				dragged = false;
				if (e.isStillSincePress()) { // a click with an unsteady hand: not a move
					setTranslateX(oldTranslateX);
					setTranslateY(oldTranslateY);
				} else if (onDropped != null)
					onDropped.run();
			}
			e.consume();
		});

		setOnMouseClicked(e -> {
			if (e.isStillSincePress()) {
				setRootSide(switch (getRootSide()) {
					case Left -> RootPosition.Side.Top;
					case Top -> RootPosition.Side.Right;
					case Right -> RootPosition.Side.Bottom;
					case Bottom -> RootPosition.Side.Center;
					case Center -> RootPosition.Side.Left;
				});
			}
			e.consume();
		});
	}

	/**
	 * an arrow that starts beside the ring and points away from it, white underneath so that it shows on any picture
	 */
	private static Node createArrow(double dx, double dy) {
		var arrow = new Group();
		for (var outline : new boolean[]{true, false}) {
			var shaft = new Line(dx * ARROW_START, dy * ARROW_START, dx * (ARROW_END - ARROW_HEAD), dy * (ARROW_END - ARROW_HEAD));
			shaft.setStyle((outline ? OUTLINE : ACCENT) + "-fx-stroke-width: " + (outline ? 7 : 3) + ";");
			shaft.setStrokeLineCap(StrokeLineCap.ROUND);
			// the head: its tip, and the two corners of its base, which lies across the arrow
			var head = new Polygon(dx * ARROW_END, dy * ARROW_END,
					dx * (ARROW_END - ARROW_HEAD - 2) - dy * (ARROW_HEAD - 2), dy * (ARROW_END - ARROW_HEAD - 2) + dx * (ARROW_HEAD - 2),
					dx * (ARROW_END - ARROW_HEAD - 2) + dy * (ARROW_HEAD - 2), dy * (ARROW_END - ARROW_HEAD - 2) - dx * (ARROW_HEAD - 2));
			head.setStyle((outline ? OUTLINE : ACCENT) + "-fx-stroke-width: " + (outline ? 4 : 0) + "; -fx-stroke-line-join: round;");
			arrow.getChildren().addAll(shaft, head);
		}
		arrow.setMouseTransparent(true);
		return arrow;
	}

	/**
	 * sets the zoom of the view that the marker is drawn in, so that the marker can undo it and keep its size on the screen
	 *
	 * @param zoom the zoom of the view
	 */
	public void setZoom(double zoom) {
		if (zoom > 0) {
			scale.setX(1.0 / zoom);
			scale.setY(1.0 / zoom);
		}
	}

	/**
	 * sets what is to be run when the user has dragged the marker and let go of it
	 */
	public void setOnDropped(Runnable onDropped) {
		this.onDropped = onDropped;
	}

	/**
	 * is the marker being dragged? As long as it is, it is not to be put anywhere else
	 */
	public boolean isDragging() {
		return dragged;
	}

	public Point2D getRootLocationOnScreen() {
		return localToScreen(0, 0);
	}

	public RootPosition.Side getRootSide() {
		return rootSide.get();
	}

	public void setRootSide(RootPosition.Side rootSide) {
		this.rootSide.set(rootSide);
	}

	public ObjectProperty<RootPosition.Side> rootSideProperty() {
		return rootSide;
	}

	public boolean isOnLine() {
		return onLine.get();
	}

	public void setOnLine(boolean onLine) {
		this.onLine.set(onLine);
	}

	public BooleanProperty onLineProperty() {
		return onLine;
	}

	/**
	 * the way the drawing grows for a root on the given side, in words
	 */
	public static String getDirection(RootPosition.Side side) {
		return switch (side) {
			case Left -> "Left to right";
			case Right -> "Right to left";
			case Top -> "Top to bottom";
			case Bottom -> "Bottom to top";
			case Center -> "Radial";
		};
	}
}

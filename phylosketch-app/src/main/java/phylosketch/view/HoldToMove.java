/*
 * HoldToMove.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;

/**
 * hold to move: how a node, edge or label is moved in Sketch mode on a touch screen
 * <p>
 * A quick drag from a node or an edge draws a new edge. To move an item, the finger is held still on it until the
 * item is "lifted", and the drag that follows moves it. A long press also asks for the context menu of the item, so
 * what happens after the lift decides: a drag moves the item and no menu is shown, and a release without a drag
 * shows the menu. The context menu event of the system is therefore swallowed for such an item, and the menu is
 * requested here when the finger goes up.
 * <p>
 * Daniel Huson, 11.2026
 */
public class HoldToMove {
	/**
	 * how long the finger has to stay put before the item is lifted; shorter than the long press of the system
	 */
	public static final Duration HOLD = Duration.millis(350);

	private static final PauseTransition timer = new PauseTransition(HOLD);

	private static Node item; // the item that is being pressed
	private static final ReadOnlyBooleanWrapper lifted = new ReadOnlyBooleanWrapper(false); // has the finger stayed put for long enough?
	private static boolean moved; // has the item been moved since it was lifted?
	private static boolean requestingMenu; // the context menu event under way is the one fired here
	private static Runnable onDrop;
	private static double downX, downY, downScreenX, downScreenY;

	private static final double RING_RADIUS = 32; // wider than a finger tip
	private static StackPane feedbackPane;
	private static Label caption;
	private static FadeTransition captionFade;
	private static Group ring;
	private static Arc ringArc;
	private static Timeline ringFill;
	private static ParallelTransition ringPulse;

	private HoldToMove() {
	}

	/**
	 * the context menu of the item is shown when a hold ends without a drag, and not when the system reports a long press
	 */
	public static void install(Node item) {
		item.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
			if (!requestingMenu && !e.isKeyboardTrigger())
				e.consume();
		});
	}

	/**
	 * sets up the feedback for the gestures of a touch screen in the given pane. The finger covers the item that it is
	 * on, so the item itself cannot show what is happening to it. There are two parts: a ring around the finger that
	 * closes during the hold, and so shows when the item is lifted, and a caption at the top of the pane that names
	 * what the drag is doing. Neither can be touched.
	 */
	public static void installFeedback(StackPane pane) {
		feedbackPane = pane;

		caption = new Label();
		caption.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 14; -fx-padding: 4 12 4 12; -fx-text-fill: white; -fx-font-size: 14;");
		caption.setMouseTransparent(true);
		caption.setVisible(false);
		caption.setOpacity(0);
		StackPane.setAlignment(caption, Pos.TOP_CENTER);
		StackPane.setMargin(caption, new Insets(10));
		captionFade = new FadeTransition(Duration.millis(150), caption);

		var track = new Circle(RING_RADIUS);
		track.setStyle("-fx-fill: transparent; -fx-stroke: rgba(128,128,128,0.35); -fx-stroke-width: 3;");
		ringArc = new Arc(0, 0, RING_RADIUS, RING_RADIUS, 90, 0);
		ringArc.setType(ArcType.OPEN);
		ringArc.setStyle("-fx-fill: transparent; -fx-stroke: -fx-accent; -fx-stroke-width: 3; -fx-stroke-line-cap: round;");
		ring = new Group(track, ringArc);
		ring.setManaged(false);
		ring.setMouseTransparent(true);
		ring.setVisible(false);
		ringFill = new Timeline(new KeyFrame(HOLD, new KeyValue(ringArc.lengthProperty(), -360)));
		var fade = new FadeTransition(Duration.millis(250), ring);
		fade.setToValue(0);
		var grow = new ScaleTransition(Duration.millis(250), ring);
		grow.setToX(1.3);
		grow.setToY(1.3);
		ringPulse = new ParallelTransition(fade, grow);
		ringPulse.setOnFinished(e -> ring.setVisible(false));

		pane.getChildren().addAll(ring, caption);
	}

	/**
	 * names what the drag under way is doing, at the top of the pane
	 */
	public static void showCaption(String text) {
		if (caption != null) {
			caption.setText(text);
			if (!caption.isVisible() || captionFade.getToValue() == 0) {
				captionFade.stop();
				caption.setVisible(true);
				captionFade.setOnFinished(null);
				captionFade.setToValue(1);
				captionFade.play();
			}
		}
	}

	/**
	 * is a finger on an item, and it is still open whether this is a hold?
	 */
	public static boolean isPending() {
		return timer.getStatus() == Animation.Status.RUNNING;
	}

	public static void hideCaption() {
		if (caption != null && caption.isVisible() && captionFade.getToValue() != 0) {
			captionFade.stop();
			captionFade.setToValue(0);
			captionFade.setOnFinished(e -> caption.setVisible(false));
			captionFade.play();
		}
	}

	private static void startRing(double sceneX, double sceneY) {
		if (ring != null) {
			ringPulse.stop();
			var location = feedbackPane.sceneToLocal(sceneX, sceneY);
			ring.setLayoutX(location.getX());
			ring.setLayoutY(location.getY());
			ring.setScaleX(1);
			ring.setScaleY(1);
			ring.setOpacity(1);
			ringArc.setLength(0);
			ring.setVisible(true);
			ringFill.playFromStart();
		}
	}

	private static void stopRing(boolean hasLifted) {
		if (ring != null) {
			ringFill.stop();
			if (hasLifted) {
				ringArc.setLength(-360);
				ringPulse.playFromStart();
			} else {
				ringPulse.stop();
				ring.setVisible(false);
			}
		}
	}

	/**
	 * a finger has gone down on an item
	 *
	 * @param canLift asked when the hold time is over: may the item be lifted now?
	 * @param onLift  the item is lifted: show this, and prepare for moving it
	 * @param onDrop  the finger has gone up after a lift: undo what was shown
	 */
	public static void press(Node item, MouseEvent me, BooleanSupplier canLift, Runnable onLift, Runnable onDrop) {
		timer.stop();
		HoldToMove.item = item;
		HoldToMove.onDrop = onDrop;
		lifted.set(false);
		moved = false;
		downX = me.getSceneX();
		downY = me.getSceneY();
		downScreenX = me.getScreenX();
		downScreenY = me.getScreenY();
		timer.setOnFinished(e -> {
			if (HoldToMove.item == item && canLift.getAsBoolean()) {
				lifted.set(true);
				stopRing(true);
				onLift.run();
			} else
				stopRing(false);
		});
		timer.playFromStart();
		startRing(downX, downY);
	}

	/**
	 * the finger has moved: if it leaves the area of a tap before the item is lifted, this is not a hold
	 */
	public static void drag(MouseEvent me) {
		if (item != null && !lifted.get() && Math.hypot(me.getSceneX() - downX, me.getSceneY() - downY) > SetupPaneInteraction.TAP_TOLERANCE) {
			timer.stop();
			stopRing(false);
		}
	}

	/**
	 * the lifted item has been moved, so the release will not show its menu
	 */
	public static void setMoved() {
		moved = true;
	}

	/**
	 * the finger has gone up
	 */
	public static void release(Node item) {
		if (HoldToMove.item == item) {
			timer.stop();
			if (lifted.get()) {
				hideCaption();
				onDrop.run();
				if (!moved) {
					var x = downX;
					var y = downY;
					var screenX = downScreenX;
					var screenY = downScreenY;
					// later, so that the menu comes up after the click that follows this release
					Platform.runLater(() -> {
						if (item.getScene() != null) {
							requestingMenu = true;
							try {
								Event.fireEvent(item, new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, x, y, screenX, screenY, false, null));
							} finally {
								requestingMenu = false;
							}
						}
					});
				}
			}
			else
				stopRing(false);
			HoldToMove.item = null;
			lifted.set(false);
		}
	}
}

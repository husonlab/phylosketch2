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

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
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
	private static boolean lifted; // has the finger stayed put for long enough?
	private static boolean moved; // has the item been moved since it was lifted?
	private static boolean requestingMenu; // the context menu event under way is the one fired here
	private static Runnable onDrop;
	private static double downX, downY, downScreenX, downScreenY;

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
		lifted = false;
		moved = false;
		downX = me.getSceneX();
		downY = me.getSceneY();
		downScreenX = me.getScreenX();
		downScreenY = me.getScreenY();
		timer.setOnFinished(e -> {
			if (HoldToMove.item == item && canLift.getAsBoolean()) {
				lifted = true;
				onLift.run();
			}
		});
		timer.playFromStart();
	}

	/**
	 * the finger has moved: if it leaves the area of a tap before the item is lifted, this is not a hold
	 */
	public static void drag(MouseEvent me) {
		if (item != null && !lifted && Math.hypot(me.getSceneX() - downX, me.getSceneY() - downY) > SetupPaneInteraction.TAP_TOLERANCE)
			timer.stop();
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
			if (lifted) {
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
			HoldToMove.item = null;
			lifted = false;
		}
	}
}

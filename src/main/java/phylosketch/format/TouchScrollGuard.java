package phylosketch.format;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;

public final class TouchScrollGuard {

	private TouchScrollGuard() {
	}

	public static void install(ScrollPane sp, Node content, double thresholdPx) {
		final State s = new State(thresholdPx);

		// Make touch scrolling feel right on iOS
		sp.setPannable(true);
		sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

		// Capture at the content level so children don't get "tap on release" after a drag.
		content.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			s.pressX = e.getScreenX();
			s.pressY = e.getScreenY();
			s.dragging = false;
			s.pressed = true;
		});

		content.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			if (!s.pressed) return;

			double dx = e.getScreenX() - s.pressX;
			double dy = e.getScreenY() - s.pressY;
			if (!s.dragging && (dx * dx + dy * dy) >= s.threshold2) {
				s.dragging = true;

				// Optional but very effective: once we know it is a scroll gesture,
				// consume the current event so it doesn't look like a drag on a control.
				e.consume();

				// Optional: prevent accidental focus transfer (TextField keyboard pop-up, etc.)
				// If a child already got focus on press, move focus away.
				Platform.runLater(() -> {
					if (content.getScene() != null) {
						content.getScene().getRoot().requestFocus();
					}
				});
			}
		});

		content.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (s.dragging) {
				// This is the key: prevents Button/ColorPicker/TitledPane header from firing.
				e.consume();
			}
			s.pressed = false;
			s.dragging = false;
		});

		// Also suppress CLICKED if it is generated after a drag (some controls fire on click)
		content.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (s.draggingRecently) {
				e.consume();
				s.draggingRecently = false;
			}
		});

		// Mark "dragging recently" for a short grace window
		content.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			if (s.dragging) s.draggingRecently = true;
		});
		content.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (s.draggingRecently) {
				// clear after a short delay, keeping it simple
				Platform.runLater(() -> s.draggingRecently = false);
			}
		});
	}

	private static final class State {
		final double threshold2;
		double pressX, pressY;
		boolean pressed;
		boolean dragging;
		boolean draggingRecently;

		State(double thresholdPx) {
			this.threshold2 = thresholdPx * thresholdPx;
		}
	}
}
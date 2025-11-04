/*
 * WindowNotifications.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.window;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;

/**
 * Utility to show floating notification messages in a window.
 * <p>
 * Usage:
 * WindowNotifications.show(rootAnchorPane, "Hello world", MessageType.INFO);
 */
public final class WindowNotifications {

	public enum MessageType {INFO, WARNING, ERROR}

	// Durations per message type
	private static final Duration INFO_LIFETIME = Duration.seconds(8);
	private static final Duration WARNING_LIFETIME = Duration.seconds(30);
	private static final Duration ERROR_LIFETIME = Duration.seconds(120);

	// Animation settings
	private static final Duration ANIM_DURATION = Duration.millis(200);
	private static final double GAP = 8.0;
	private static final double H_MARGIN = 16.0;
	private static final double MAX_WIDTH = 420.0;

	private static final Map<AnchorPane, List<Notification>> ACTIVE = new WeakHashMap<>();
	private static final String OVERLAY_KEY = "windowNotificationsOverlay";

	private WindowNotifications() {
	}

	/**
	 * Show a notification on the given anchorPane.
	 */
	public static void show(AnchorPane anchorPane, String text, MessageType type) {
		if (anchorPane == null || text == null || type == null) return;
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> show(anchorPane, text, type));
			return;
		}

		Pane overlay = getOrCreateOverlay(anchorPane);
		List<Notification> list = ACTIVE.computeIfAbsent(anchorPane, ap -> new ArrayList<>());

		Notification notification = createNotification(anchorPane, overlay, text, type);
		list.add(notification);
		overlay.getChildren().add(notification.node);

		double paneHeight = overlay.getHeight() > 0 ? overlay.getHeight() : anchorPane.getHeight();
		if (paneHeight <= 0) paneHeight = 400; // fallback for early calls

		notification.node.setOpacity(0.0);
		notification.node.setLayoutY(paneHeight + 10);

		layoutNotifications(anchorPane);
		Platform.runLater(() -> layoutNotifications(anchorPane));

		notification.expiry.playFromStart();
	}

	/**
	 * Convenience overload with string messageType: "info", "warning", "error".
	 */
	public static void show(AnchorPane anchorPane, String text, String messageType) {
		MessageType type;
		if (messageType == null) type = MessageType.INFO;
		else switch (messageType.toLowerCase(Locale.ROOT)) {
			case "warning":
				type = MessageType.WARNING;
				break;
			case "error":
				type = MessageType.ERROR;
				break;
			default:
				type = MessageType.INFO;
		}
		show(anchorPane, text, type);
	}

	// Internal wrapper
	private static final class Notification {
		final Node node;
		final PauseTransition expiry;

		Notification(Node node, PauseTransition expiry) {
			this.node = node;
			this.expiry = expiry;
		}
	}

	// Create or reuse an overlay pane
	private static Pane getOrCreateOverlay(AnchorPane root) {
		Object existing = root.getProperties().get(OVERLAY_KEY);
		if (existing instanceof Pane) return (Pane) existing;

		Pane overlay = new Pane();
		overlay.setPickOnBounds(false);

		AnchorPane.setTopAnchor(overlay, 0.0);
		AnchorPane.setRightAnchor(overlay, 0.0);
		AnchorPane.setBottomAnchor(overlay, 0.0);
		AnchorPane.setLeftAnchor(overlay, 0.0);

		root.getChildren().add(overlay);
		root.getProperties().put(OVERLAY_KEY, overlay);

		root.heightProperty().addListener((obs, o, n) -> layoutNotifications(root));
		root.widthProperty().addListener((obs, o, n) -> layoutNotifications(root));

		return overlay;
	}

	// Create a single notification node
	private static Notification createNotification(AnchorPane root, Pane overlay, String text, MessageType type) {
		Label label = new Label(text);
		label.setWrapText(true);
		label.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

		Region spacer = new Region();
		spacer.setMinWidth(0);
		spacer.setPrefWidth(10);
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button closeButton = new Button("✖");
		closeButton.setFocusTraversable(false);
		closeButton.setStyle(
				"-fx-background-color: transparent; " +
				"-fx-text-fill: white; -fx-font-size: 11; -fx-padding: 0 0 0 8;"
		);

		HBox box = new HBox(10, label, spacer, closeButton);
		box.setPadding(new Insets(8, 12, 8, 12));
		box.setAlignment(Pos.CENTER_LEFT);
		box.setMaxWidth(MAX_WIDTH);
		box.setMinHeight(Region.USE_PREF_SIZE);
		label.maxWidthProperty().bind(box.widthProperty().subtract(40));

		String bgColor;
		Duration lifetime;
		switch (type) {
			case ERROR:
				bgColor = "#C53030";
				lifetime = ERROR_LIFETIME;
				break;
			case WARNING:
				bgColor = "#B7791F";
				lifetime = WARNING_LIFETIME;
				break;
			case INFO:
			default:
				bgColor = "#2B6CB0";
				lifetime = INFO_LIFETIME;
				break;
		}

		box.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6; -fx-border-radius: 6;");
		box.setEffect(new DropShadow(8, Color.color(0, 0, 0, 0.4)));

		PauseTransition expiry = new PauseTransition(lifetime);
		Notification notification = new Notification(box, expiry);
		expiry.setOnFinished(e -> dismiss(root, notification));
		closeButton.setOnAction(e -> dismiss(root, notification));

		return notification;
	}

	/**
	 * Stack and animate notifications bottom-up.
	 */
	private static void layoutNotifications(AnchorPane root) {
		List<Notification> list = ACTIVE.get(root);
		if (list == null || list.isEmpty()) return;

		Pane overlay = (Pane) root.getProperties().get(OVERLAY_KEY);
		if (overlay == null) return;

		double paneHeight = overlay.getHeight() > 0 ? overlay.getHeight() : root.getHeight();
		double paneWidth = overlay.getWidth() > 0 ? overlay.getWidth() : root.getWidth();
		if (paneHeight <= 0) {
			Platform.runLater(() -> layoutNotifications(root));
			return;
		}

		double maxAllowedWidth = Math.min(MAX_WIDTH, paneWidth - 2 * H_MARGIN);
		if (maxAllowedWidth <= 0) maxAllowedWidth = MAX_WIDTH;

		for (Notification n : list) {
			if (n.node instanceof Region) {
				Region r = (Region) n.node;
				r.setMaxWidth(maxAllowedWidth);
				r.setPrefWidth(maxAllowedWidth);
				r.applyCss();
				r.autosize();
			} else n.node.applyCss();
		}

		double y = paneHeight - GAP;
		List<Notification> toRemove = new ArrayList<>();

		for (int i = list.size() - 1; i >= 0; i--) {
			Notification n = list.get(i);
			double h = (n.node instanceof Region)
					? ((Region) n.node).prefHeight(-1)
					: n.node.getBoundsInParent().getHeight();

			y -= h;
			double targetY = y;

			if (targetY + h < 0) toRemove.add(n);
			else {
				double nodeWidth = (n.node instanceof Region)
						? ((Region) n.node).prefWidth(-1)
						: n.node.getBoundsInParent().getWidth();

				double x = H_MARGIN;
				if (paneWidth > 0 && nodeWidth < paneWidth - 2 * H_MARGIN)
					x = (paneWidth - nodeWidth) / 2.0;

				n.node.setLayoutX(x);
				animateToY(n.node, targetY);
			}
			y -= GAP;
		}

		if (!toRemove.isEmpty()) {
			for (Notification n : toRemove) {
				n.expiry.stop();
				overlay.getChildren().remove(n.node);
				list.remove(n);
			}
		}

		if (list.isEmpty()) ACTIVE.remove(root);
	}

	private static void animateToY(Node node, double targetY) {
		Timeline tl = new Timeline(
				new KeyFrame(Duration.ZERO,
						new KeyValue(node.layoutYProperty(), node.getLayoutY()),
						new KeyValue(node.opacityProperty(), node.getOpacity())),
				new KeyFrame(ANIM_DURATION,
						new KeyValue(node.layoutYProperty(), targetY, Interpolator.EASE_BOTH),
						new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
		);
		tl.play();
	}

	private static void dismiss(AnchorPane root, Notification notification) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> dismiss(root, notification));
			return;
		}

		List<Notification> list = ACTIVE.get(root);
		if (list == null || !list.contains(notification)) return;

		Pane overlay = (Pane) root.getProperties().get(OVERLAY_KEY);
		if (overlay == null) return;

		notification.expiry.stop();

		FadeTransition fade = new FadeTransition(ANIM_DURATION, notification.node);
		fade.setFromValue(notification.node.getOpacity());
		fade.setToValue(0.0);
		fade.setOnFinished(e -> {
			overlay.getChildren().remove(notification.node);
			list.remove(notification);
			if (list.isEmpty()) ACTIVE.remove(root);
			else layoutNotifications(root);
		});
		fade.play();
	}
}
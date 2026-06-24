/*
 * ZoomToFit.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import jloda.fx.control.ZoomableScrollPane;
import phylosketch.window.MainWindow;

public class ZoomToFit {
	private static final double PADDING = 40.0;

	public static void apply(MainWindow window) {
		var scrollPane = window.getController().getScrollPane();

		if (window.isEmpty()) {
			scrollPane.resetZoom();
			return;
		}

		var world = window.getDrawView().getWorld();

		var bounds = computeVisibleContentBounds(world);
		if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0)
			return;

		var viewport = scrollPane.getViewportBounds();
		var vw = viewport.getWidth() - 2 * PADDING;
		var vh = viewport.getHeight() - 2 * PADDING;
		if (vw <= 0 || vh <= 0)
			return;

		// bounds is in world (unscaled) coordinates, so this is the absolute zoom we want
		var targetZoom = Math.min(vw / bounds.getWidth(), vh / bounds.getHeight());

		scrollPane.zoomBy(targetZoom / scrollPane.getZoomX(), targetZoom / scrollPane.getZoomY());

		// zoomBy zooms about the viewport centre; recentre on the phylogeny once the new scale is laid out
		Platform.runLater(() -> centerOn(scrollPane, world, bounds));
	}

	private static void centerOn(ZoomableScrollPane scrollPane, Node world, Bounds worldBounds) {
		scrollPane.layout(); // ensure the scaled bounds and viewport are current

		// hvalue/vvalue run over the zoom group's bounds (the same frame the pane uses in doZoom)
		Group group = scrollPane.getContentGroup();
		var inner = group.getBoundsInLocal();
		var viewport = scrollPane.getViewportBounds();
		var vw = viewport.getWidth();
		var vh = viewport.getHeight();

		// centre of the phylogeny, expressed in the zoom group's coordinates (includes the current scale)
		var center = group.sceneToLocal(world.localToScene(worldBounds.getCenterX(), worldBounds.getCenterY()));

		if (inner.getWidth() > vw)
			scrollPane.setHvalue(clamp((center.getX() - inner.getMinX() - vw / 2.0) / (inner.getWidth() - vw)));
		if (inner.getHeight() > vh)
			scrollPane.setVvalue(clamp((center.getY() - inner.getMinY() - vh / 2.0) / (inner.getHeight() - vh)));
	}

	private static Bounds computeVisibleContentBounds(Node root) {
		Bounds result = null;
		if (root.isVisible() && root instanceof javafx.scene.Parent parent) {
			for (var child : parent.getChildrenUnmodifiable()) {
				if (!child.isVisible())
					continue;
				var b = child.getBoundsInParent();
				if (b.getWidth() <= 0 || b.getHeight() <= 0)
					continue;
				result = (result == null) ? b : union(result, b);
			}
		}
		return result;
	}

	private static Bounds union(Bounds a, Bounds b) {
		var minX = Math.min(a.getMinX(), b.getMinX());
		var minY = Math.min(a.getMinY(), b.getMinY());
		var maxX = Math.max(a.getMaxX(), b.getMaxX());
		var maxY = Math.max(a.getMaxY(), b.getMaxY());
		return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
	}

	public static double clamp(double v) {
		return Math.max(0.0, Math.min(1.0, v));
	}
}
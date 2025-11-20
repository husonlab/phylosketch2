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
import phylosketch.window.MainWindow;

public class ZoomToFit {
	public static void apply(MainWindow window) {
		var scrollPane = window.getController().getScrollPane();
		var world = window.getDrawView().getWorld();

		{
			var worldLocalBounds = world.getLayoutBounds();
			var cw = worldLocalBounds.getWidth();
			var ch = worldLocalBounds.getHeight();
			if (cw <= 0 || ch <= 0) {
				System.err.println("zoomToFit: content has zero size: " + worldLocalBounds);
				return;
			}

			var viewportBounds = scrollPane.getViewportBounds();
			var vw = viewportBounds.getWidth();
			var vh = viewportBounds.getHeight();

			if (vw <= 0 || vh <= 0) {
				System.err.println("zoomToFit: viewport has zero size: " + viewportBounds);
				return;
			}

			var scaleX = (vw / cw);
			var scaleY = (vh / ch);

			scaleX = scaleY = Math.min(scaleX, scaleY);
			scrollPane.zoomBy(scaleX / scrollPane.getZoomX(), scaleY / scrollPane.getZoomY());
		}

		Platform.runLater(() -> {
			var worldLocalBounds = world.getLayoutBounds();
			var centerWorldScreenCoordinates = world.localToScreen(worldLocalBounds.getCenterX(), worldLocalBounds.getCenterY());

			var content = scrollPane.getContent();
			var centerInContent = content.screenToLocal(centerWorldScreenCoordinates);
			var contentBounds = content.getLayoutBounds();
			var contentWidth = contentBounds.getWidth();
			var contentHeight = contentBounds.getHeight();

			var viewportBounds = scrollPane.getViewportBounds();
			var viewportWidth = viewportBounds.getWidth();
			var viewportHeight = viewportBounds.getHeight();

			if (contentWidth <= viewportWidth || contentHeight <= viewportHeight) {
				return; // nothing to scroll or weird case
			}

			// 4. Compute target hvalue / vvalue so that centerInContent is in the middle
			var hx = (centerInContent.getX() - viewportWidth / 2.0) / (contentWidth - viewportWidth);
			var vy = (centerInContent.getY() - viewportHeight / 2.0) / (contentHeight - viewportHeight);

			scrollPane.setHvalue(clamp(hx));
			scrollPane.setVvalue(clamp(vy));
		});
	}

	private static double clamp(double v) {
		return (v < 0) ? 0 : (v > 1) ? 1 : v;
	}
}
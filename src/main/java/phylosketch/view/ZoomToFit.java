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

import phylosketch.window.MainWindow;

public class ZoomToFit {
	public static void apply(MainWindow window) {
		var scrollPane = window.getController().getScrollPane();
		var world = window.getDrawView().getWorld();

		var contentBounds = world.getLayoutBounds();
		var cw = contentBounds.getWidth();
		var ch = contentBounds.getHeight();
		if (cw <= 0 || ch <= 0) {
			System.err.println("zoomToFit: content has zero size: " + contentBounds);
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

		scrollPane.setHvalue(scrollPane.getHmin());
		scrollPane.setVvalue(scrollPane.getVmin());
	}
}
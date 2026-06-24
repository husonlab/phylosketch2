/*
 * ScrollPaneUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import jloda.fx.control.ZoomableScrollPane;

import static phylosketch.view.ZoomToFit.clamp;

/**
 * scroll pane utils
 * Daniel Huson, 2.2025
 */
public class ScrollPaneUtils {
	/**
	 * use this method to remove items from content pane that is contained in a scroll pane
	 * without changing the scroll position
	 *
	 * @param contentPane the content pane
	 * @param runRemove   the code that removes items from the content pane
	 */
	public static void runRemoveAndKeepScrollPositions(Node contentPane, Runnable runRemove) {
		if (false) // only remove
			runRemove.run();
		else {
			var scrollPane = findScrollPane(contentPane);
			if (scrollPane == null) {
				runRemove.run();
				return;
			}
			var group = (scrollPane instanceof ZoomableScrollPane z) ? z.getContentGroup() : scrollPane.getContent();
			var inner = group.getBoundsInLocal();
			var vp = scrollPane.getViewportBounds();
			// content point currently shown at the viewport's top-getLeft, captured before removal
			var anchorX = inner.getMinX() + scrollPane.getHvalue() * Math.max(0, inner.getWidth() - vp.getWidth());
			var anchorY = inner.getMinY() + scrollPane.getVvalue() * Math.max(0, inner.getHeight() - vp.getHeight());

			runRemove.run();

			Platform.runLater(() -> {
				scrollPane.layout();
				var b = group.getBoundsInLocal();
				var v = scrollPane.getViewportBounds();
				if (b.getWidth() > v.getWidth())
					scrollPane.setHvalue(clamp((anchorX - b.getMinX()) / (b.getWidth() - v.getWidth())));
				if (b.getHeight() > v.getHeight())
					scrollPane.setVvalue(clamp((anchorY - b.getMinY()) / (b.getHeight() - v.getHeight())));
			});
		}
	}

	private static ScrollPane findScrollPane(Node contentPane) {
		var parent = (Parent) contentPane;
		while (parent != null) {
			if (parent instanceof ScrollPane sp) {
				break;
			} else
				parent = parent.getParent();
		}
		if (parent instanceof ScrollPane scrollPane)
			return scrollPane;
		else return null;
	}
}

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
import jloda.fx.util.RunAfterAWhile;

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
		var parent = (Parent) contentPane;
		while (parent != null) {
			if (parent instanceof ScrollPane sp) {
				break;
			} else
				parent = parent.getParent();
		}
		if (parent instanceof ScrollPane scrollPane) {
			var hValue = scrollPane.getHvalue();
			var vValue = scrollPane.getVvalue();

			runRemove.run();

			Platform.runLater(() -> {
				scrollPane.setHvalue(hValue);
				scrollPane.setVvalue(vValue);
			});
			Platform.runLater(() -> {
				scrollPane.setHvalue(hValue);
				scrollPane.setVvalue(vValue);
			});
			RunAfterAWhile.applyInFXThread(runRemove, () -> {
				scrollPane.setHvalue(hValue);
				scrollPane.setVvalue(vValue);
			});
		} else {
			runRemove.run();
		}
	}
}

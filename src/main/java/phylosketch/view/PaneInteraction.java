/*
 * PaneInteraction.java Copyright (C) 2024 Daniel H. Huson
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

import phylosketch.main.PhyloSketch;

/**
 * setup view mouse
 */
public class PaneInteraction {
	public static void setup(DrawPane view) {
		view.setOnMouseClicked(me -> {
			if ((me.getClickCount() == 2 || !PhyloSketch.isDesktop() && me.getClickCount() == 1) && me.isStillSincePress()) {
				if (view.getNodeSelection().size() > 0 || view.getEdgeSelection().size() > 0) {
					view.getNodeSelection().clearSelection();
					view.getEdgeSelection().clearSelection();
				}
				me.consume();
			}
		});
	}
}

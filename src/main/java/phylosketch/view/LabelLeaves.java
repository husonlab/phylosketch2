/*
 * LabelLeaves.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Stage;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * interactively label all leaves
 * Daniel Huson, 1.2020
 */
public class LabelLeaves {
	public static void labelLeaves(Stage owner, DrawView drawView) {

		var canceled = new SimpleBooleanProperty(false);

		for (var component : ConnectedComponents.components(drawView.getGraph())) {
			var nodes = new ArrayList<>(component.stream().filter(Node::isLeaf)
					.filter(v -> drawView.getNodeSelection().size() == 0 || drawView.getNodeSelection().isSelected(v)).toList());

			if (!nodes.isEmpty()) {
				var rootPosition = RootPosition.compute(component);
				var leaves = new ArrayList<Node>();
				TraversalByLayout.apply(component.stream().filter(v -> v.getInDegree() == 0).toList(), nodes, rootPosition, leaves::add);

				relabelRec(drawView, leaves, canceled);
				if (canceled.get()) {
					return;
				}
			}
        }
    }

	private static void relabelRec(DrawView drawView, List<Node> leaves, BooleanProperty canceled) {
        if (!leaves.isEmpty()) {
            var v = leaves.remove(0);
			var shape = DrawView.getShape(v);
            var bounds = shape.getBoundsInLocal();
            var location = shape.localToScreen(bounds.getMinX(), bounds.getMinY());
			NodeLabelEditBox.show(drawView, location.getX(), location.getY(), v, canceled, () -> relabelRec(drawView, leaves, canceled));
		}
    }
}

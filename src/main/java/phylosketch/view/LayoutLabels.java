/*
 *  LayoutLabels.java Copyright (C) 2024 Daniel H. Huson
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
 */

package phylosketch.view;

import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.util.GeometryUtilsFX;

import java.util.Collection;

/**
 * layout labels
 * Daniel Huson, 11.2025
 */
public class LayoutLabels {

	public static void apply(DrawView view, jloda.graph.Node root, Collection<jloda.graph.Node> nodes, LayoutRootedPhylogeny.Layout layout, double labelGap) {
		if (layout == LayoutRootedPhylogeny.Layout.Circular) {
			var rootLocation = view.getLocation(root);
			for (var v : nodes) {
				if (v.isLeaf()) {
					var location = view.getLocation(v);
					var label = DrawView.getLabel(v);
					if (label != null) {
						var angle = GeometryUtilsFX.computeAngle(location.getX() - rootLocation.getX(), location.getY() - rootLocation.getY());
						var shift = 0.5 * label.getWidth() + labelGap;
						label.setLayoutX(-0.5 * label.getWidth());
						label.setLayoutY(-0.5 * label.getHeight());
						label.setRotate(angle);
						label.ensureUpright();

						var move = GeometryUtilsFX.translateByAngle(label.getLayoutX(), label.getLayoutY(), angle, shift);
						label.setLayoutX(move.getX());
						label.setLayoutY(move.getY());
					}
				}
			}
		} else if (layout == LayoutRootedPhylogeny.Layout.Rectangular) {
			for (var v : nodes) {
				if (v.isLeaf()) {
					var label = DrawView.getLabel(v);
					if (label != null) {
						label.setRotate(0.0);
						label.setLayoutX(10);
						label.setLayoutY(-0.5 * label.getHeight());
						label.ensureUpright();
					}
				}
			}
		} else if (layout == LayoutRootedPhylogeny.Layout.Radial) {
			var rootLocation = view.getLocation(root);
			for (var v : nodes) {
				if (v.isLeaf()) {
					var location = view.getLocation(v);
					var otherLocation = view.getLocation(v.getFirstInEdge().getOpposite(v));
					if (otherLocation.distance(location) <= 1) {
						otherLocation = rootLocation;
					}
					var label = DrawView.getLabel(v);
					if (label != null) {
						var angle = GeometryUtilsFX.computeAngle(location.getX() - otherLocation.getX(), location.getY() - otherLocation.getY());
						var shift = 0.5 * label.getWidth() + labelGap;
						label.setLayoutX(-0.5 * label.getWidth());
						label.setLayoutY(-0.5 * label.getHeight());
						//label.setRotate(angle);
						label.ensureUpright();

						var move = GeometryUtilsFX.translateByAngle(label.getLayoutX(), label.getLayoutY(), angle, shift);
						label.setLayoutX(move.getX());
						label.setLayoutY(move.getY());
					}
				}
			}
		}
		view.setHorizontalLabels(layout == LayoutRootedPhylogeny.Layout.Radial);
	}
}

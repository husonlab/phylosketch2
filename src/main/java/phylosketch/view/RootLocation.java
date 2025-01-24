/*
 *  Copyright (C) 2018. Daniel H. Huson
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

import javafx.geometry.Point2D;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;

import java.util.Collection;

public enum RootLocation {
	Top, Bottom, Left, Right, Center;

	public boolean isHorizontal() {
		return this == Left || this == Right;
	}

	public static RootLocation compute(Collection<Node> nodes) {
		var averageRoot = new Point2D(0, 0);
		var roots = 0;
		var averageNode = new Point2D(0, 0);
		var count = 0;

		for (Node w : nodes) {
			final double x = DrawPane.getX(w);
			final double y = DrawPane.getY(w);
			if (w.getInDegree() == 0) {
				averageRoot = new Point2D(averageRoot.getX() + x, averageRoot.getY() + y);
				roots++;
			}
			averageNode = new Point2D(averageNode.getX() + x, averageNode.getY() + y);
			count++;
		}
		if (roots > 0)
			averageRoot = new Point2D(averageRoot.getX() / roots, averageRoot.getY() / roots);
		if (count > 0)
			averageNode = new Point2D(averageNode.getX() / count, averageNode.getY() / count);

		final double angle = GeometryUtilsFX.computeAngle(averageRoot.subtract(averageNode));
		if (angle >= 45 && angle <= 135)
			return RootLocation.Bottom;
		else if (angle >= 135 && angle <= 225)
			return RootLocation.Left;
		else if (angle >= 225 && angle <= 315)
			return RootLocation.Top;
		else
			return RootLocation.Right;
	}

	}

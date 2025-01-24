/*
 * SetupRootTool.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.capture;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import phylocap.window.CaptureViewController;

import java.util.function.Consumer;

/**
 * setup root selection tool
 * Daniel Huson, 1.2025
 */
public class SetupRootTool {

	public static void apply(CaptureViewController controller, Consumer<Point2D> setRootLocation, Group group) {
		var rootCircle = new Circle(50, 50, 10);
		rootCircle.setFill(Color.TRANSPARENT);
		rootCircle.setStroke(Color.DARKORANGE);
		var rootLabel = new Text("Root location");
		rootLabel.layoutXProperty().bind(rootCircle.centerXProperty().add(rootCircle.radiusProperty()).add(5));
		rootLabel.layoutYProperty().bind(rootCircle.centerYProperty().add(rootCircle.radiusProperty()).add(5));
		rootLabel.setStroke(Color.DARKORANGE.deriveColor(1.0, 1.0, 1.0, 0.5));
		rootLabel.setFill(rootLabel.getStroke());
		DrawUtils.setupDragCircle(rootCircle, setRootLocation);
		rootCircle.centerXProperty().addListener(e -> setRootLocation.accept(new Point2D(rootCircle.getCenterX(), rootCircle.getCenterY())));
		rootCircle.centerYProperty().addListener(e -> setRootLocation.accept(new Point2D(rootCircle.getCenterX(), rootCircle.getCenterY())));
		group.getChildren().addAll(rootCircle, rootLabel);
		group.visibleProperty().bindBidirectional(controller.getSetRootLocationToggleButton().selectedProperty());
		group.visibleProperty().addListener(((v, o, n) -> {
			if (n)
				setRootLocation.accept(new Point2D(rootCircle.getCenterX(), rootCircle.getCenterY()));
			else setRootLocation.accept(null);
		}));
	}
}

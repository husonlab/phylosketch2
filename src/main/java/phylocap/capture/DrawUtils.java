/*
 * DrawUtils.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import jloda.fx.selection.SelectionModel;
import net.sourceforge.tess4j.Word;
import phylosketch.main.PhyloSketch;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DrawUtils {
	public static void createCircles(List<Point> points, Group group) {
		group.getChildren().setAll(points.stream().map(DrawUtils::createCircle).toList());
	}

	public static Shape createCircle(Point point) {
		var shape = new Circle(2);
		shape.setFill(Color.DEEPPINK.deriveColor(1.0, 1.0, 1.0, 0.5));
		shape.setStroke(Color.DEEPPINK.deriveColor(1.0, 1.0, 1.0, 0.5));
		shape.setLayoutX(point.x());
		shape.setLayoutY(point.y());
		return shape;
	}

	public static void createPaths(List<Segment> segments, SelectionModel<Segment> selection, Supplier<Boolean> canSelect, Group group) {
		group.getChildren().clear();
		for (var segment : segments) {
			var path = createPath(segment);
			path.setOnMouseClicked(e -> {
				if (canSelect.get()) {
					if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
						selection.clearSelection();
					}
					selection.toggleSelection(segment);
					e.consume();
				}
			});
			group.getChildren().add(path);
		}
	}

	public static Path createPath(Segment segment) {
		var points = segment.points();
		var path = new Path();
		for (var i = 0; i < points.size(); i++) {
			if (i == 0)
				path.getElements().add(new MoveTo(points.get(i).x(), points.get(i).y()));
			else
				path.getElements().add(new LineTo(points.get(i).x(), points.get(i).y()));
		}
		path.setStroke(new Color(Math.random(), Math.random(), 1, 0.5).darker());
		path.setStrokeWidth(7);
		path.setFill(Color.TRANSPARENT);
		path.setUserData(segment);
		return path;
	}

	public static void createWordShapes(List<Word> wordList, SelectionModel<Word> selection, Supplier<Boolean> canSelect, Group group) {
		group.getChildren().clear();
		for (var word : wordList) {
			var awtRect = word.getBoundingBox();
			System.err.printf("Text: '%s', Location: [x=%d, y=%d, width=%d, height=%d]\n",
					word.getText(), awtRect.x, awtRect.y, awtRect.width, awtRect.height);
			var rectangle = new Rectangle(awtRect.x, awtRect.y, awtRect.width, awtRect.height);
			rectangle.setFill(Color.WHITE);
			rectangle.setStroke(Color.GREEN);
			rectangle.setUserData(word);

			var text = new Text(word.getText());
			text.setStroke(Color.GREEN);
			text.setLayoutX(awtRect.x + 1);
			text.setLayoutY(awtRect.y + 10);
			text.setUserData(word);

			group.getChildren().addAll(rectangle, text);

			rectangle.setOnMouseClicked(e -> {
				if (canSelect.get()) {
					if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
						selection.clearSelection();
					}
					selection.toggleSelection(word);
					e.consume();
				}
			});
			text.setOnMouseClicked(rectangle.getOnMouseClicked());
		}
	}

	private static double mouseX;
	private static double mouseY;

	public static void setupDragCircle(Circle circle, Consumer<Point2D> runOnRelease) {
		circle.setOnMousePressed(e -> {
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
		});
		circle.setOnMouseDragged(e -> {
			circle.setCenterX(circle.getCenterX() + e.getScreenX() - mouseX);
			circle.setCenterY(circle.getCenterY() + e.getScreenY() - mouseY);
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
		});

		if (runOnRelease != null) {
			circle.setOnMouseDragReleased(e -> {
				if (!e.isStillSincePress()) {
					runOnRelease.accept(new Point2D(circle.getCenterX(), circle.getCenterY()));
				}
			});
		}
	}
}

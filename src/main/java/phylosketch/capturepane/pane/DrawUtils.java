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

package phylosketch.capturepane.pane;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import jloda.fx.selection.SelectionModel;
import jloda.fx.util.ClipboardUtils;
import phylosketch.capturepane.capture.Point;
import phylosketch.capturepane.capture.Segment;
import phylosketch.capturepane.capture.Word;
import phylosketch.main.PhyloSketch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * utilities for drawing stuff
 * Daniel Huson, 1.2025
 */
public class DrawUtils {

	public static List<Node> createCircles(List<Point> points) {
		var list = new ArrayList<Node>(points.stream().map(DrawUtils::createCircle).toList());
		if (false)
			list.addAll(points.stream().map(
					p -> {
						var text = new Text(p.x() + "," + p.y());
						text.setLayoutX(p.x() + 5 + (50 * Math.random() - 25));
						text.setLayoutY(p.y() - 5 + (50 * Math.random() - 25));
						text.setStroke(Color.DARKRED);
						return text;
					}
			).toList());
		return list;
	}

	public static Shape createCircle(Point point) {
		var shape = new Circle(2);
		shape.setFill(Color.DEEPPINK.deriveColor(1.0, 1.0, 1.0, 0.5));
		shape.setStroke(Color.DEEPPINK.deriveColor(1.0, 1.0, 1.0, 0.5));
		shape.setLayoutX(point.x());
		shape.setLayoutY(point.y());
		return shape;
	}

	public static List<Node> createPaths(List<Segment> segments, SelectionModel<Segment> selection, Supplier<Boolean> canSelect) {
		var list = new ArrayList<Node>();
		for (var segment : segments) {
			var path = createPath(segment);
			path.setTranslateX(30 * Math.random() - 15);
			path.setTranslateY(30 * Math.random() - 15);

			path.setOnMouseClicked(e -> {
				if (canSelect.get()) {
					if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
						selection.clearSelection();
					}
					selection.toggleSelection(segment);
					e.consume();
				}
			});
			list.add(path);
		}
		return list;
	}

	public static Path createPath(Segment segment) {
		var points = segment.points();
		var path = new Path();
		//path.getStyleClass().add("graph-edge");
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
			var awtRect = word.boundingBox();
			var rectangle = new Rectangle(awtRect.x, awtRect.y, awtRect.width, awtRect.height);
			rectangle.setStyle("-fx-fill: rgba(240, 255, 240, 0.3);-fx-stroke: darkgreen;-fx-stroke-width: 0.5;");
			rectangle.setUserData(word);

			var label = new Label(word.text());
			label.setFont(new Font(label.getFont().getFamily(), 14));
			label.setStyle("-fx-text-fill: darkgreen;");
			label.setLayoutX(awtRect.x + 1);
			label.setLayoutY(awtRect.y + 1);
			label.setUserData(word);

			group.getChildren().addAll(rectangle, label);

			rectangle.setOnMouseClicked(e -> {
				if (canSelect.get()) {
					if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
						selection.clearSelection();
					}
					selection.toggleSelection(word);
					e.consume();
				}
			});
			label.setOnMouseClicked(rectangle.getOnMouseClicked());

			rectangle.setOnContextMenuRequested(me -> {
				var copyItem = new MenuItem("Copy");
				copyItem.setOnAction(e -> ClipboardUtils.putString(word.text()));
				var menu = new ContextMenu();
				menu.getItems().add(copyItem);
				menu.show(rectangle, me.getScreenX(), me.getScreenY());
			});
		}
	}

}

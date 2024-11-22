/*
 * BackgroundImagePane.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.undo.UndoManager;
import jloda.util.ProgramProperties;

/**
 * maintain an image in the background
 */
public class BackgroundImagePane extends HBox {
	private static double mouseDownX;
	private static double mouseDownY;
	private static double mouseX;
	private static double mouseY;

	private final ObservableIntegerArray defaultGeometry;
	private final BooleanProperty show = new SimpleBooleanProperty(this, "show", false);

	private final ImageView imageView;

	public BackgroundImagePane(UndoManager undoManager) {
		defaultGeometry = FXCollections.observableIntegerArray(ProgramProperties.get("BackgroundImageGeometry", new int[]{20, 20, 400, 400}));
		defaultGeometry.addListener(e -> ProgramProperties.put("BackgroundImageGeometry", defaultGeometry.toArray(new int[0])));

		setTranslateX(defaultGeometry.get(0));
		setTranslateY(defaultGeometry.get(1));

		imageView = new ImageView();
		imageView.setOpacity(0.5);
		imageView.imageProperty().addListener((v, o, n) -> {
			if (n != null) {
				imageView.setFitWidth(defaultGeometry.get(2));
				imageView.setFitHeight(defaultGeometry.get(3));
			}
			undoManager.add("background image", imageView.imageProperty(), o, n);
		});

		show.addListener((v, o, n) -> undoManager.add("background image", show, o, n));

		getStyleClass().add("viewer-background");
		setStyle("-fx-border-color: gray; ");

		setOnMousePressed(e -> {
			mouseX = mouseDownX = e.getScreenX();
			mouseY = mouseDownY = e.getScreenY();
			e.consume();
		});

		setOnMouseDragged(e -> {
			var dx = e.getScreenX() - mouseX;
			var dy = e.getScreenY() - mouseY;

			setTranslateX(getTranslateX() + dx);
			setTranslateY(getTranslateY() + dy);
			defaultGeometry.set(0, (int) getTranslateX());
			defaultGeometry.set(1, (int) getTranslateY());
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();

			e.consume();
		});

		setOnMouseReleased(e -> {
			if (mouseX != mouseDownX || mouseY != mouseDownY) {
				var oldX = getTranslateX() - (mouseX - mouseDownX);
				var oldY = getTranslateY() - (mouseY - mouseDownY);
				var newX = getTranslateX();
				var newY = getTranslateY();
				undoManager.add("move image", () -> {
					setTranslateX(oldX);
					setTranslateY(oldY);
				}, () -> {
					setTranslateX(newX);
					setTranslateY(newY);
				});
			}
			e.consume();
		});

		var closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(a -> show.set(false));
		final var resizeHandle = MaterialIcons.graphic(MaterialIcons.open_in_full, "-fx-rotate: 90;");
		resizeHandle.setOnMousePressed(e -> {
			mouseX = mouseDownX = e.getScreenX();
			mouseY = mouseDownY = e.getScreenY();
			e.consume();
		});
		resizeHandle.setOnMouseDragged(e -> {
			var dx = e.getScreenX() - mouseX;
			var dy = e.getScreenY() - mouseY;
			if (dx > 0 || imageView.getFitWidth() + dx >= 100) {
				imageView.setFitWidth(imageView.getFitWidth() + dx);
				defaultGeometry.set(2, (int) imageView.getFitWidth());
			}
			if (dy > 0 || imageView.getFitHeight() + dx >= 100) {
				imageView.setFitHeight(imageView.getFitHeight() + dy);
				defaultGeometry.set(3, (int) imageView.getFitHeight());
			}
			mouseX = e.getScreenX();
			mouseY = e.getScreenY();
			e.consume();
		});

		resizeHandle.setOnMouseReleased(e -> {
			if (mouseX != mouseDownX || mouseY != mouseDownY) {
				var oldX = imageView.getFitWidth() - (mouseX - mouseDownX);
				var oldY = imageView.getFitHeight() - (mouseY - mouseDownY);
				var newX = imageView.getFitWidth();
				var newY = imageView.getFitHeight();
				undoManager.add("move image", () -> {
					imageView.setFitWidth(oldX);
					imageView.setFitHeight(oldY);
				}, () -> {
					imageView.setFitWidth(newX);
					imageView.setFitHeight(newY);
				});
			}
			e.consume();
		});

		var rightBorderPane = new BorderPane();
		rightBorderPane.setTop(closeButton);
		rightBorderPane.setBottom(resizeHandle);
		getChildren().addAll(imageView, rightBorderPane);
	}

	public ImageView getImageView() {
		return imageView;
	}

	public boolean isShow() {
		return show.get();
	}

	public BooleanProperty showProperty() {
		return show;
	}

	public void setShow(boolean show) {
		showProperty().set(show);
	}


}

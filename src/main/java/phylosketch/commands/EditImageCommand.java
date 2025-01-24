/*
 * DeleteInImageCommand.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.commands;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import jloda.fx.undo.UndoableRedoableCommand;

public class EditImageCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public EditImageCommand(String name, ImageView imageView, Bounds sourceRectangle, Bounds targetRectangle) {
		super(name);

		var oldImage = imageView.getImage();
		undo = () -> imageView.setImage(oldImage);

		var tmpImage = getImageInRect(imageView.getImage(), sourceRectangle);
		var newImage = deleteAndPlace(imageView.getImage(), sourceRectangle, targetRectangle, tmpImage);
		redo = () -> imageView.setImage(newImage);
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}

	/**
	 * gets a new image that contains the same pixels as the original image in the given rect
	 *
	 * @param image image
	 * @param rect  the rectangle to get in image coordinates
	 * @return image contained in rectangle
	 */
	public static Image getImageInRect(Image image, Bounds rect) {
		var x = (int) Math.round(rect.getMinX());
		var y = (int) Math.round(rect.getMinY());
		var width = (int) Math.round(rect.getWidth());
		var height = (int) Math.round(rect.getHeight());

		var pixelReader = image.getPixelReader();
		var writableImage = new WritableImage(width, height);
		var pixelWriter = writableImage.getPixelWriter();

		for (var i = 0; i < width; i++) {
			if (x + i >= 0 && x + i < (int) image.getWidth()) {
				for (var j = 0; j < height; j++) {
					if (y + j >= 0 && y + j < (int) image.getHeight()) {
						pixelWriter.setColor(i, j, pixelReader.getColor(x + i, y + j));
					}
				}
			}
		}
		return writableImage;
	}

	/**
	 * get a new image
	 *
	 * @param image           the original image
	 * @param srcRectangle    if non-null, this rectangle will get set to white
	 * @param targetRectangle if non-null, this is the target rectangle for the given insert image
	 * @param insertImage     the image to insert
	 * @return the new image
	 */
	public static Image deleteAndPlace(Image image, Bounds srcRectangle, Bounds targetRectangle, Image insertImage) {
		var imageWidth = (int) image.getWidth();
		var imageHeight = (int) image.getHeight();

		var pixelReader = insertImage.getPixelReader();

		var writableImage = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
		var pixelWriter = writableImage.getPixelWriter();

		if (srcRectangle != null) {
			var x = (int) Math.round(srcRectangle.getMinX());
			var y = (int) Math.round(srcRectangle.getMinY());
			var width = (int) Math.round(srcRectangle.getWidth());
			var height = (int) Math.round(srcRectangle.getHeight());
			for (var i = 0; i < width; i++) {
				if (x + i >= 0 && x + i < imageWidth) {
					for (var j = 0; j < height; j++) {
						if (y + j >= 0 && y + j < imageHeight) {
							pixelWriter.setColor(x + i, y + j, Color.WHITE);
						}
					}
				}
			}
		}

		if (targetRectangle != null) {
			var x = (int) Math.round(targetRectangle.getMinX());
			var y = (int) Math.round(targetRectangle.getMinY());
			var width = (int) Math.round(targetRectangle.getWidth());
			var height = (int) Math.round(targetRectangle.getHeight());
			for (var i = 0; i < width; i++) {
				if (x + i < imageWidth) {
					for (var j = 0; j < height; j++) {
						if (y + j < imageHeight) {
							pixelWriter.setColor(x + i, y + j, pixelReader.getColor(i, j));
						}
					}
				}
			}
		}
		return writableImage;
	}

	public static Bounds screenToImage(Bounds screenRectangle, ImageView imageView) {
		var boundsInImageView = imageView.screenToLocal(screenRectangle);

		var image = imageView.getImage();
		double imageWidth = image.getWidth();
		double imageHeight = image.getHeight();

		double scaleX = imageWidth / imageView.getBoundsInLocal().getWidth();
		double scaleY = imageHeight / imageView.getBoundsInLocal().getHeight();

		double imageX = boundsInImageView.getMinX() * scaleX;
		double imageY = boundsInImageView.getMinY() * scaleY;
		double imageWidthRect = boundsInImageView.getWidth() * scaleX;
		double imageHeightRect = boundsInImageView.getHeight() * scaleY;

		return new BoundingBox(imageX, imageY, imageWidthRect, imageHeightRect);
	}
}

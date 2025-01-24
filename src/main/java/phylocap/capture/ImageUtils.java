/*
 * ImageUtils.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.List;

public class ImageUtils {
	public static int[][] convertToBinaryArray(Image image, double brightnessThreshold) {
		var reader = image.getPixelReader();
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var binaryImage = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (reader.getColor(x, y).getBrightness() <= brightnessThreshold)
					binaryImage[x][y] = 1;
			}
		}

		return binaryImage;
	}

	public static Image convertToImage(int[][] binaryImage) {
		int width = binaryImage.length;
		int height = binaryImage[0].length;

		var writableImage = new WritableImage(width, height);
		var pixelWriter = writableImage.getPixelWriter();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				// Map 0 to BLACK and 1 to WHITE
				var color = (binaryImage[x][y] != 0) ? Color.BLACK : Color.WHITE;
				pixelWriter.setColor(x, y, color);
			}
		}
		return writableImage;
	}

	public static Image replaceRectanglesWithWhite(Image image, List<java.awt.Rectangle> awtRectangles, int margin) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();

		var writableImage = new WritableImage(image.getPixelReader(), width, height);
		var pixelWriter = writableImage.getPixelWriter();

		// Process each rectangle
		for (var rect : awtRectangles) {
			var xStart = Math.max(0, rect.x - margin);
			var yStart = Math.max(0, rect.y - margin);
			var xEnd = Math.min(width, rect.x + rect.width + margin);
			var yEnd = Math.min(height, rect.y + rect.height + margin);

			// Fill the rectangle with white
			for (var y = yStart; y < yEnd; y++) {
				for (var x = xStart; x < xEnd; x++) {
					pixelWriter.setColor(x, y, Color.WHITE);
				}
			}
		}
		return writableImage; // Return the modified image
	}


	public static Image convertToGrayScale(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();

		// Convert to grayscale
		var grayImage = new WritableImage(width, height);
		var reader = image.getPixelReader();
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var color = reader.getColor(x, y);
				if (color.getOpacity() > 0) {
					var gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3; // Average grayscale
					grayImage.getPixelWriter().setColor(x, y, new Color(gray, gray, gray, color.getOpacity()));
				} else {
					grayImage.getPixelWriter().setColor(x, y, Color.WHITE);
				}
			}
		}
		return grayImage;
	}

	public static BufferedImage convertToBufferedImage(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();

		var bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		PixelReader pixelReader = image.getPixelReader();

		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				Color color = pixelReader.getColor(x, y);
				var gray = (int) (255 * ((color.getRed() + color.getGreen() + color.getBlue()) / 3));
				var rgb = (gray << 16) | (gray << 8) | gray;
				bufferedImage.setRGB(x, y, rgb);
			}
		}
		return bufferedImage;
	}

	public static Image replaceTransparentBackground(Image image) {
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();

		// Create a writable image
		var writableImage = new WritableImage(width, height);
		var pixelReader = image.getPixelReader();
		var pixelWriter = writableImage.getPixelWriter();

		for (var x = 0; x < width; x++) {
			for (var y = 0; y < height; y++) {
				// Read the color at each pixel
				var color = pixelReader.getColor(x, y);

				// Check if the pixel is transparent (alpha < 1.0)
				if (color.getOpacity() == 0) {
					// Replace it with white
					pixelWriter.setColor(x, y, Color.WHITE);
				} else {
					// Keep the original color
					pixelWriter.setColor(x, y, color);
				}
			}
		}
		return writableImage;
	}

	public static boolean tooMuchBlack(int[][] binaryMatrix, double maxProportion) {
		var width = binaryMatrix.length;
		var height = binaryMatrix[0].length;
		var max = maxProportion * width * height;
		var blackPixels = 0;
		for (int[] row : binaryMatrix) {
			for (var value : row) {
				if (value != 0) {
					blackPixels++;
					if (blackPixels >= max)
						return true;
				}
			}
		}
		return false;
	}
}

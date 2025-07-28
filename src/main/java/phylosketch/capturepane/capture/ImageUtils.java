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

package phylosketch.capturepane.capture;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;


/**
 * utilities for working with images
 * Daniel Huson, 12.2024
 */
public class ImageUtils {


	/**
	 * Converts an Image into a binary matrix optimized for line extraction.
	 */
	public static int[][] convertToBinaryArray(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();
		var binaryMatrix = new int[height][width];

		var pixelReader = image.getPixelReader();

		// Adaptive thresholding parameters
		var thresholdFactor = 1.2; // Adjust to make more lines visible

		// Step 1: Convert to grayscale with enhanced contrast
		var pixelValues = new int[width * height];
		var index = 0;
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var argb = pixelReader.getArgb(x, y);
				var r = (argb >> 16) & 0xFF;
				var g = (argb >> 8) & 0xFF;
				var b = argb & 0xFF;

				// Convert to grayscale but give more weight to dark features
				var gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
				pixelValues[index++] = gray;
			}
		}

		// Compute the median brightness as an adaptive threshold
		var threshold = computeAdaptiveThreshold(pixelValues, thresholdFactor);

		// Step 2: Apply thresholding
		for (var y = 0; y < height; y++) {
			for (var x = 0; x < width; x++) {
				var argb = pixelReader.getArgb(x, y);
				int r = (argb >> 16) & 0xFF;
				int g = (argb >> 8) & 0xFF;
				int b = argb & 0xFF;

				var gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
				binaryMatrix[y][x] = (gray > threshold) ? 0 : 1; // Black (1) or White (0)
			}
		}

		return binaryMatrix;
	}

	/**
	 * Computes an adaptive threshold based on image contrast.
	 */
	private static int computeAdaptiveThreshold(int[] pixelValues, double thresholdFactor) {
		java.util.Arrays.sort(pixelValues);

		var medianIndex = pixelValues.length / 2;
		var median = pixelValues[medianIndex];
		var threshold = (int) (median * thresholdFactor);
		return Math.max(50, Math.min(threshold, 200));
	}

	public static Image convertToImage(int[][] binaryImage, Color fgColor) {
		int height = binaryImage.length;
		int width = binaryImage[0].length;

		var writableImage = new WritableImage(width, height);
		var pixelWriter = writableImage.getPixelWriter();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					var color = (binaryImage[y][x] != 0) ? fgColor : Color.TRANSPARENT;
				pixelWriter.setColor(x, y, color);
			}
		}
		return writableImage;
	}

	public static Image convertToGrayScale(Image image) {
		var width = (int) image.getWidth();
		var height = (int) image.getHeight();

		// Convert to grayscale
		var grayImage = new WritableImage(width, height);
		var reader = image.getPixelReader();
			for (var x = 0; x < width; x++) {
				for (var y = 0; y < height; y++) {
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
		var height = binaryMatrix.length;
		var width = binaryMatrix[0].length;
		var max = maxProportion * width * height;
		var blackPixels = 0;
		for (var row : binaryMatrix) {
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

	public static Image cropImage(Image input, int x, int y, int width, int height) {
		var reader = input.getPixelReader();
		if (reader == null) {
			throw new IllegalArgumentException("Image has no pixel reader.");
		}

		// Clamp to avoid IndexOutOfBounds
		var croppedWidth = Math.min(width, (int) input.getWidth() - x);
		var croppedHeight = Math.min(height, (int) input.getHeight() - y);

		return new WritableImage(reader, x, y, croppedWidth, croppedHeight);
	}
}

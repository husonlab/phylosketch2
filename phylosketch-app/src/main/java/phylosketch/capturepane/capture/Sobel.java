/*
 * Sobel.java Copyright (C) 2025 Daniel H. Huson
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


import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

@Deprecated // not used
public class Sobel {
	public static Image applySobelFilter(Image inputImage) {
		int width = (int) inputImage.getWidth();
		int height = (int) inputImage.getHeight();
		WritableImage outputImage = new WritableImage(width, height);
		PixelReader pixelReader = inputImage.getPixelReader();
		PixelWriter pixelWriter = outputImage.getPixelWriter();

		int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
		int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

		for (int x = 1; x < width - 1; x++) {
			for (int y = 1; y < height - 1; y++) {
				int gx = 0, gy = 0;

				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						int pixel = pixelReader.getColor(x + i, y + j).grayscale().getRed() > 0.5 ? 255 : 0;
						gx += pixel * sobelX[i + 1][j + 1];
						gy += pixel * sobelY[i + 1][j + 1];
					}
				}

				int magnitude = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
				pixelWriter.setColor(x, y, javafx.scene.paint.Color.gray(magnitude / 255.0));
			}
		}

		return outputImage;
	}

	public static Image increaseContrast(Image inputImage, double factor) {
		int width = (int) inputImage.getWidth();
		int height = (int) inputImage.getHeight();
		WritableImage outputImage = new WritableImage(width, height);
		PixelReader reader = inputImage.getPixelReader();
		PixelWriter writer = outputImage.getPixelWriter();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Color color = reader.getColor(x, y);
				double newGray = Math.pow(color.getRed(), factor);  // Adjust contrast
				writer.setColor(x, y, Color.gray(newGray));
			}
		}
		return outputImage;
	}

	public static Image applyGaussianBlur(Image inputImage, double radius) {
		ImageView imageView = new ImageView(inputImage);
		imageView.setEffect(new GaussianBlur(radius));
		return imageView.getImage();
	}

}

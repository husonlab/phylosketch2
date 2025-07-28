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

/*
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC4;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
 */


/**
 * utilities for working with images
 * Daniel Huson, 12.2024
 */
public class ImageUtils_OpenCV {
	/*
	public static int[][] convertToBinaryArray(Image image) {
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();

		// convert image to mat format
		var mat=imageToMat(width,height,image);

		// 1. Convert to grayscale
		{
			var gray = new Mat(height, width, CV_8UC4);
			cvtColor(mat, gray, COLOR_BGR2GRAY);
			mat = gray;
		}

		if(false) {
			// brightness and constrast
			var result = new Mat();
			var alpha = 2.0;  // contrast multiplier
			var beta = -100;  // brightness offset
			mat.convertTo(result, -1, alpha, beta);
			mat=result;
		}

		if(false) {
			// deblur
			var result = new Mat();
			GaussianBlur(mat, result, new Size(3, 3), 0);
			mat=result;
		}

// 2. Binarize using Otsu (or adaptive if lighting is uneven)

		if(false) {
			var result = new Mat(height, width, opencv_core.CV_8UC4);
			adaptiveThreshold(mat, result, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 11, 2);
			mat=result;
		} else if(true) {
			var result = new Mat(height, width, opencv_core.CV_8UC4);
			threshold(mat, result, 0, 255, THRESH_BINARY | THRESH_OTSU);
			mat=result;
		}

			if(true) {
				// 4. (Optional) Morphological operations to clean up noise and small blobs

				var kernel = getStructuringElement(MORPH_RECT, new Size(3, 3));
				var result = new Mat(height, width, opencv_core.CV_8UC4);
				morphologyEx(mat, result, MORPH_OPEN, kernel);
				mat=result;
			}

		return matToIntMatrix(mat);

	}

	public static Mat imageToMat(int width,int height,Image image) {
		var buffer = new byte[width * height * 4]; // ARGB
		var reader = image.getPixelReader();
		if (reader == null) {
			throw new IllegalArgumentException("Image has no pixel reader");
		}

		var format = PixelFormat.getByteBgraInstance(); // JavaFX uses BGRA
		reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);

		// Convert byte buffer to OpenCV Mat (CV_8UC4 for 4-channel image)
		var mat = new Mat(height, width, CV_8UC4);
		var pointer = mat.data();
		pointer.put(buffer);
		return mat;
	}

	public static int[][] matToIntMatrix(Mat mat) {
		var rows = mat.rows();
		var cols = mat.cols();

		if (mat.channels() != 1) {
			throw new IllegalArgumentException("Only single-channel Mats (e.g. grayscale) are supported");
		}

		var result = new int[rows][cols];

		var data = new byte[rows * cols];
		var pointer = mat.data();
		pointer.get(data);

		// Convert each byte to unsigned int and fill the matrix
		for (var i = 0; i < rows; i++) {
			for (var j = 0; j < cols; j++) {
				var val = data[i * cols + j] & 0xFF;
				result[i][j]=(val==0?1:0);
			}
		}
		return result;
	}
	 */
}

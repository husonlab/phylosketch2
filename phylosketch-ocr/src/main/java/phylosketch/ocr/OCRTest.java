/*
 *  OCRTest.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.ocr;

import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.io.File;

public class OCRTest {

	public static void main(String[] args) throws Exception {

		// 1. Load the test image
		var imageFile = new File("example.png");
		if (!imageFile.exists()) {
			System.err.println("Could not find example.png");
			return;
		}

		var bimg = ImageIO.read(imageFile);
		var fxImage = SwingFXUtils.toFXImage(bimg, null);

		// 2. Create desktop OCR implementation
		var ocr = new OCRService();

		// 3. Run OCR
		var result = ocr.getWords(fxImage);

		// 4. Print full text
		System.out.println("=== OCR RESULT ===");
		System.out.println(result);

		// 5. List words
		System.out.println("=== WORDS FOUND ===");
		for (var w : result) {
			System.out.println(w);
		}

		ocr.shutdown();
	}
}
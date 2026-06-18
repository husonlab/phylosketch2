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

package phylosketch.ocr.tesseract;

import javafx.application.Platform;
import javafx.scene.image.Image;
import phylosketch.ocr.OCRService;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Manual smoke test for the desktop (Tesseract) OCR path.
 * Usage: pass an image path, or place "example.png" in the working directory.
 * <p>
 * Daniel Huson, 11.2025
 */
public class OCRTest {

	public static void main(String[] args) throws Exception {
		// Initialize the JavaFX toolkit so images can be decoded, without launching an Application.
		try {
			var latch = new CountDownLatch(1);
			Platform.startup(latch::countDown);
			latch.await();
		} catch (IllegalStateException alreadyStarted) {
			// toolkit is already running
		}

		var imageFile = new File(args.length > 0 ? args[0] : "example.png");
		if (!imageFile.exists()) {
			System.err.println("Could not find " + imageFile);
			Platform.exit();
			return;
		}

		final Image image;
		try (var in = new FileInputStream(imageFile)) {
			image = new Image(in);
		}

		var ocr = new OCRService();
		System.out.println("Provider: " + ocr.providerName());

		var words = ocr.getWords(image);
		System.out.println("=== WORDS FOUND (" + words.size() + ") ===");
		for (var w : words) {
			System.out.println(w);
		}

		ocr.shutdown();
		Platform.exit();
	}
}

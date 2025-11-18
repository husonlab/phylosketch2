/*
 *  OCRService.java Copyright (C) 2025 Daniel H. Huson
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


import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.tesseract.TessBaseAPI;
import phylosketch.ocr.utils.PngEncoderFX;
import phylosketch.ocr.utils.TessdataManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.leptonica.global.leptonica.*;
import static org.bytedeco.tesseract.global.tesseract.RIL_WORD;

/**
 * OCR using Tesseract
 * Daniel Huson, 11.2025
 */
public class OCRService {
	private TessBaseAPI tesseract;


	/**
	 * get words in an image
	 *
	 * @param image the image
	 * @return the words
	 */
	public List<OcrWord> getWords(Image image) throws IOException {
		if (tesseract == null) {
			tesseract = createAPI();
		}
		var tempFile = File.createTempFile("image" + System.currentTimeMillis(), ".png");
		System.err.println(tempFile.getAbsolutePath());
		tempFile.deleteOnExit();

		save(image, tempFile);
		return getWords(tempFile.toString());
	}

	public boolean isAvailable() {
		return true;
	}

	public void shutdown() {
		if (tesseract != null) {
			tesseract.close();
			tesseract = null;
		}
	}

	private void save(Image image, File file) throws IOException {
		if (file.exists())
			Files.delete(file.toPath());
		var bytes = new PngEncoderFX(image, true, PngEncoderFX.FILTER_NONE, 9).pngEncode();
		try (var outs = new FileOutputStream(file)) {
			outs.write(bytes);
		}
	}

	/**
	 * get words in an image
	 *
	 * @param imagePath the image path
	 * @return the words
	 */
	private static List<OcrWord> getWords(String imagePath) throws IOException {
		var words = new ArrayList<OcrWord>();
		try (var api = createAPI(); var image = pixRead(imagePath)) {
			int width = pixGetWidth(image);
			int height = pixGetHeight(image);

			if (width == 0 || height == 0) {
				throw new IOException("Invalid image");
			}

			api.SetImage(image);

			if (api.Recognize(null) != 0) {
				throw new IOException("Recognition failed");
			}

			try (var ri = api.GetIterator()) {
				if (ri != null) {
					do {
						try (IntPointer left = new IntPointer(1);
							 IntPointer top = new IntPointer(1);
							 IntPointer right = new IntPointer(1);
							 IntPointer bottom = new IntPointer(1)) {

							ri.BoundingBox(RIL_WORD, left, top, right, bottom);

							var word = ri.GetUTF8Text(RIL_WORD);

							if (word != null) {
								var text = word.getString();
								float confidence = ri.Confidence(RIL_WORD);
								words.add(new OcrWord(text, confidence, new Rectangle2D(Math.min(left.get(), right.get()), Math.min(top.get(), bottom.get()), Math.abs(right.get() - left.get()), Math.abs(top.get() - bottom.get()))));
							}
						}
					} while (ri.Next(RIL_WORD));
				}
			}
		}
		return words;
	}

	private static TessBaseAPI createAPI() throws IOException {
		var tessDataDir = TessdataManager.getTessdataDir();
		System.setProperty("TESSDATA_PREFIX", tessDataDir.toString());

		var api = new TessBaseAPI();
		if (api.Init(tessDataDir.getAbsolutePath(), "eng") != 0) {
			api.close();
			throw new RuntimeException("Could not initialize Tesseract.");
		}

		api.SetVariable("user_words_suffix", "organism_names.txt");
		api.SetVariable("load_system_dawg", "F"); // Disable the default system dictionary
		api.SetVariable("load_freq_dawg", "F"); // Disable the frequency dictionary
		api.SetVariable("user_words", "tessdata/organism_names.txt");
		api.SetVariable("user_words_file", "tessdata/organism_names.txt");
		// todo: the white prevents words in quotes, so disabled for now
		if (false)
			api.SetVariable("tessedit_char_whitelist", "`'\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_+='()[],. ");
		return api;
	}


}

/*
 *  TesseractOcrProvider.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.tesseract.TessBaseAPI;
import phylosketch.ocr.OcrProvider;
import phylosketch.ocr.OcrWord;
import phylosketch.ocr.utils.PngEncoderFX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.leptonica.global.leptonica.*;
import static org.bytedeco.tesseract.global.tesseract.RIL_WORD;

/**
 * OCR provider backed by Tesseract (via the Bytedeco/JavaCPP presets). Desktop only:
 * JavaCPP loads its native libraries by extracting them at runtime, which neither GraalVM
 * native-image nor iOS allow, so this artifact must be kept off the iOS classpath.
 * <p>
 * One {@link TessBaseAPI} is created lazily and reused. Tesseract is not thread-safe, so all
 * access is serialized here (and again in {@code OCRService}).
 * <p>
 * Daniel Huson, 11.2025
 */
public class TesseractOcrProvider implements OcrProvider {
	private TessBaseAPI api;
	private Boolean available; // cached result of the first availability probe

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public String name() {
		return "Tesseract";
	}

	@Override
	public synchronized boolean isAvailable() {
		if (available == null) {
			try {
				ensureApi();
				available = true;
			} catch (Throwable t) {
				available = false;
			}
		}
		return available;
	}

	@Override
	public synchronized List<OcrWord> getWords(Image image) throws IOException {
		ensureApi();

		// Encode the JavaFX image to PNG in memory and let Leptonica read it directly,
		// avoiding the temp-file round trip the original used.
		var bytes = new PngEncoderFX(image, true, PngEncoderFX.FILTER_NONE, 9).pngEncode();

		var words = new ArrayList<OcrWord>();
		try (var data = new BytePointer(bytes); var pix = pixReadMem(data, bytes.length)) {
			if (pix == null || pixGetWidth(pix) == 0 || pixGetHeight(pix) == 0)
				throw new IOException("Invalid image");

			api.SetImage(pix);
			if (api.Recognize(null) != 0)
				throw new IOException("Recognition failed");

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
								word.deallocate(); // free the native string Tesseract allocated
								var confidence = ri.Confidence(RIL_WORD);
								words.add(new OcrWord(text, confidence, new Rectangle2D(
										Math.min(left.get(), right.get()),
										Math.min(top.get(), bottom.get()),
										Math.abs(right.get() - left.get()),
										Math.abs(bottom.get() - top.get()))));
							}
						}
					} while (ri.Next(RIL_WORD));
				}
			}
		}
		return words;
	}

	@Override
	public synchronized void shutdown() {
		if (api != null) {
			api.End();
			api.close();
			api = null;
		}
		available = null;
	}

	private void ensureApi() throws IOException {
		if (api != null)
			return;

		var tessDataDir = TessdataManager.getTessdataDir();
		System.setProperty("TESSDATA_PREFIX", tessDataDir.toString());

		var a = new TessBaseAPI();
		if (a.Init(tessDataDir.getAbsolutePath(), "eng") != 0) {
			a.close();
			throw new IOException("Could not initialize Tesseract.");
		}

		a.SetVariable("user_words_suffix", "organism_names.txt");
		a.SetVariable("load_system_dawg", "F"); // disable the default system dictionary
		a.SetVariable("load_freq_dawg", "F");   // disable the frequency dictionary
		a.SetVariable("user_words", "tessdata/organism_names.txt");
		a.SetVariable("user_words_file", "tessdata/organism_names.txt");
		// Note: a char whitelist prevents words in quotes, so it is intentionally left unset.

		api = a;
	}
}

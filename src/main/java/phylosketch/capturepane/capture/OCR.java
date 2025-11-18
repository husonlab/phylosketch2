/*
 * OCR.java Copyright (C) 2025 Daniel H. Huson
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
import phylosketch.ocr.OCRService;
import phylosketch.ocr.OcrWord;

import java.util.List;


/**
 * provides the interface to Tessseract
 * Daniel Huson, 3.2025
 */
public class OCR {
	private static OCRService ocrService;

	public static void setOCRService(OCRService ocrService) {
		OCR.ocrService = ocrService;
	}

	public static OCRService getOCRService() {
		return ocrService;
	}


	/**
	 * get words in an image
	 *
	 * @param image the image
	 * @return the words
	 */
	public static List<OcrWord> getWords(Image image) throws Exception {
		return ocrService.getWords(image);
	}
}

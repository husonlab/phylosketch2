/*
 * Copyright (C) 2025 Daniel H. Huson - GNU GPL v3 or later. See <http://www.gnu.org/licenses/>.
 */

/**
 * Desktop OCR provider backed by Tesseract (Bytedeco/JavaCPP presets).
 * Not for iOS: JavaCPP loads native libraries at runtime, which native-image/iOS do not allow.
 */
module phylosketch.ocr.tesseract {
	requires javafx.graphics;
	requires phylosketch.ocr;

	requires org.bytedeco.tesseract;  // TessBaseAPI, RIL_WORD, tesseract globals
	requires org.bytedeco.leptonica;  // PIX, pixReadMem, pixGetWidth/Height
	requires org.bytedeco.javacpp;    // BytePointer, IntPointer

	opens tessdata;                   // bundled *.traineddata + organism_names.txt

	provides phylosketch.ocr.OcrProvider
			with phylosketch.ocr.tesseract.TesseractOcrProvider;
}

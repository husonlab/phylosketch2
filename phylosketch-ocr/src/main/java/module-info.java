module phylosketch.ocr {
	requires javafx.graphics;
	requires org.bytedeco.tesseract;
	requires java.desktop;
	requires javafx.swing;

	opens tessdata;
	exports phylosketch.ocr;
	exports phylosketch.ocr.utils;
}
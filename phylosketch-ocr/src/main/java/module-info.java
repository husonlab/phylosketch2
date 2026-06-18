/*
 * Copyright (C) 2025 Daniel H. Huson - GNU GPL v3 or later. See <http://www.gnu.org/licenses/>.
 */

/**
 * PhyloSketch OCR API: the platform-neutral OCR contract.
 * Implementations (Tesseract on desktop, Apple Vision on iOS) are supplied by separate modules
 * and discovered at runtime via {@link java.util.ServiceLoader}.
 */
module phylosketch.ocr {
	requires javafx.graphics;          // Image, Rectangle2D, Platform; also covers PngEncoderFX

	exports phylosketch.ocr;
	exports phylosketch.ocr.utils;

	uses phylosketch.ocr.OcrProvider;  // discover whichever provider module is on the path
}

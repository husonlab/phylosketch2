/*
 *  OcrProvider.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.image.Image;

import java.io.IOException;
import java.util.List;

/**
 * Service-provider interface for OCR back-ends.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. Each build of
 * PhyloSketch ships exactly the providers that can run on that platform (Tesseract on
 * desktop, Apple Vision on iOS), so {@link OCRService} simply picks the best available one.
 * <p>
 * Every provider must return {@link OcrWord}s in the same coordinate convention:
 * pixel coordinates, origin top-left, matching the source image. Providers whose native
 * engine reports differently (Vision) are responsible for converting before returning.
 * <p>
 * Daniel Huson, 6.2026
 */
public interface OcrProvider {
	/**
	 * Can this provider actually run here? Implementations must answer cheaply and must not
	 * throw: a back-end whose native code is absent should return {@code false}, not crash.
	 */
	boolean isAvailable();

	/**
	 * Recognize words in the given image.
	 *
	 * @param image the image
	 * @return the recognized words, in image pixel coordinates (origin top-left)
	 */
	List<OcrWord> getWords(Image image) throws IOException;

	/**
	 * Release any native resources. Safe to call more than once.
	 */
	default void shutdown() {
	}

	/**
	 * When more than one provider is available (e.g. on macOS), the one with the highest
	 * priority is chosen. Vision overrides this to outrank Tesseract.
	 */
	default int priority() {
		return 0;
	}

	/**
	 * Short human-readable name, for logging / about boxes.
	 */
	default String name() {
		return getClass().getSimpleName();
	}
}

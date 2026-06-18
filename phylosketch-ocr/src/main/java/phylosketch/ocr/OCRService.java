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

import javafx.scene.image.Image;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Front door for OCR. Locates the best available {@link OcrProvider} (Tesseract on desktop,
 * Apple Vision on iOS) and delegates to it. The public API is unchanged from the original
 * Tesseract-only version, so existing call sites do not need to change.
 * <p>
 * OCR back-ends are not thread-safe (Tesseract in particular keeps mutable native state), so
 * this class serializes access. Call it off the JavaFX Application Thread.
 *
 * Daniel Huson, 11.2025
 */
public class OCRService {
	private OcrProvider provider;
	private boolean resolved;

	/**
	 * Get the words in an image.
	 *
	 * @param image the image
	 * @return the words, in image pixel coordinates (origin top-left)
	 * @throws IOException on recognition failure, or if no provider is available here
	 */
	public synchronized List<OcrWord> getWords(Image image) throws IOException {
		var p = provider();
		if (p == null)
			throw new IOException("No OCR provider is available on this platform");
		return p.getWords(image);
	}

	/**
	 * Is OCR available on this platform?
	 */
	public synchronized boolean isAvailable() {
		return provider() != null;
	}

	/**
	 * Name of the active provider ("Tesseract", "AppleVision", or "none").
	 */
	public synchronized String providerName() {
		var p = provider();
		return p == null ? "none" : p.name();
	}

	/**
	 * Release native resources held by the active provider.
	 */
	public synchronized void shutdown() {
		if (provider != null) {
			provider.shutdown();
			provider = null;
		}
		resolved = false;
	}

	private OcrProvider provider() {
		if (!resolved) {
			resolved = true;
			provider = locate();
		}
		return provider;
	}

	/**
	 * Find the highest-priority provider that reports itself available. Any provider whose
	 * native back-end fails to load is skipped rather than allowed to abort the search; this
	 * is what lets the same launcher code run on a device that only has one of the back-ends.
	 */
	private static OcrProvider locate() {
		return ServiceLoader.load(OcrProvider.class).stream()
				.map(OCRService::tryGet)
				.filter(Objects::nonNull)
				.filter(OCRService::tryIsAvailable)
				.max(Comparator.comparingInt(OcrProvider::priority))
				.orElse(null);
	}

	private static OcrProvider tryGet(ServiceLoader.Provider<OcrProvider> p) {
		try {
			return p.get();
		} catch (Throwable t) { // e.g. UnsatisfiedLinkError / NoClassDefFoundError on the wrong platform
			return null;
		}
	}

	private static boolean tryIsAvailable(OcrProvider p) {
		try {
			return p.isAvailable();
		} catch (Throwable t) {
			return false;
		}
	}
}

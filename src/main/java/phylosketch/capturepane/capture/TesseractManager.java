/*
 * TesseractManager.java Copyright (C) 2025 Daniel H. Huson
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

import net.sourceforge.tess4j.Tesseract;

import java.util.Locale;

/**
 * setup and manage tesseract
 * Daniel Huson, 1.2025
 */
public class TesseractManager {
	private static boolean loaded = false;

	public static Tesseract createInstance() {
		loadTesseractLibrary();

		var tesseract = new Tesseract();
		tesseract.setVariable("dump-config", "user-words");
		tesseract.setVariable("debug_file", "stderr");
		tesseract.setDatapath("tessdata");
		tesseract.setLanguage("eng");

		// Set the custom wordlist file
		if (true) {
			tesseract.setVariable("user_words_suffix", "organism_names.txt");
			tesseract.setVariable("load_system_dawg", "F"); // Disable the default system dictionary
			tesseract.setVariable("load_freq_dawg", "F"); // Disable the frequency dictionary
			tesseract.setVariable("user_words", "tessdata/organism_names.txt");
			tesseract.setVariable("user_words_file", "tessdata/organism_names.txt");
		}

		// Set optional configurations (e.g., character whitelist)
		tesseract.setVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_+='()[],. ");
		return tesseract;
	}

	public static void loadTesseractLibrary() {
		if (!loaded) {
			var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			if (os.contains("nix") || os.contains("nux") || os.contains("linux")) {
				System.setProperty("jna.library.path", "lib/linux");
			} else if (os.contains("mac")) {
				System.setProperty("jna.library.path", "lib/macos");
			} else if (os.contains("win")) {
				System.setProperty("jna.library.path", "lib/windows");
			} else {
				throw new RuntimeException("Unsupported OS: " + os);
			}
			loaded = true;
		}
	}
}

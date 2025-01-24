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

package phylocap.capture;

import net.sourceforge.tess4j.Tesseract;

/**
 * setup and manage tesseract
 * Daniel Huson, 1.2025
 */
public class TesseractManager {

	public static Tesseract createInstance() {
		var instance = new Tesseract();
		instance.setDatapath("tessdata");
		instance.setLanguage("eng");

		// Set the custom wordlist file
		instance.setVariable("user_words_suffix", "organism_names.txt");
		instance.setVariable("load_system_dawg", "F"); // Disable the default system dictionary
		instance.setVariable("load_freq_dawg", "F"); // Disable the frequency dictionary

		// Set the path to the wordlist file
		instance.setVariable("user-words", "tessdata/organism_names.txt");

		// Set optional configurations (e.g., character whitelist)
		instance.setVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+='()[],. ");
		return instance;
	}
}

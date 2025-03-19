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
import phylosketch.utils.NativeLibLoader;

import java.io.File;
import java.io.IOException;

/**
 * setup and manage tesseract
 * Daniel Huson, 1.2025
 */
public class TesseractManager {
	private static boolean loaded = false;

	public static Tesseract createInstance() {
		if (false)
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
			loaded = true;
			return;
		}
		if (!loaded) {

			if (true) {
				try {
					NativeLibLoader.loadLibrary("libtesseract");
					NativeLibLoader.loadLibrary("liblept");
					loaded = true;
				} catch (IOException ex) {
					System.err.println(ex.getMessage());
				}
			} else {
				// use something like -Djava.library.path=/Users/huson/IdeaProjects/apps/phylosketch2/lib/macos
				var libPath = System.getProperty("java.library.path");

			/*
			todo: need to figure out what to use from these:
			        /usr/local/opt/tesseract/lib/libtesseract.dylib (compatibility version 6.0.0, current version 6.4.0)
        			/usr/local/opt/leptonica/lib/libleptonica.dylib (compatibility version 7.0.0, current version 7.0.0)
			 */

				String libName;
				var os = System.getProperty("os.name").toLowerCase();
				{
					if (os.contains("mac")) {
						libName = "libtesseract.dylib";
					} else if (os.contains("linux")) {
						libName = "libtesseract.so";
					} else if (os.contains("win")) {
						libName = "tesseract.dll";
					} else {
						throw new RuntimeException("Unsupported OS: " + os);
					}
					var libFile = new File(libPath, libName);
					System.err.print("Loading library from: " + libFile.getAbsolutePath());
					System.load(libFile.getAbsolutePath());
					System.err.println(" done");
				}
				{
					if (os.contains("mac")) {
						libName = "libleptonica.dylib";
					} else if (os.contains("linux")) {
						libName = "liblept.so";
					} else if (os.contains("win")) {
						libName = "liblept.dll";
					} else {
						throw new RuntimeException("Unsupported OS: " + os);
					}
					var libFile = new File(libPath, libName);
					System.err.print("Loading library from: " + libFile.getAbsolutePath());
					System.load(libFile.getAbsolutePath());
					System.err.println(" done");
				}
				loaded = true;
			}
		}
	}
}

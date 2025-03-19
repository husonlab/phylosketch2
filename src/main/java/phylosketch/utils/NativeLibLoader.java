/*
 * NativeLibLoader.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class NativeLibLoader {
	private static final String OS = System.getProperty("os.name").toLowerCase();
	private static final String ARCH = System.getProperty("os.arch").toLowerCase();

	public static void loadLibrary(String libName) throws IOException {
		String libExtension;
		String osFolder;

		if (OS.contains("win")) {
			libExtension = ".dll";
			osFolder = "windows";
		} else if (OS.contains("mac")) {
			libExtension = ".dylib";
			osFolder = "macos";
		} else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
			libExtension = ".so";
			osFolder = "linux";
		} else {
			throw new UnsupportedOperationException("Unsupported OS: " + OS);
		}

		var libPath = "/native/" + osFolder + "/" + libName + libExtension;

		extractAndLoad(libPath, libName, libExtension);
	}

	private static void extractAndLoad(String resourcePath, String libName, String libExtension) throws IOException {
		var tempFile = File.createTempFile(libName, libExtension);
		tempFile.deleteOnExit();

		try (var in = NativeLibLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new FileNotFoundException("Native library not found: " + resourcePath);
			}
			try (var out = new FileOutputStream(tempFile)) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
			}
		}
		System.load(tempFile.getAbsolutePath());
	}
}
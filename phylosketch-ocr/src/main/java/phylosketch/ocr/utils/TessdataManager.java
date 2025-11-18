/*
 *  TessdataManager.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.ocr.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TessdataManager {

	private static Path tessdataDir;

	public static synchronized File getTessdataDir() throws IOException {
		if (tessdataDir == null) {
			// Example: ~/.phylosketch/tessdata
			var baseDir = Path.of(System.getProperty("user.home"), ".phylosketch");
			tessdataDir = baseDir.resolve("tessdata");

			if (!Files.exists(tessdataDir)) {
				Files.createDirectories(tessdataDir);
				copyResource("tessdata/eng.traineddata");
				copyResource("tessdata/lat.traineddata");
				copyResource("tessdata/organism_names.txt");
			}
		}
		return tessdataDir.toFile();
	}

	private static void copyResource(String resourcePath) throws IOException {
		try (var in = TessdataManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new IOException("Resource not found: " + resourcePath);
			}
			Path target = tessdataDir.resolve(Path.of(resourcePath).getFileName().toString());
			if (!Files.exists(target)) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
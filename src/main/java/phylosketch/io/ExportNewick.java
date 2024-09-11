/*
 * ExportNewick.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.io;

import javafx.stage.FileChooser;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.TextFileFilter;
import jloda.fx.window.NotificationManager;
import jloda.util.FileUtils;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;

/**
 * save in Newick format
 * Daniel Huson, 9.2024
 */
public class ExportNewick {
	public static void apply(MainWindow mainWindow) {
		final var fileChooser = new FileChooser();
		fileChooser.setTitle("Export Newick Format");

		final var previousFile = new File(ProgramProperties.get("NewickExport", ""));
		if (previousFile.isFile()) {
			fileChooser.setInitialDirectory(previousFile.getParentFile());
			fileChooser.setInitialFileName(mainWindow.getName() + ".newick");
		}
		fileChooser.setSelectedExtensionFilter(TextFileFilter.getInstance());
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Newick format", "*.tree", "*.tre", "*.trees", "*.new", "*.newick", "*.nwk", "*.treefile"), TextFileFilter.getInstance());
		var file = fileChooser.showSaveDialog(mainWindow.getStage());
		if (file != null) {
			try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(file.getPath())) {
				w.write(mainWindow.getDrawPane().toBracketString(true));
				ProgramProperties.put("NewickExport", file.getPath());
			} catch (IOException ex) {
				NotificationManager.showError("Export failed: " + ex);
			}
		}
	}
}


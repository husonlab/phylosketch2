/*
 * ImportNewickDialog.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.fx.util.RecentFilesManager;
import jloda.fx.util.TextFileFilter;
import jloda.util.ProgramProperties;
import phylosketch.window.MainWindow;

import java.io.File;

import static jloda.fx.util.FileOpenManager.getFileOpener;

/**
 * import dialog for Newick files
 * Daniel Huson, 10.2024
 */
public class ImportNewickDialog {
	public static void apply(MainWindow window) {
		File previousDir = new File(ProgramProperties.get("NewickImportFile", ""));
		FileChooser fileChooser = new FileChooser();
		if (previousDir.isDirectory()) {
			fileChooser.setInitialDirectory(previousDir);
		}

		if (ProgramProperties.getProgramVersion() != null) {
			fileChooser.setTitle("Open File - " + ProgramProperties.getProgramVersion());
		} else {
			fileChooser.setTitle("Open File");
		}

		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Newick format", "*.getTree", "*.tre", "*.trees", "*.new", "*.newick", "*.nwk", "*.treefile"), TextFileFilter.getInstance());

		File selectedFile = fileChooser.showOpenDialog(window.getStage());
		if (selectedFile != null && getFileOpener() != null) {
			ProgramProperties.put("NewickImportFile", selectedFile.getParent());
			getFileOpener().accept(selectedFile.getPath());
			RecentFilesManager.getInstance().insertRecentFile(selectedFile.getPath());
		}
	}
}

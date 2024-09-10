/*
 * Save.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.io;

import javafx.stage.FileChooser;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.RecentFilesManager;
import jloda.fx.util.TextFileFilter;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;

/**
 * save file
 * Daniel Huson, 1.2020
 */
public class Save {
    /**
     * save file
     *
	 */
    public static void apply(File file, MainWindow window) {
		try {
			InputOutput.save(file, window.getDrawPane());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * show save dialog
     *
     * @return true, if saved
     */
    public static boolean showSaveDialog(MainWindow window) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Save File - " + ProgramProperties.getProgramVersion());
        var currentFile = new File(window.getFileName());
        fileChooser.getExtensionFilters().addAll(PhyloSketchFileFilter.getInstance(), TextFileFilter.getInstance());

        if (!currentFile.isDirectory()) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
            fileChooser.setInitialFileName(currentFile.getName());
        } else {
            var tmp = new File(ProgramProperties.get("SaveFileDir", ""));
            if (tmp.isDirectory()) {
                fileChooser.setInitialDirectory(tmp);
            }
        }

        var selectedFile = fileChooser.showSaveDialog(window.getStage());

        if (selectedFile != null) {
            Save.apply(selectedFile, window);
            ProgramProperties.put("SaveFileDir", selectedFile.getParent());
            RecentFilesManager.getInstance().insertRecentFile(selectedFile.getPath());
			window.dirtyProperty().set(false);
            return true;
        } else
            return false;
    }
}

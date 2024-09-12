/*
 * FileOpener.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.fx.util.RecentFilesManager;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.NotificationManager;
import jloda.util.FileUtils;
import phylosketch.main.NewWindow;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * opens a file
 * Daniel Huson, 9.2024
 */
public class FileOpener implements Consumer<String> {

	@Override
	public void accept(String fileName) {
		var window = (MainWindow) MainWindowManager.getInstance().getLastFocusedMainWindow();
		if (window == null || !window.isEmpty())
			window = NewWindow.apply();
		accept(fileName, window);
	}

	public void accept(String fileName, MainWindow window) {
		var firstLine = Objects.requireNonNull(FileUtils.getFirstLineFromFile(new File(fileName))).trim().toLowerCase();
		try {
			if(firstLine.startsWith("graph")) {
				PhyloSketchIO.open(fileName, window.getDrawPane());
				window.fileNameProperty().set(fileName);
			}
			else if (firstLine.startsWith("#nexus")) {
				NotificationManager.showWarning("Nexus: not implemented");
			}
			else if (firstLine.startsWith("<nex:nexml") || firstLine.startsWith("<?xml version="))
				NotificationManager.showWarning("NEXML: not implemented");
			else if (firstLine.startsWith("(") || firstLine.contains(")")) {
				ImportNewick.apply(fileName, window.getDrawPane());
				window.dirtyProperty().set(true);
			}

			RecentFilesManager.getInstance().insertRecentFile(fileName);
		} catch (IOException e) {
			NotificationManager.showError("Open file failed: " + e.getMessage());
		}
	}
}
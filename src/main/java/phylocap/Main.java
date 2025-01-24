/*
 * Main.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jloda.fx.util.ProgramProperties;
import phylocap.window.CaptureView;

public class Main extends Application {
	@Override
	public void init() throws Exception {
		if (ProgramProperties.isMacOS())
			System.setProperty("jna.library.path", "lib/macos");
	}

	@Override
	public void start(Stage stage) throws Exception {
		var imageTab = new CaptureView();

		var scene = new Scene(imageTab.getRoot());
		stage.setScene(scene);
		stage.show();
	}
}

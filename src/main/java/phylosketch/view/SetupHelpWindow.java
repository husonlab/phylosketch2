/*
 * SetupHelpWindow.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.Stage;
import phylosketch.help.HelpView;
import phylosketch.main.Version;
import phylosketch.window.MainWindow;

/**
 * set up the help window
 * Daniel Huson, 12.2024
 */
public class SetupHelpWindow {
	private static Stage helpStage;

	public static void apply(MainWindow window, CheckMenuItem showHelpWindow) {
		if (helpStage == null) {
			var helpView = new HelpView();
			helpStage = new Stage();
			helpStage.setOnCloseRequest(event -> {
				event.consume();
				helpStage.hide();
			});
			helpStage.setTitle("Help - " + Version.SHORT_DESCRIPTION);
			helpStage.setScene(new Scene(helpView.getPane()));
			helpStage.setX(window.getStage().getX() + 50);
			helpStage.setY(window.getStage().getY() + 50);
		}
		showHelpWindow.setOnAction(e -> {
			if (showHelpWindow.isSelected())
				helpStage.show();
			else
				helpStage.hide();
		});
		helpStage.showingProperty().addListener((v, o, n) -> showHelpWindow.setSelected(n));
	}
}

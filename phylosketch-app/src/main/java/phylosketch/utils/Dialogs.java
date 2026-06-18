/*
 * Dialogs.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Dialogs {

	public static boolean askForConfirmation(Stage primaryStage, String title, String message, String question) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.initOwner(primaryStage);                // Tie dialog to main window
		alert.initModality(Modality.WINDOW_MODAL);   // Block only this window

		alert.setTitle(title);
		alert.setHeaderText(message);
		alert.setContentText(question);

		// Replace default OK/CANCEL with YES / NO / CANCEL
		alert.getButtonTypes().setAll(
				ButtonType.YES,
				ButtonType.NO,
				ButtonType.CANCEL
		);

		var result = alert.showAndWait();

		return result.isPresent() && result.get() == ButtonType.YES;
	}
}

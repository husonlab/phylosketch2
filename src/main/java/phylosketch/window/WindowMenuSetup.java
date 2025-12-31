/*
 *  WindowMenuSetup.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.window;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.RunAfterAWhile;
import jloda.fx.window.MainWindowManager;

import java.util.ArrayList;
import java.util.HashSet;

public class WindowMenuSetup {
	public static void setup(Menu windowMenu, Observable... observables) {
		final var originalWindowMenuItems = new ArrayList<>(windowMenu.getItems());

		final InvalidationListener invalidationListener = observable -> RunAfterAWhile.applyInFXThread(originalWindowMenuItems, () -> {
			windowMenu.getItems().setAll(originalWindowMenuItems);
			var count = 1;
			var seen = new HashSet<String>();
			for (var mainWindow : MainWindowManager.getInstance().getMainWindows()) {
				if (mainWindow.getStage() != null && mainWindow.getStage().getTitle() != null) {
					var title = mainWindow.getStage().getTitle().replaceAll("- " + ProgramProperties.getProgramName(), "");
					while (seen.contains(title)) {
						title = title + "+";
					}
					seen.add(title);

					try {
						var menuItem = new MenuItem(title);
						menuItem.setOnAction(e -> Platform.runLater(() -> mainWindow.getStage().toFront()));
						if (count <= 9)
							menuItem.setAccelerator(new KeyCharacterCombination("" + (count++), KeyCombination.SHORTCUT_DOWN));
						windowMenu.getItems().add(menuItem);
					} catch (Exception ignored) {
					}
				}
				if (MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow) != null) {
					for (var auxStage : MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow)) {
						if (auxStage.getTitle() != null) {
							var title = auxStage.getTitle().replaceAll("- " + ProgramProperties.getProgramName(), "");
							while (seen.contains(title)) {
								title = title + "+";
							}
							seen.add(title);
							try {
								var menuItem = new MenuItem(title);
								menuItem.setOnAction(e -> Platform.runLater(auxStage::toFront));
								Platform.runLater(() -> windowMenu.getItems().add(menuItem));
							} catch (Exception ignored) {
							}
						}
					}
				}
			}
		});
		MainWindowManager.getInstance().changedProperty().addListener(invalidationListener);
		for (var observable : observables) {
			observable.addListener(invalidationListener);
		}
		RunAfterAWhile.applyInFXThread(invalidationListener, () -> invalidationListener.invalidated(null));
	}

}

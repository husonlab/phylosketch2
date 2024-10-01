/*
 *  CheckForUpdate.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch2.main;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import jloda.fx.util.ProgramProperties;
import jloda.fx.window.NotificationManager;
import jloda.util.Basic;

import java.util.concurrent.Executors;

/**
 * check for update
 * Daniel Huson, 5.2018
 */
public class CheckForUpdate {
	public static final String programURL = "https://software-ab.cs.uni-tuebingen.de/download/phylosketch";
	public static final String applicationId = "1691242391";

	/**
	 * check for update, download and install, if present
	 */
	public static void apply() {
		try {
			final ApplicationDisplayMode applicationDisplayMode = ProgramProperties.isUseGUI() ? ApplicationDisplayMode.GUI : ApplicationDisplayMode.CONSOLE;
			final UpdateDescriptor updateDescriptor = UpdateChecker.getUpdateDescriptor(programURL + "/updates.xml", applicationDisplayMode);
			final UpdateDescriptorEntry possibleUpdate = updateDescriptor.getPossibleUpdateEntry();
			if (possibleUpdate == null) {
                NotificationManager.showInformation("Installed version is up-to-date");
            } else {
                if (!ProgramProperties.isUseGUI()) {
                    NotificationManager.showInformation("New version available: " + possibleUpdate.getNewVersion() + "\nPlease download from: " + programURL);
                } else {
                    final Runnable runnable = () -> {
                        System.err.println("Launching update dialog");
                        ApplicationLauncher.launchApplicationInProcess(applicationId, null,
                                new ApplicationLauncher.Callback() {
                                    public void exited(int exitValue) {
                                        System.err.println("Exit value: " + exitValue);
                                    }

                                    public void prepareShutdown() {
                                        ProgramProperties.store();
                                    }
                                },
                                ApplicationLauncher.WindowMode.FRAME, null);
                    };
                    //SwingUtilities.invokeLater(runnable);
                    Executors.newSingleThreadExecutor().submit(runnable);
                }
            }
        } catch (Exception e) {
            Basic.caught(e);
            NotificationManager.showInformation("Failed to check for updates: " + e);
        }
    }
}

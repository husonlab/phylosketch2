/*
 * PhyloSketch.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import jloda.fx.util.ArgsOptions;
import jloda.fx.util.FileOpenManager;
import jloda.fx.util.ProgramProperties;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.NotificationManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.util.Basic;
import jloda.util.ProgramExecutorService;
import jloda.util.UsageException;
import phylosketch.io.PhyloSketchIO;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;

/**
 * runs the phylosketch program
 * Daniel Huson, 9.2024
 */
public class PhyloSketch extends Application {
	public static boolean test = false;

    private static String[] inputFilesAtStartup;

    private static boolean desktop = true;

    @Override
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(ProgramProperties::store));
        ProgramProperties.setUseGUI(true);
    }

    /**
     * main
     */
    public static void main(String[] args)  {
        Basic.restoreSystemOut(System.err); // send system out to system err
        Basic.startCollectionStdErr();

        ProgramProperties.getProgramIconsFX().setAll(Utils.getImage(PhyloSketch.class,"PhyloSketch-512x512.png"));

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);
        ProgramProperties.setProgramLicence("""
                Copyright (C) 2025. This program comes with ABSOLUTELY NO WARRANTY.
                This is free software, licensed under the terms of the GNU General Public License, Version 3.
                Sources available at: https://github.com/husonlab/phylosketch
                """);
        SplashScreen.setVersionString(ProgramProperties.getProgramVersion());
        SplashScreen.setImage(Utils.getImage(PhyloSketch.class,"PhyloSketch-Splash.png"));
        SplashScreen.setLabelAnchor(new Point2D(20, 10));

        try {
            parseArguments(args);
        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
            if (!th.getMessage().startsWith("Help")) {
                System.err.println("Fatal error:" + "\n" + th);
                Basic.caught(th);
            }
            System.exit(1);
        }

        launch(args);
    }

    protected static void parseArguments(String[] args) throws UsageException {
        var options = new ArgsOptions(args, PhyloSketch.class, "Sketch phylogenetic trees and networks");
        options.setAuthors("Daniel H. Huson");
        options.setLicense(ProgramProperties.getProgramLicence());
        options.setVersion(ProgramProperties.getProgramVersion());

        options.comment("Input:");
        inputFilesAtStartup = options.getOption("-i", "input", "Input file(s)", new String[0]);

        options.comment(ArgsOptions.OTHER);

        final var propertiesFile = options.getOption("-p", "propertiesFile", "Properties file", getDefaultPropertiesFile());
        final var showVersion = options.getOption("-V", "version", "Show version string", false);
        final var silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-t", "threads", "Maximum number of threads to use in a parallel algorithm (0=all available)", 0));
        ProgramProperties.setConfirmQuit(options.getOption("-q", "confirmQuit", "Confirm quit on exit", ProgramProperties.isConfirmQuit()));
        test = true;
        options.getOption("!x", "x", "test", false);
        ProgramProperties.put("MaxNumberRecentFiles", 100);
        options.done();

        ProgramProperties.load(propertiesFile);

        if (silentMode) {
            Basic.stopCollectingStdErr();
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }

        if (showVersion) {
            System.err.println(ProgramProperties.getProgramVersion());
            System.err.println(jloda.util.Version.getVersion(PhyloSketch.class, ProgramProperties.getProgramName()));
            System.err.println("Java version: " + System.getProperty("java.version"));
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (false)
            SplashScreen.showSplash(Duration.ofSeconds(3));
        try {
            primaryStage.setTitle("Untitled - " + ProgramProperties.getProgramName());
            NotificationManager.setShowNotifications(false);

            final var mainWindow = new MainWindow();

            final var windowGeometry = new WindowGeometry(ProgramProperties.get("WindowGeometry", "50 50 800 800"));

            mainWindow.show(primaryStage, windowGeometry.getX(), windowGeometry.getY(), windowGeometry.getWidth(), windowGeometry.getHeight());
            MainWindowManager.getInstance().addMainWindow(mainWindow);

            if(true) // reopen last
            {
                var last=ProgramProperties.get("Last","");
                if(!last.isBlank()) {
                    Platform.runLater(()->{
						try {
							if (false)
								System.err.println(last);
                            PhyloSketchIO.load(new StringReader(last), mainWindow.getDrawView(), mainWindow.getPresenter().getCapturePane().getImageView());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
                }

            }

            for (var fileName : inputFilesAtStartup) {
                Platform.runLater(() -> FileOpenManager.getFileOpener().accept(fileName));
            }
        } catch (Exception ex) {
            Basic.caught(ex);
            throw ex;
        }
    }

    @Override
    public void stop() {
        ProgramProperties.store();
        System.exit(0);
    }

    public static String getDefaultPropertiesFile() {
        if (ProgramProperties.isMacOS())
            return System.getProperty("user.home") + "/Library/Preferences/PhyloSketch2.def";
        else
            return System.getProperty("user.home") + File.separator + ".PhyloSketch2.def";
    }

    public static boolean isDesktop() {
        return desktop;
    }

    public static void setDesktop(boolean desktop) {
        PhyloSketch.desktop = desktop;
    }
}

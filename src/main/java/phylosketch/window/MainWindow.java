/*
 * MainWindow.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.window;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import jloda.fx.util.FileOpenManager;
import jloda.fx.util.MemoryUsage;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.StatementFilter;
import jloda.fx.window.IMainWindow;
import jloda.fx.window.MainWindowManager;
import jloda.phylo.PhyloTree;
import jloda.util.FileUtils;
import phylosketch.io.ExtensionFilters;
import phylosketch.io.FileOpener;
import phylosketch.main.PhyloSketch;
import phylosketch.view.DrawView;

import java.io.IOException;
import java.util.Objects;

/**
 * the main window
 * Daniel Huson, 7.2019
 */
public class MainWindow implements IMainWindow {
    private Stage stage;

    private final Scene scene;

    private final MainWindowController controller;

    private MainWindowPresenter presenter;

    private final FlowPane statusPane;

    private final StringProperty fileName = new SimpleStringProperty("Untitled");
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);


    private final BooleanProperty empty = new SimpleBooleanProperty(this, "empty", true);

    private final StringProperty name = new SimpleStringProperty(this, "name", "Untitled");

    private final DrawView drawView = new DrawView();

    static {
        PhyloTree.SUPPORT_RICH_NEWICK = true;
    }

    /**
     * constructor
     */
    public MainWindow() {
        Platform.setImplicitExit(false);

        Parent root;
        {
            var fxmlLoader = new FXMLLoader();
            try (var ins = StatementFilter.applyMobileFXML(Objects.requireNonNull(MainWindowController.class.getResource("MainWindow.fxml")).openStream(), PhyloSketch.isDesktop())) {
                fxmlLoader.load(ins);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            root = fxmlLoader.getRoot();
            controller = fxmlLoader.getController();
            statusPane = controller.getBottomFlowPane();
        }

        FileOpenManager.setExtensions(ExtensionFilters.allSupported());
        FileOpenManager.setFileOpener(new FileOpener());

        final InvalidationListener listener = (e -> {
            name.set(getFileName() == null ? "Untitled" : FileUtils.getFileNameWithoutPathOrSuffix(getFileName()));
            if (getStage() != null)
                getStage().setTitle(getName() + (isDirty() ? "*" : "") + " - " + ProgramProperties.getProgramName());
        });
        fileNameProperty().addListener(listener);
        dirtyProperty().addListener(listener);

        scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("MainWindow.css")).toExternalForm());
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public IMainWindow createNew() {
        return new MainWindow();
    }

    @Override
    public void show(Stage stage, double screenX, double screenY, double width, double height) {
        if (stage == null)
            stage = new Stage();
        this.stage = stage;

        stage.getIcons().addAll(ProgramProperties.getProgramIconsFX());
        stage.setScene(scene);
        stage.setX(screenX);
        stage.setY(screenY);
        stage.setWidth(width);
        stage.setHeight(height);

        stage.titleProperty().addListener((e) -> MainWindowManager.getInstance().fireChanged());

        presenter = new MainWindowPresenter(this);

        final MemoryUsage memoryUsage = MemoryUsage.getInstance();
        controller.getMemoryUsageLabel().textProperty().bind(memoryUsage.memoryUsageStringProperty());

        stage.show();

        empty.bind(drawView.getGraphFX().emptyProperty());
    }

    @Override
    public boolean isEmpty() {
        return empty.get();
    }

    @Override
    public void close() {
        stage.hide();
    }


    public MainWindowController getController() {
        return controller;
    }

    public MainWindowPresenter getPresenter() {
        return presenter;
    }

    public FlowPane getStatusPane() {
        return statusPane;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public DrawView getDrawView() {
        return drawView;
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        fileNameProperty().set(fileName);
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }
}

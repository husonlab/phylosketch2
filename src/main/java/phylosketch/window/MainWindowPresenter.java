/*
 *  MainWindowPresenter.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.control.CopyableLabel;
import jloda.fx.control.RichTextLabel;
import jloda.fx.dialog.ExportImageDialog;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.*;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.WindowGeometry;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.FileUtils;
import phylosketch.io.ExportNewick;
import phylosketch.io.PhyloSketchIO;
import phylosketch.io.Save;
import phylosketch.io.SaveBeforeClosingDialog;
import phylosketch.main.NewWindow;
import phylosketch.view.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

/**
 * setup all control bindings
 * Daniel Huson, 9.2024
 */
public class MainWindowPresenter {

	public MainWindowPresenter(MainWindow window) {
		var controller = window.getController();
		var view = window.getDrawPane();
		var undoManager = view.getUndoManager();

		view.getUndoManager().undoStackSizeProperty().addListener((v,o,n)->{
			window.dirtyProperty().set(n.intValue()>0);
		});

		view.minWidthProperty().bind(Bindings.createDoubleBinding(() ->
				controller.getScrollPane().getViewportBounds().getWidth(), controller.getScrollPane().viewportBoundsProperty()));

		view.minHeightProperty().bind(Bindings.createDoubleBinding(() ->
				controller.getScrollPane().getViewportBounds().getHeight(), controller.getScrollPane().viewportBoundsProperty()));
		controller.getScrollPane().setContent(view);

		controller.getQuitMenuItem().setOnAction((e) -> {
			while (MainWindowManager.getInstance().size() > 0) {
				final MainWindow aWindow = (MainWindow) MainWindowManager.getInstance().getMainWindow(MainWindowManager.getInstance().size() - 1);
				if (SaveBeforeClosingDialog.apply(aWindow) == SaveBeforeClosingDialog.Result.cancel || !MainWindowManager.getInstance().closeMainWindow(aWindow))
					break;
			}
		});

		controller.getNewMenuItem().setOnAction(e -> NewWindow.apply());
		controller.getOpenMenuItem().setOnAction(FileOpenManager.createOpenFileEventHandler(window.getStage()));

		controller.getModeButton().setOnAction(e -> {
			if (view.isEditable()) {
				view.editableProperty().set(false);
				view.moveableProperty().set(true);
				controller.getModeButton().setTooltip(new Tooltip("Move mode"));
				MaterialIcons.setIcon(controller.getModeButton(), MaterialIcons.open_with);
			} else if (view.isMoveable()) {
				view.editableProperty().set(false);
				view.moveableProperty().set(false);
				controller.getModeButton().setTooltip(new Tooltip("Locked mode"));
				MaterialIcons.setIcon(controller.getModeButton(), MaterialIcons.lock);
			} else {
				view.editableProperty().set(true);
				view.moveableProperty().set(true);
				controller.getModeButton().setTooltip(new Tooltip("Edit mode"));
				MaterialIcons.setIcon(controller.getModeButton(), MaterialIcons.edit);
			}
		});

		view.arrowHeadsProperty().addListener((v, o, n) -> {
			for (var node : view.getEdgesGroup().getChildren()) {
				if (node instanceof Path path) {
					if (n)
						DrawUtils.addArrowHead(path);
					else DrawUtils.removeArrowHead(path);
				}
			}
		});

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>) e -> {
			while (e.next()) {
				for (var node : e.getAddedSubList()) {
					if (node instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
						shape.setOnContextMenuRequested(cm -> {
							var setLabel = new MenuItem("Edit Label");
							setLabel.setOnAction(x -> NodeLabelDialog.apply(window.getStage(), view, v));
							new ContextMenu(setLabel).show(window.getStage(), cm.getScreenX(), cm.getScreenY());
						});
					}
				}
			}
		});
		view.getNodeLabelsGroup().getChildren().addListener((ListChangeListener<? super Node>) e -> {
			while (e.next()) {
				for (var node : e.getAddedSubList()) {
					if (node instanceof RichTextLabel richTextLabel && richTextLabel.getUserData() instanceof Integer nodeId) {
						richTextLabel.setOnContextMenuRequested(cm -> {
							var v = view.getGraph().findNodeById(nodeId);
							var setLabel = new MenuItem("Edit Label");
							setLabel.setOnAction(x -> NodeLabelDialog.apply(window.getStage(), view, v));
							new ContextMenu(setLabel).show(window.getStage(), cm.getScreenX(), cm.getScreenY());
						});
					}
				}
			}
		});

		RecentFilesManager.getInstance().setFileOpener(FileOpenManager.getFileOpener());
		RecentFilesManager.getInstance().setupMenu(controller.getRecentFilesMenu());

		window.getStage().setOnCloseRequest((e) -> {
			if (view.getGraph().getNumberOfNodes() > 0) {
				var w = new StringWriter();
				try {
					PhyloSketchIO.save(w, view);
					ProgramProperties.put("Last", w.toString());
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}

			controller.getCloseMenuItem().getOnAction().handle(null);
			e.consume();
		});

		controller.getCloseMenuItem().setOnAction(e -> {
			if (SaveBeforeClosingDialog.apply(window) != SaveBeforeClosingDialog.Result.cancel) {
				ProgramProperties.put("WindowGeometry", (new WindowGeometry(window.getStage())).toString());
				MainWindowManager.getInstance().closeMainWindow(window);
			}
		});

		controller.getSaveMenuItem().setOnAction(e -> Save.showSaveDialog(window));
		controller.getSaveMenuItem().disableProperty().bind(window.dirtyProperty().not());
		//controller.getSaveButton().setOnAction(controller.getSaveAsMenuItem().getOnAction());
		//controller.getSaveButton().disableProperty().bind(controller.getSaveAsMenuItem().disableProperty());

		controller.getUndoMenuItem().setOnAction(e -> view.getUndoManager().undo());
		controller.getUndoMenuItem().disableProperty().bind(view.getUndoManager().undoableProperty().not());
		controller.getRedoMenuItem().setOnAction(e -> view.getUndoManager().redo());
		controller.getRedoMenuItem().disableProperty().bind(view.getUndoManager().redoableProperty().not());

		controller.getDeleteMenuItem().setOnAction(unused -> {
			view.getUndoManager().doAndAdd(new DeleteNodesEdgesCommand(view,
					view.getNodeSelection().getSelectedItems(),
					view.getEdgeSelection().getSelectedItems()));
		});
		controller.getDeleteMenuItem().disableProperty().bind((view.getNodeSelection().sizeProperty().isEqualTo(0)
				.and(view.getEdgeSelection().sizeProperty().isEqualTo(0))).or(view.editableProperty().not()));

		controller.getClearMenuItem().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new DeleteNodesEdgesCommand(view, view.getGraph().getNodesAsList(),
					Collections.emptyList()));
		});
		controller.getClearMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(view.editableProperty().not()));

		controller.getZoomInMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.1, 1.1);
		});
		controller.getZoomOutMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.0 / 1.1, 1.1);
		});

		controller.getArrowsMenuItem().selectedProperty().addListener((v, o, n) -> {
			view.arrowHeadsProperty().set(n);
			if (n)
				controller.getOutlineEdgesMenuItem().setSelected(false);
		});
		controller.getOutlineEdgesMenuItem().selectedProperty().addListener((v, o, n) -> {
			view.setOutlineEdges(n);
			if (n)
				controller.getArrowsMenuItem().setSelected(false);
		});

		MainWindowManager.useDarkThemeProperty().addListener(e->{
			if(view.getOutlineEdges()) { // need to trigger recomputation
				Platform.runLater(()->view.setOutlineEdges(false));
				RunAfterAWhile.applyInFXThread(controller.getOutlineEdgesMenuItem(),()->view.setOutlineEdges(true));
			}
		});

		controller.getCopyMenuItem().setOnAction(e -> ClipboardUtils.putString(view.toBracketString(true) + "\n"));
		controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportImageMenuItem().setOnAction(e -> {
			if (view.getOutlineEdges())
				view.setOutlineEdges(false);
			try {
				ExportImageDialog.show(window.getFileName(), window.getStage(), window.getDrawPane());
			} finally {
				view.setOutlineEdges(controller.getOutlineEdgesMenuItem().isSelected());
			}
		});
		controller.getExportImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportNewickMenuItem().setOnAction(e -> {
			ExportNewick.apply(window);
		});
		controller.getExportNewickMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getCopyImageMenuItem().setOnAction(e -> {
			var params = new SnapshotParameters();
			params.setFill(MainWindowManager.isUseDarkTheme()?Color.BLACK:Color.WHITE);
			params.setTransform(javafx.scene.transform.Transform.scale(2.0, 2.0));
			var writableImage = new WritableImage((int) (view.getWidth() * 2), (int) (view.getHeight() * 2));
			var snapshot = view.snapshot(params, writableImage);
			ClipboardUtils.putImage(snapshot);
		});
		controller.getCopyImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeavesABCMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelLeavesABC(view))));
		controller.getLabelLeavesABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeaves123MenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelLeaves123(view))));
		controller.getLabelLeaves123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeavesMenuItem().setOnAction(c -> LabelLeaves.labelLeaves(window.getStage(), view));
		controller.getLabelLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternalABCMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelInternalABC(view))));
		controller.getLabelInternalABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternal123MenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelInternal123(view))));
		controller.getLabelInternal123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getClearLabelsMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.clear(view))));
		controller.getClearLabelsMenuItem().disableProperty().bind(Bindings.isEmpty(view.getNodeLabelsGroup().getChildren()));


		controller.getUseDarkThemeCheckMenuItem().selectedProperty().bindBidirectional(MainWindowManager.useDarkThemeProperty());
		BasicFX.setupFullScreenMenuSupport(window.getStage(), controller.getFullScreenMenuItem());

		var infoLabel = new CopyableLabel();
		view.getGraphFX().lastUpdateProperty().addListener(e -> {
			try (var componentMap = view.getGraph().newNodeIntArray()) {
				var components = view.getGraph().computeConnectedComponents(componentMap);
				var roots = view.getGraph().nodeStream().filter(v -> v.getInDegree() == 0).count();
				infoLabel.setText("comps=" + components + " roots=" + roots + " " + RootedNetworkProperties.computeInfoString(view.getGraph()));
			}
		});
		controller.getBottomFlowPane().getChildren().add(infoLabel);

		SetupSelection.setupSelect(view, controller);

		controller.getImportButton().setOnAction(e -> openString(ClipboardUtils.getTextFilesContentOrString()));
		controller.getImportButton().disableProperty().bind(ClipboardUtils.hasStringProperty().not().and(ClipboardUtils.hasFilesProperty().not()));
	}

	public static void openString(String string) {
		if (string != null && !string.isBlank()) {
			try {
				var file = FileUtils.getUniqueFileName(ProgramProperties.get("SaveFileDir", System.getProperty("user.dir")), "Untitled", ".psketch");
				FileUtils.writeLinesToFile(List.of(string), file.getPath(), false);
				FileOpenManager.getFileOpener().accept(file.getPath());
			} catch (IOException ignored) {
			}
		}
	}
}

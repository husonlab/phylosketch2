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

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.shape.Shape;
import jloda.fx.control.CopyableLabel;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.BasicFX;
import jloda.fx.util.ClipboardUtils;
import jloda.fx.util.ProgramProperties;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.WindowGeometry;
import jloda.graph.NodeIntArray;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.StringUtils;
import phylosketch.io.InputOutput;
import phylosketch.io.Save;
import phylosketch.io.SaveBeforeClosingDialog;
import phylosketch.view.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Objects;

/**
 * setup all control bindings
 * Daniel Huson, 9.2024
 */
public class MainWindowPresenter {
	public MainWindowPresenter(MainWindow window) {
		var controller = window.getController();
		var view = window.getDrawPane();
		var graph = view.getGraph();
		var undoManager = view.getUndoManager();

		window.dirtyProperty().bind(view.getUndoManager().undoableProperty());

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

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>)e->{
			while(e.next()) {
				for(var node : e.getAddedSubList()) {
					if(node instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
						shape.setOnContextMenuRequested(cm->{
							var setLabel = new MenuItem("Edit Label");
							setLabel.setOnAction(x -> NodeLabelDialog.apply(window.getStage(), view, v));
							new ContextMenu(setLabel).show(window.getStage(), cm.getScreenX(), cm.getScreenY());
						});
					}
				}
			}
		});
		view.getNodeLabelsGroup().getChildren().addListener((ListChangeListener<? super Node>)e->{
			while(e.next()) {
				for(var node : e.getAddedSubList()) {
					if(node instanceof RichTextLabel richTextLabel && richTextLabel.getUserData() instanceof Integer nodeId) {
						richTextLabel.setOnContextMenuRequested(cm->{
							var v=view.getGraph().findNodeById(nodeId);
							var setLabel = new MenuItem("Edit Label");
							setLabel.setOnAction(x -> NodeLabelDialog.apply(window.getStage(), view, v));
							new ContextMenu(setLabel).show(window.getStage(), cm.getScreenX(), cm.getScreenY());
						});
					}
				}
			}
		});

		window.getStage().setOnCloseRequest((e) -> {
			if(view.getGraph().getNumberOfNodes()>0) {
				var w = new StringWriter();
				try {
					InputOutput.save(w, view);
					ProgramProperties.put("Last", w.toString());
					System.err.println(ProgramProperties.get("Last"));
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
		controller.getSaveMenuItem().disableProperty().bind(window.dirtyProperty());
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
		controller.getDeleteMenuItem().disableProperty().bind(view.getNodeSelection().sizeProperty().isEqualTo(0)
				.and(view.getEdgeSelection().sizeProperty().isEqualTo(0)));

		controller.getClearMenuItem().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new DeleteNodesEdgesCommand(view, view.getGraph().getNodesAsList(),
					Collections.emptyList()));
		});
		controller.getClearMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getZoomInMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.1, 1.1);
		});
		controller.getZoomOutMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.0 / 1.1, 1.1);
		});

		controller.getCopyMenuItem().setOnAction(e -> ClipboardUtils.putString(view.toBracketString(false)+"\n"));
		controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getCopyImageMenuItem().setOnAction(e -> ClipboardUtils.putImage(view.snapshot(null, null)));
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

		controller.getClearLabelsMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view,  LabelLeaves.clear(view))));
		controller.getClearLabelsMenuItem().disableProperty().bind(Bindings.isEmpty(view.getNodeLabelsGroup().getChildren()));


		controller.getUseDarkThemeCheckMenuItem().selectedProperty().bindBidirectional(MainWindowManager.useDarkThemeProperty());
		BasicFX.setupFullScreenMenuSupport(window.getStage(), controller.getFullScreenMenuItem());

		var infoLabel=new CopyableLabel();
		view.getGraphFX().lastUpdateProperty().addListener(e->{
			try(var componentMap=view.getGraph().newNodeIntArray()) {
				var components = view.getGraph().computeConnectedComponents(componentMap);
				var roots=view.getGraph().nodeStream().filter(v->v.getInDegree()==0).count();
				infoLabel.setText("comps="+components+" roots="+roots+" "+RootedNetworkProperties.computeInfoString(view.getGraph()));
			}
		});
		controller.getBottomFlowPane().getChildren().add(infoLabel);

		SetupSelection.setupSelect(view, controller);
	}

}

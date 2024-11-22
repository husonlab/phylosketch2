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
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.control.RichTextLabel;
import jloda.fx.dialog.ExportImageDialog;
import jloda.fx.find.FindToolBar;
import jloda.fx.find.Searcher;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.qr.QRViewUtils;
import jloda.fx.util.*;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import phylosketch.commands.*;
import phylosketch.io.ExportNewick;
import phylosketch.io.PhyloSketchIO;
import phylosketch.io.Save;
import phylosketch.io.SaveBeforeClosingDialog;
import phylosketch.main.CheckForUpdate;
import phylosketch.main.NewWindow;
import phylosketch.main.PhyloSketch;
import phylosketch.view.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * setup all control bindings
 * Daniel Huson, 9.2024
 */
public class MainWindowPresenter {
	private final FindToolBar findToolBar;

	private final BooleanProperty allowResize = new SimpleBooleanProperty(this, "enableResize", false);
	private final BooleanProperty allowRemoveSuperfluous = new SimpleBooleanProperty(this, "allowRemoveSuperfluous", false);
	private final BooleanProperty allowReRoot = new SimpleBooleanProperty(this, "allowReRoot", false);

	public MainWindowPresenter(MainWindow window) {
		var controller = window.getController();
		var view = window.getDrawPane();
		var undoManager = view.getUndoManager();

		findToolBar = new FindToolBar(window.getStage(), setupSearcher(view));
		findToolBar.setShowFindToolBar(false);
		findToolBar.setShowReplaceToolBar(false);
		controller.getTopVBox().getChildren().add(findToolBar);
		controller.getFindButton().setOnAction(e -> {
			if (!findToolBar.isShowFindToolBar()) {
				findToolBar.setShowFindToolBar(true);
				Platform.runLater(() -> controller.getFindButton().setSelected(true));
			} else if (!findToolBar.isShowReplaceToolBar()) {
				findToolBar.setShowReplaceToolBar(true);
				Platform.runLater(() -> controller.getFindButton().setSelected(true));
			} else {
				findToolBar.setShowFindToolBar(false);
				findToolBar.setShowReplaceToolBar(false);
				Platform.runLater(() -> controller.getFindButton().setSelected(false));
			}
		});
		findToolBar.showFindToolBarProperty().addListener(a -> {
			if (!findToolBar.isShowFindToolBar() && !findToolBar.isShowReplaceToolBar()) {
				controller.getFindButton().setSelected(false);
			}
		});
		findToolBar.showReplaceToolBarProperty().addListener(a -> {
			if (!findToolBar.isShowFindToolBar() && !findToolBar.isShowReplaceToolBar()) {
				controller.getFindButton().setSelected(false);
			}
		});

		if (false) {
			view.getGraphFX().getNodeList().addListener((ListChangeListener<? super jloda.graph.Node>) a -> {
				while (a.next()) {
					for (var v : a.getAddedSubList()) {
						System.err.println("Node added: " + v.getId());
					}
					for (var v : a.getRemoved()) {
						System.err.println("Node removed: " + v.getId());
					}
				}
			});
			view.getGraphFX().getEdgeList().addListener((ListChangeListener<? super jloda.graph.Edge>) a -> {
				while (a.next()) {
					for (var e : a.getAddedSubList()) {
						System.err.println("Edge added: " + e.getId());
					}
					for (var e : a.getRemoved()) {
						System.err.println("Edge removed: " + e.getId());
					}
				}
			});
		}

		PaneInteraction.setup(view, allowResize);
		NodeInteraction.setup(view, controller.getResizeModeCheckMenuItem().selectedProperty(), () -> controller.getSelectButton().fire());
		EdgeInteraction.setup(view, controller.getResizeModeCheckMenuItem().selectedProperty(), () -> controller.getSelectButton().fire());

		controller.getLabelEdgeByWeightsMenuItem().selectedProperty().bindBidirectional(view.showWeightProperty());
		controller.getLabelEdgeByConfidenceMenuItem().selectedProperty().bindBidirectional(view.showConfidenceProperty());
		controller.getLabelEdgeByProbabilityMenuItem().selectedProperty().bindBidirectional(view.showProbabilityProperty());

		controller.getLabelEdgeByWeightsMenuItem().selectedProperty().addListener(e ->
				undoManager.doAndAdd(new ShowEdgeValueCommand(view, view.isShowWeight(), view.isShowConfidence(), view.isShowProbability())));
		controller.getLabelEdgeByWeightsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		controller.getLabelEdgeByConfidenceMenuItem().selectedProperty().addListener(e ->
				undoManager.doAndAdd(new ShowEdgeValueCommand(view, view.isShowWeight(), view.isShowConfidence(), view.isShowProbability())));
		controller.getLabelEdgeByConfidenceMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		controller.getLabelEdgeByProbabilityMenuItem().selectedProperty().addListener(e ->
				undoManager.doAndAdd(new ShowEdgeValueCommand(view, view.isShowWeight(), view.isShowConfidence(), view.isShowProbability())));
		controller.getLabelEdgeByProbabilityMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		view.getUndoManager().undoStackSizeProperty().addListener((v, o, n) -> {
			window.dirtyProperty().set(n.intValue() > 0);
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

		controller.getAboutMenuItem().setOnAction(e -> SplashScreen.showSplash(Duration.ofMinutes(2)));

		controller.getNewMenuItem().setOnAction(e -> NewWindow.apply());
		controller.getOpenMenuItem().setOnAction(FileOpenManager.createOpenFileEventHandler(window.getStage()));

		ChangeListener<DrawPane.Mode> listener = (v, o, n) -> {
			if (n == DrawPane.Mode.Edit) {
				controller.getEditModeToggleButton().setSelected(true);
				MaterialIcons.setIcon(controller.getEditModeToggleButton(), MaterialIcons.edit);
				controller.getEditModeCheckMenuItem().setSelected(true);
				controller.getMoveModeCheckMenuItem().setSelected(false);
				controller.getEditModeToggleButton().setTooltip(new Tooltip("Edit mode on, allows interactive creation of new nodes and edges"));
			} else if (n == DrawPane.Mode.Move) {
				controller.getEditModeToggleButton().setSelected(true);
				MaterialIcons.setIcon(controller.getEditModeToggleButton(), MaterialIcons.transform);
				controller.getEditModeCheckMenuItem().setSelected(false);
				controller.getMoveModeCheckMenuItem().setSelected(true);
				controller.getEditModeToggleButton().setTooltip(new Tooltip("Transform mode on, allows interactive relocation of nodes and reshaping of edges"));
			} else {
				controller.getEditModeToggleButton().setSelected(false);
				MaterialIcons.setIcon(controller.getEditModeToggleButton(), MaterialIcons.lock);
				controller.getEditModeCheckMenuItem().setSelected(false);
				controller.getMoveModeCheckMenuItem().setSelected(false);
				controller.getEditModeToggleButton().setTooltip(new Tooltip("Edit mode off, press to allow editing"));
			}
		};

		view.modeProperty().addListener(listener);

		listener.changed(view.modeProperty(), null, view.getMode());

		controller.getEditModeToggleButton().setOnAction(e -> {
			if (view.getMode() == DrawPane.Mode.Edit)
				view.setMode(DrawPane.Mode.Move);
			else if (view.getMode() == DrawPane.Mode.Move)
				view.setMode(DrawPane.Mode.View);
			else view.setMode(DrawPane.Mode.Edit);
		});

		controller.getEditModeCheckMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				view.setMode(DrawPane.Mode.Edit);
			else view.setMode(DrawPane.Mode.View);
		});

		controller.getMoveModeCheckMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				view.setMode(DrawPane.Mode.Move);
			else view.setMode(DrawPane.Mode.View);
		});

		view.getNodesGroup().getChildren().addListener((ListChangeListener<? super Node>) e -> {
			while (e.next()) {
				for (var node : e.getAddedSubList()) {
					if (node instanceof Shape shape && shape.getUserData() instanceof jloda.graph.Node v) {
						shape.setOnContextMenuRequested(cm -> {
							var setLabel = new MenuItem("Edit Label");
							setLabel.setOnAction(x -> NodeLabelDialog.apply(view, cm.getScreenX(), cm.getScreenY(), v, null));
							new ContextMenu(setLabel).show(node, cm.getScreenX(), cm.getScreenY());
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
							if (false || PhyloSketch.isDesktop())
								setLabel.setOnAction(x -> NodeLabelDialog.apply(window.getStage(), view, v));
							else
								setLabel.setOnAction(x -> NodeLabelDialog.apply(view, cm.getScreenX(), cm.getScreenY(), v, null));
							new ContextMenu(setLabel).show(node, cm.getScreenX(), cm.getScreenY());
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

		controller.getPageSetupMenuItem().setOnAction(e -> Print.showPageLayout(window.getStage()));
		controller.getPrintMenuItem().setOnAction((e) -> Print.print(window.getStage(), window.getDrawPane()));
		controller.getPrintMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getUndoMenuItem().setOnAction(e -> view.getUndoManager().undo());
		controller.getUndoMenuItem().disableProperty().bind(view.getUndoManager().undoableProperty().not());
		controller.getRedoMenuItem().setOnAction(e -> view.getUndoManager().redo());
		controller.getRedoMenuItem().disableProperty().bind(view.getUndoManager().redoableProperty().not());

		controller.getDeleteMenuItem().setOnAction(unused -> {
			view.getUndoManager().doAndAdd(new DeleteCommand(view,
					view.getNodeSelection().getSelectedItems(),
					view.getEdgeSelection().getSelectedItems()));
		});
		controller.getDeleteMenuItem().disableProperty().bind((view.getNodeSelection().sizeProperty().isEqualTo(0)
				.and(view.getEdgeSelection().sizeProperty().isEqualTo(0))).or(view.modeProperty().isNotEqualTo(DrawPane.Mode.Edit)));

		controller.getClearMenuItem().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new DeleteCommand(view, view.getGraph().getNodesAsList(),
					Collections.emptyList()));
		});
		controller.getClearMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(view.modeProperty().isNotEqualTo(DrawPane.Mode.Edit)));

		controller.getZoomInMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.1, 1.1);
		});
		controller.getZoomOutMenuItem().setOnAction(e -> {
			controller.getScrollPane().zoomBy(1.0 / 1.1, 1.1);
		});

		controller.getArrowsMenuItem().setSelected(view.isShowArrows());

		controller.getArrowsMenuItem().selectedProperty().addListener((v, o, n) -> {
			view.showArrowsProperty().set(n);
			undoManager.doAndAdd(new ShowArrowsCommand(view, controller.getArrowsMenuItem().isSelected()));
		});

		controller.getOutlineEdgesMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				view.setMode(DrawPane.Mode.View);
			view.showOutlinesProperty().set(n);
		});
		controller.getOutlineEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		view.modeProperty().addListener((v, o, n) -> {
			if (n != DrawPane.Mode.View && view.isShowOutlines())
				controller.getOutlineEdgesMenuItem().setSelected(false);
		});
		MainWindowManager.useDarkThemeProperty().addListener(e -> controller.getOutlineEdgesMenuItem().setSelected(false));

		controller.getCopyMenuItem().setOnAction(e -> {
			ClipboardUtils.putString(view.toBracketString());
		});
		controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportImageMenuItem().setOnAction(e -> {
			ExportImageDialog.show(window.getFileName(), window.getStage(), window.getDrawPane());
		});
		controller.getExportImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportNewickMenuItem().setOnAction(e -> {
			ExportNewick.apply(window);
		});
		controller.getExportNewickMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getCopyImageMenuItem().setOnAction(e -> {
			var params = new SnapshotParameters();
			params.setFill(MainWindowManager.isUseDarkTheme() ? Color.BLACK : Color.WHITE);
			params.setTransform(javafx.scene.transform.Transform.scale(2.0, 2.0));
			var writableImage = new WritableImage((int) (view.getWidth() * 2), (int) (view.getHeight() * 2));
			var snapshot = view.snapshot(params, writableImage);
			ClipboardUtils.putImage(snapshot);
		});
		controller.getCopyImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e ->
				RunAfterAWhile.applyInFXThread(allowRemoveSuperfluous, () -> {
					for (var v : view.getNodeSelection().getSelectedItems()) {
						if (v.getInDegree() == 1 && v.getOutDegree() == 1 && view.getLabel(v).getRawText().isBlank()) {
							allowRemoveSuperfluous.set(true);
							return;
						}
					}
					allowRemoveSuperfluous.set(false);
				})
		);

		controller.getRemoveThruNodesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new RemoveThruNodesCommand(view)));
		controller.getRemoveThruNodesMenuItem().disableProperty().bind(allowRemoveSuperfluous.not());

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

		controller.getSmoothMenuItem().setOnAction(a -> view.getUndoManager().doAndAdd(new SmoothCommand(view.getGraph(), view.getSelectedOrAllEdges())));
		controller.getSmoothMenuItem().disableProperty().bind(Bindings.isEmpty(view.getGraphFX().getEdgeList()));

		controller.getStraightMenuItem().setOnAction(a -> view.getUndoManager().doAndAdd(new StraightenCommand(view.getGraph(), view.getSelectedOrAllEdges())));
		controller.getStraightMenuItem().disableProperty().bind(controller.getSmoothMenuItem().disableProperty());

		controller.getRerootMenuItem().setOnAction(a -> {
			var e = (view.getEdgeSelection().size() == 1 ? view.getEdgeSelection().getSelectedItem() : null);
			var v = (e == null && view.getNodeSelection().size() == 1 ? view.getNodeSelection().getSelectedItem() : null);
			if (e != null || v != null)
				view.getUndoManager().doAndAdd(new RerootCommand(view, v, e));
		});
		controller.getRerootMenuItem().disableProperty().bind(allowReRoot.not());

		controller.getRectangularMenuItem().setOnAction(a -> view.getUndoManager().doAndAdd(new RectangularCommand(view.getGraph(), view.getSelectedOrAllEdges())));
		controller.getRectangularMenuItem().disableProperty().bind(controller.getSmoothMenuItem().disableProperty());

		controller.getQuadraticCurveMenuItem().setOnAction(a -> view.getUndoManager().doAndAdd(new QuadraticCurveCommand(view.getGraph(), view.getSelectedOrAllEdges())));
		controller.getQuadraticCurveMenuItem().disableProperty().bind(controller.getSmoothMenuItem().disableProperty());

		view.getGraphFX().lastUpdateProperty().addListener(e -> {
			window.getStatusPane().getChildren().removeAll(BasicFX.findRecursively(window.getStatusPane(), n -> "info".equals(n.getUserData())));
			try (var componentMap = view.getGraph().newNodeIntArray()) {
				var components = view.getGraph().computeConnectedComponents(componentMap);
				var roots = view.getGraph().nodeStream().filter(v -> v.getInDegree() == 0).count();
				for (var str : StringUtils.toList(("comps=" + components + " roots=" + roots + " " + RootedNetworkProperties.computeInfoString(view.getGraph())).replaceAll(" ", "\n"))) {
					var text = new Text(str);
					text.setUserData("info");
					text.getStyleClass().add("rich-text-label");
					window.getStatusPane().getChildren().add(text);
				}
			}
		});

		var waitObject = new Object();
		InvalidationListener selectionInvalidationListener = e -> {
			RunAfterAWhile.applyInFXThread(waitObject, () -> {
				var toRemove = window.getStatusPane().getChildren().stream().filter(c -> c instanceof Text).map(c -> (Text) c)
						.filter(t -> t.getText().contains("selected")).toList();
				window.getStatusPane().getChildren().removeAll(toRemove);
				if (view.getNodeSelection().size() > 0) {
					var nodeText = window.getStatusPane().getChildren().stream().filter(c -> c instanceof Text).map(c -> (Text) c)
							.filter(t -> t.getText().startsWith("nodes=")).findAny();
					if (nodeText.isPresent()) {
						var index = window.getStatusPane().getChildren().indexOf(nodeText.get());
						var text = new Text("(" + view.getNodeSelection().size() + " selected)");
						text.setUserData("info");
						text.getStyleClass().add("rich-text-label");
						window.getStatusPane().getChildren().add(index + 1, text);
					}
				}
				if (view.getEdgeSelection().size() > 0) {
					var edgeText = window.getStatusPane().getChildren().stream().filter(c -> c instanceof Text).map(c -> (Text) c)
							.filter(t -> t.getText().startsWith("edges=")).findAny();
					if (edgeText.isPresent()) {
						var index = window.getStatusPane().getChildren().indexOf(edgeText.get());
						var text = new Text("(" + view.getEdgeSelection().size() + " selected)");
						text.setUserData("info");
						text.getStyleClass().add("rich-text-label");
						window.getStatusPane().getChildren().add(index + 1, text);
					}
				}

				if (view.getNodeSelection().size() == 1) {
					var v = view.getNodeSelection().getSelectedItem();
					while (true) {
						if (v.getInDegree() == 1)
							v = v.getParent();
						else {
							allowReRoot.set(v.getInDegree() == 0);
							return;
						}
					}
				} else if (view.getEdgeSelection().size() == 1) {
					var f = view.getEdgeSelection().getSelectedItem();
					if (f.nodes().containsAll(view.getNodeSelection().getSelectedItems())) {
						var v = f.getTarget();

						while (true) {
							if (v.getInDegree() == 1)
								v = v.getParent();
							else {
								allowReRoot.set(v.getInDegree() == 0);
								return;
							}
						}
					}
				} else allowReRoot.set(false);
			});
		};
		view.getNodeSelection().getSelectedItems().addListener(selectionInvalidationListener);
		view.getEdgeSelection().getSelectedItems().addListener(selectionInvalidationListener);

		controller.getResizeModeCheckMenuItem().selectedProperty().bindBidirectional(allowResize);
		allowResize.addListener((v, o, n) -> {
			if (n && view.getNodeSelection().size() == 0) {
				if (view.getEdgeSelection().size() > 0) {
					for (var e : view.getEdgeSelection().getSelectedItems()) {
						view.getNodeSelection().select(e.getSource());
						view.getNodeSelection().select(e.getTarget());
					}
				} else {
					view.getNodeSelection().selectAll(view.getGraph().getNodesAsList());
					view.getEdgeSelection().selectAll(view.getGraph().getEdgesAsList());
				}
			}
		});

		controller.getRotateLeftMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new RotateCommand(view, false));
		});
		controller.getRotateLeftMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		controller.getRotateRightMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new RotateCommand(view, true));
		});
		controller.getRotateRightMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getFlipHorizontalMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new FlipCommand(view, true));
		});
		controller.getFlipHorizontalMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		controller.getFlipVerticalMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new FlipCommand(view, false));
		});
		controller.getFlipVerticalMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getEdgeWeightTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeWeightTextField().getText())) {
				var value = NumberUtils.parseDouble(controller.getEdgeWeightTextField().getText());
				view.getUndoManager().doAndAdd(new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, value));
			}
		});
		setupTriggerOnEnter(controller.getEdgeWeightTextField());
		controller.getEdgeWeightTextField().disableProperty().bind(Bindings.isEmpty(view.getEdgeSelection().getSelectedItems()));

		controller.getMeasureWeightsButton().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, -1));
			controller.getLabelEdgeByWeightsMenuItem().setSelected(true);
		});
		controller.getMeasureWeightsButton().disableProperty().bind(controller.getEdgeWeightTextField().disableProperty());

		controller.getEdgeConfidenceTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeConfidenceTextField().getText())) {
				var value = NumberUtils.parseDouble(controller.getEdgeConfidenceTextField().getText());
				view.getUndoManager().doAndAdd(new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Confidence, value));
			}
			controller.getLabelEdgeByConfidenceMenuItem().setSelected(true);
		});
		setupTriggerOnEnter(controller.getEdgeConfidenceTextField());
		controller.getEdgeConfidenceTextField().disableProperty().bind(controller.getEdgeWeightTextField().disableProperty());

		controller.getEdgeProbabilityTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeProbabilityTextField().getText())) {
				var value = NumberUtils.parseDouble(controller.getEdgeProbabilityTextField().getText());
				view.getUndoManager().doAndAdd(new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Probability, value));
			}
			controller.getLabelEdgeByProbabilityMenuItem().setSelected(true);
		});
		setupTriggerOnEnter(controller.getEdgeProbabilityTextField());
		controller.getEdgeProbabilityTextField().disableProperty().bind(controller.getEdgeWeightTextField().disableProperty());

		controller.getCheckForUpdatesMenuItem().setOnAction(e -> CheckForUpdate.apply("https://software-ab.cs.uni-tuebingen.de/download/phylosketch2"));
		CheckForUpdate.setupDisableProperty(controller.getCheckForUpdatesMenuItem().disableProperty());

		SwipeUtils.setConsumeSwipes(controller.getRootPane());

		SetupSelection.apply(view, controller);
		SetupResize.apply(view, allowResize);

		SetupFormatting.apply(view, controller);

		var qrImageView = new SimpleObjectProperty<ImageView>();

		var updateProperty = new SimpleLongProperty(System.currentTimeMillis());
		view.getGraphFX().lastUpdateProperty().addListener(e -> updateProperty.set(System.currentTimeMillis()));
		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e -> updateProperty.set(System.currentTimeMillis()));

		QRViewUtils.setup(controller.getCenterAnchorPane(), updateProperty, () -> view.toBracketString(4296),
				qrImageView, controller.getShowQRCode().selectedProperty());
		controller.getShowQRCode().disableProperty().bind(view.getGraphFX().emptyProperty());

		NewickPane.setup(controller.getCenterAnchorPane(), updateProperty, () -> view.toBracketString(4296), controller.getShowNewick().selectedProperty());
		controller.getShowNewick().disableProperty().bind(view.getGraphFX().emptyProperty());

		var backgroundImage = new BackgroundImagePane(view.getUndoManager());
		var showImageProperty = backgroundImage.showProperty();
		showImageProperty.addListener((v, o, n) -> {
			if (n)
				view.getBackgroundGroup().getChildren().add(backgroundImage);
			else
				view.getBackgroundGroup().getChildren().remove(backgroundImage);
		});


		ImportButtonUtils.setup(controller.getPasteMenuItem(), controller.getImportButton(), s -> {
			var pasteCommand = new PasteCommand(view, s);
			if (pasteCommand.isRedoable()) {
				view.getUndoManager().doAndAdd(pasteCommand);
				allowResize.set(true);
			}
		}, image -> {
			backgroundImage.getImageView().setImage(image);
			showImageProperty.set(true);
		});
		controller.getImportButton().disableProperty().bind(view.modeProperty().isNotEqualTo(DrawPane.Mode.Edit));


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

	public static void setupTriggerOnEnter(TextField textField) {
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				textField.getOnAction().handle(null);
			}
		});
	}


	private Searcher<Node> setupSearcher(DrawPane view) {
		var graph = view.getGraph();

		Function<Integer, jloda.graph.Node> index2node = index -> (jloda.graph.Node) view.getNodesGroup().getChildren().get(index).getUserData();
		var nodeSelection = view.getNodeSelection();
		return new Searcher<>(view.getNodesGroup().getChildren(),
				index -> nodeSelection.isSelected(index2node.apply(index)),
				(index, s) -> {
					if (s)
						nodeSelection.select(index2node.apply(index));
					else
						nodeSelection.clearSelection(index2node.apply(index));
				}, new SimpleObjectProperty<>(SelectionMode.MULTIPLE),
				index -> graph.getLabel(index2node.apply(index)), null,
				(index, label) -> {
					var v = index2node.apply(index);
					var oldLabel = graph.getLabel(v);
					var data = new ChangeNodeLabelsCommand.Data(v.getId(), oldLabel, label);
					view.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(view, List.of(data)));
				}
				, null, null);
	}
}

/*
 * MainWindowPresenter.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.control.RichTextLabel;
import jloda.fx.control.StateToggleButton;
import jloda.fx.dialog.ExportImageDialog;
import jloda.fx.dialog.SetParameterDialog;
import jloda.fx.find.FindToolBar;
import jloda.fx.find.Searcher;
import jloda.fx.qr.QRViewUtils;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.*;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import phylosketch.capture.SetupCapturePane;
import phylosketch.commands.*;
import phylosketch.format.FormatPaneView;
import phylosketch.io.ExportNewick;
import phylosketch.io.PhyloSketchIO;
import phylosketch.io.Save;
import phylosketch.io.SaveBeforeClosingDialog;
import phylosketch.main.CheckForUpdate;
import phylosketch.main.NewWindow;
import phylosketch.main.PhyloSketch;
import phylosketch.utils.Clusters;
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
	private final FormatPaneView formatPaneView;

	private final BooleanProperty allowResize = new SimpleBooleanProperty(this, "enableResize", false);
	private final BooleanProperty allowRemoveSuperfluous = new SimpleBooleanProperty(this, "allowRemoveSuperfluous", false);
	private final BooleanProperty allowFixCrossingEdges = new SimpleBooleanProperty(this, "allowFixCrossingEdges", false);
	private final BooleanProperty allowReRoot = new SimpleBooleanProperty(this, "allowReRoot", false);

	public MainWindowPresenter(MainWindow window) {
		var controller = window.getController();
		var view = window.getDrawPane();

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

		formatPaneView = new FormatPaneView(view, controller.getShowSettingsButton().selectedProperty());
		AnchorPane.setTopAnchor(formatPaneView.getPane(), 10.0);
		AnchorPane.setRightAnchor(formatPaneView.getPane(), 20.0);
		controller.getShowSettingsButton().selectedProperty().addListener((v, o, n) -> {
			if (n)
				controller.getCenterAnchorPane().getChildren().add(formatPaneView.getPane());
			else
				controller.getCenterAnchorPane().getChildren().remove(formatPaneView.getPane());
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

		new StateToggleButton<>(List.of(DrawPane.Mode.values()), MainWindowController::getIcon, false, true, view.modeProperty(), controller.getModeMenuButton());
		controller.setupModeMenu(view.modeProperty());

		var captureImagePane = SetupCapturePane.apply(view, controller);

		ChangeListener<DrawPane.Mode> listener = (v, o, n) -> {
			if (n == DrawPane.Mode.Edit) {
				controller.getDeleteMenuItem().setOnAction(e -> {
					view.getUndoManager().doAndAdd(new DeleteCommand(view, view.getNodeSelection().getSelectedItems(), view.getEdgeSelection().getSelectedItems()));
				});
				controller.getDeleteMenuItem().disableProperty().bind((view.getNodeSelection().sizeProperty().isEqualTo(0)
						.and(view.getEdgeSelection().sizeProperty().isEqualTo(0))).or(view.modeProperty().isNotEqualTo(DrawPane.Mode.Edit)));
				controller.getModeMenuButton().setTooltip(new Tooltip("Edit mode on, allows interactive creation of new nodes and edges"));
			} else if (n == DrawPane.Mode.Move) {
				controller.getDeleteMenuItem().disableProperty().bind(new SimpleBooleanProperty(false));
				controller.getModeMenuButton().setTooltip(new Tooltip("Transform mode on, allows interactive relocation of nodes and reshaping of edges"));
			} else if (n == DrawPane.Mode.Capture) {
				controller.getDeleteMenuItem().setOnAction(e -> controller.getClearImageMenuItem().getOnAction().handle(e));
				controller.getDeleteMenuItem().disableProperty().bind(controller.getClearImageMenuItem().disableProperty());
			} else {
				controller.getDeleteMenuItem().disableProperty().bind(new SimpleBooleanProperty(false));
				controller.getModeMenuButton().setTooltip(new Tooltip("Edit mode off, press to allow editing"));
			}
		};

		view.modeProperty().addListener(listener);

		listener.changed(view.modeProperty(), null, view.getMode());

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
		controller.getUndoMenuItem().textProperty().bind(view.getUndoManager().undoNameProperty());
		controller.getUndoMenuItem().disableProperty().bind(view.getUndoManager().undoableProperty().not());
		controller.getRedoMenuItem().setOnAction(e -> view.getUndoManager().redo());
		controller.getRedoMenuItem().textProperty().bind(view.getUndoManager().redoNameProperty());
		controller.getRedoMenuItem().disableProperty().bind(view.getUndoManager().redoableProperty().not());


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
			if (view.getSelectedOrAllNodes().size() == 1) {
				ClipboardUtils.putString(view.getLabel(view.getSelectedOrAllNodes().iterator().next()).getRawText());
			} else
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
					allowRemoveSuperfluous.set(false);
					allowFixCrossingEdges.set(false);
					for (var v : view.getSelectedOrAllNodes()) {
						if (v.getInDegree() == 1 && v.getOutDegree() == 1 && view.getLabel(v).getRawText().isBlank()) {
							allowRemoveSuperfluous.set(true);
							if (allowFixCrossingEdges.get())
								return;
						}
						if (v.getInDegree() == 2 && v.getOutDegree() == 2 && view.getLabel(v).getRawText().isBlank()) {
							allowFixCrossingEdges.set(true);
							if (allowRemoveSuperfluous.get())
								return;
						}
					}
				})
		);

		controller.getRemoveThruNodesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new RemoveThruNodesCommand(view, view.getSelectedOrAllNodes())));
		controller.getRemoveThruNodesMenuItem().disableProperty().bind(allowRemoveSuperfluous.not());

		controller.getFixCrossingEdgesMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(FixCrossingEdgesCommand.create(view, view.getSelectedOrAllNodes())));
		controller.getFixCrossingEdgesMenuItem().disableProperty().bind(allowFixCrossingEdges.not());

		controller.getLabelLeavesABCMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "leaves", "ABC", true)));
		controller.getLabelLeavesABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeaves123MenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "leaves", "t1t2t3", true)));
		controller.getLabelLeaves123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeavesMenuItem().setOnAction(c -> LabelLeaves.labelLeaves(window.getStage(), view));
		controller.getLabelLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternalABCMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "internal", "ABC", true)));
		controller.getLabelInternalABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternal123MenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "internal", "t1t2t3", true)));
		controller.getLabelInternal123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getClearLabelsMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "all", "none", true)));
		controller.getClearLabelsMenuItem().disableProperty().bind(Bindings.isEmpty(view.getNodeLabelsGroup().getChildren()));

		controller.getUseDarkThemeCheckMenuItem().selectedProperty().bindBidirectional(MainWindowManager.useDarkThemeProperty());
		BasicFX.setupFullScreenMenuSupport(window.getStage(), controller.getFullScreenMenuItem());

		controller.getRerootMenuItem().setOnAction(a -> {
			var e = (view.getEdgeSelection().size() == 1 ? view.getEdgeSelection().getSelectedItem() : null);
			var v = (e == null && view.getNodeSelection().size() == 1 ? view.getNodeSelection().getSelectedItem() : null);
			if (e != null || v != null)
				view.getUndoManager().doAndAdd(new RerootCommand(view, v, e));
		});
		controller.getRerootMenuItem().disableProperty().bind(allowReRoot.not());

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

		controller.getLayoutLabelMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new LayoutLabelsCommand(view, view.getSelectedOrAllNodes())));
		controller.getLayoutLabelMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

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

		controller.getCheckForUpdatesMenuItem().setOnAction(e -> CheckForUpdate.apply());
		controller.getCheckForUpdatesMenuItem().disableProperty().bind(MainWindowManager.getInstance().sizeProperty().greaterThan(1).or(window.dirtyProperty()));

		SwipeUtils.setConsumeSwipes(controller.getRootPane());

		SetupSelection.apply(view, controller);
		SetupResize.apply(view, allowResize);

		var qrImageView = new SimpleObjectProperty<ImageView>();

		var updateProperty = new SimpleLongProperty(System.currentTimeMillis());
		view.getGraphFX().lastUpdateProperty().addListener(e -> updateProperty.set(System.currentTimeMillis()));
		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e -> updateProperty.set(System.currentTimeMillis()));

		QRViewUtils.setup(controller.getCenterAnchorPane(), updateProperty, () -> view.toBracketString(4296),
				qrImageView, controller.getShowQRCode().selectedProperty());
		controller.getShowQRCode().disableProperty().bind(view.getGraphFX().emptyProperty());

		NewickPane.setup(controller.getCenterAnchorPane(), updateProperty, () -> view.toBracketString(4296), controller.getShowNewick().selectedProperty());
		controller.getShowNewick().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getSetWindowSizeMenuItem().setOnAction(e -> {
			var result = SetParameterDialog.apply(window.getStage(), "Enter size (width x height)",
					"%.0f x %.0f".formatted(window.getStage().getWidth(), window.getStage().getHeight()));

			if (result != null) {
				var tokens = StringUtils.split(result, 'x');
				if (tokens.length == 2 && NumberUtils.isInteger(tokens[0]) && NumberUtils.isInteger(tokens[1])) {
					var width = Math.max(50, NumberUtils.parseDouble(tokens[0]));
					var height = Math.max(50, NumberUtils.parseDouble(tokens[1]));
					window.getStage().setWidth(width);
					window.getStage().setHeight(height);
				}
			}
		});

		if (PhyloSketch.isDesktop())
			SetupHelpWindow.apply(window, controller.getShowHelpWindow());

		ImportButtonUtils.setup(controller.getPasteMenuItem(), controller.getImportButton(),
				s -> {
					var pasteCommand = new PasteCommand(view, s);
					if (pasteCommand.isRedoable()) {
						view.getUndoManager().doAndAdd(pasteCommand);
						allowResize.set(true);
					}
				},
				image -> {
					var pasteCommand = UndoableRedoableCommand.create("image", () -> {
						captureImagePane.getImageView().setImage(null);
						view.setMode(DrawPane.Mode.Edit);
					}, () -> {
						view.setMode(DrawPane.Mode.Capture);
						captureImagePane.getImageView().setImage(image);
					});
					view.getUndoManager().doAndAdd(pasteCommand);
				});
		controller.getImportButton().disableProperty().bind(view.modeProperty().isNotEqualTo(DrawPane.Mode.Edit).and(view.modeProperty().isNotEqualTo(DrawPane.Mode.Capture)));

		controller.getFindAgainMenuItem().setOnAction(e -> {
			if (false)
				Clusters.show(view.getGraph());
			if (findToolBar.isShowFindToolBar())
				findToolBar.findAgain();
		});

		var factor = new SimpleDoubleProperty(1.0);

		controller.getIncreaseFontSizeMenuItem().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.size, null, Double.MAX_VALUE, null));
		});
		controller.getIncreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(factor.greaterThanOrEqualTo(10.0)));

		controller.getDecreaseFontSizeMenuItem().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.size, null, Double.MIN_VALUE, null));
		});
		controller.getDecreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(factor.lessThanOrEqualTo(0.1)));

		controller.getAddLSAEdgeMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new AddLSAEdgesCommand(view)));
		controller.getAddLSAEdgeMenuItem().disableProperty().bind(Bindings.createBooleanBinding(() -> view.getGraph().nodeStream().filter(v -> v.getInDegree() == 0).count() != 1, view.getGraphFX().lastUpdateProperty()));
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

	public FormatPaneView getFormatPaneView() {
		return formatPaneView;
	}
}

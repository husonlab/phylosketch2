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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.fx.control.RichTextLabel;
import jloda.fx.control.StateToggleButton;
import jloda.fx.dialog.ExportImageDialog;
import jloda.fx.dialog.SetParameterDialog;
import jloda.fx.find.FindToolBar;
import jloda.fx.find.Searcher;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
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
import phylosketch.capturepane.pane.CapturePane;
import phylosketch.capturepane.pane.SetupCaptureMenuItems;
import phylosketch.commands.*;
import phylosketch.format.FormatPaneController;
import phylosketch.format.FormatPaneView;
import phylosketch.io.*;
import phylosketch.main.CheckForUpdate;
import phylosketch.main.NewWindow;
import phylosketch.main.PhyloSketch;
import phylosketch.utils.Clusters;
import phylosketch.utils.GraphUtils;
import phylosketch.view.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * setup all control bindings
 * Daniel Huson, 9.2024
 */
public class MainWindowPresenter {
	private final MainWindow window;
	private final FindToolBar findToolBar;
	private final FormatPaneView formatPaneView;
	private final CapturePane capturePane;
	private final MainWindowController controller;
	private final DrawView view;


	private final BooleanProperty allowResize = new SimpleBooleanProperty(this, "enableResize", false);

	public MainWindowPresenter(MainWindow window) {
		this.window = window;
		view = window.getDrawView();
		controller = window.getController();

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
		controller.getCenterAnchorPane().getChildren().add(formatPaneView.getPane());
		formatPaneView.getPane().setVisible(false);
		controller.getShowSettingsButton().selectedProperty().bindBidirectional(formatPaneView.getPane().visibleProperty());


		{
			var object = new Object();
			view.getGraphFX().lastUpdateProperty().addListener(e -> {
				RunAfterAWhile.applyInFXThread(object, () -> GraphUtils.updateReticulateEdges(view.getGraph()));
			});
		}

		var dragLineBoxSupport = DragLineBoxSupport.setup(view);

		PaneInteraction.setup(view, controller, dragLineBoxSupport, allowResize);
		NodeInteraction.setup(view, controller.getResizeModeCheckMenuItem().selectedProperty(), dragLineBoxSupport, () -> controller.getExtendSelectionMenuItem().fire());
		EdgeInteraction.setup(view, controller.getResizeModeCheckMenuItem().selectedProperty(), () -> controller.getExtendSelectionMenuItem().fire());

		ModificationSupport.setup(view, controller);

		view.getUndoManager().undoStackSizeProperty().addListener((v, o, n) -> window.dirtyProperty().set(n.intValue() > 0));

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

		controller.getAboutMenuItem().setOnAction(e -> SplashScreen.showSplash(Duration.ofSeconds(30)));

		controller.getNewMenuItem().setOnAction(e -> NewWindow.apply());
		controller.getOpenMenuItem().setOnAction(FileOpenManager.createOpenFileEventHandler(window.getStage()));

		new StateToggleButton<>(List.of(DrawView.Mode.values()), MainWindowController::getIcon, true, true, view.modeProperty(), controller.getModeMenuButton());
		controller.getModeMenuButton().setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		controller.setupModeMenu(view.modeProperty());

		capturePane = new CapturePane(view, controller);
		SetupCaptureMenuItems.apply(window, controller, capturePane);

		ChangeListener<DrawView.Mode> listener = (v, o, n) -> {
			if (n == DrawView.Mode.Sketch) {
				controller.getDeleteMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new DeleteCommand(view, view.getNodeSelection().getSelectedItems(), view.getEdgeSelection().getSelectedItems())));
				controller.getDeleteMenuItem().disableProperty().bind((view.getNodeSelection().sizeProperty().isEqualTo(0)
						.and(view.getEdgeSelection().sizeProperty().isEqualTo(0))).or(view.modeProperty().isNotEqualTo(DrawView.Mode.Sketch)));
				controller.getModeMenuButton().setTooltip(new Tooltip("Sketch mode on, allows interactive editing of new nodes and edges"));
			} else if (n == DrawView.Mode.Move) {
				controller.getDeleteMenuItem().disableProperty().bind(new SimpleBooleanProperty(false));
				controller.getModeMenuButton().setTooltip(new Tooltip("Move mode on, allows interactive relocation of nodes and reshaping of edges"));
			} else if (n == DrawView.Mode.Capture) {
				controller.getModeMenuButton().setTooltip(new Tooltip("Capture mode on, allows capture of phylogeny from image"));
			} else {
				controller.getModeMenuButton().setTooltip(new Tooltip("View mode on, view network without editing nodes or edges"));
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
							setLabel.setOnAction(x -> NodeLabelEditBox.show(view, cm.getScreenX(), cm.getScreenY(), v, null, null));
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
							if (view.getMode() == DrawView.Mode.Sketch || view.getMode() == DrawView.Mode.Move) {
								var v = view.getGraph().findNodeById(nodeId);
								var editLabelItem = new MenuItem("Edit Label");
								editLabelItem.setOnAction(x -> NodeLabelEditBox.show(view, cm.getScreenX(), cm.getScreenY(), v, null, null));
								var contextMenu = new ContextMenu(editLabelItem);
								contextMenu.show(node, cm.getScreenX(), cm.getScreenY());
							}
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
					PhyloSketchIO.save(w, view, window.getPresenter().getCapturePane().getImageView());
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
		controller.getPrintMenuItem().setOnAction((e) -> Print.print(window.getStage(), window.getDrawView()));
		controller.getPrintMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getUndoMenuItem().setOnAction(e -> view.getUndoManager().undo());
		controller.getUndoMenuItem().textProperty().bind(view.getUndoManager().undoNameProperty());
		controller.getUndoMenuItem().disableProperty().bind(view.getUndoManager().undoableProperty().not());
		controller.getRedoMenuItem().setOnAction(e -> view.getUndoManager().redo());
		controller.getRedoMenuItem().textProperty().bind(view.getUndoManager().redoNameProperty());
		controller.getRedoMenuItem().disableProperty().bind(view.getUndoManager().redoableProperty().not());


		controller.getClearMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new DeleteCommand(view, view.getGraph().getNodesAsList(),
				Collections.emptyList())));
		controller.getClearMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(view.modeProperty().isNotEqualTo(DrawView.Mode.Sketch)));

		controller.getZoomInMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1.1, 1.1));
		controller.getZoomInMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().and(capturePane.hasImageProperty().not()));
		controller.getZoomOutMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1.0 / 1.1, 1.1));
		controller.getZoomOutMenuItem().disableProperty().bind(controller.getZoomInMenuItem().disableProperty());
		controller.getZoomToFitMenuItem().setOnAction(e -> ZoomToFit.apply(window));
		controller.getZoomToFitMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getOutlineEdgesMenuItem().selectedProperty().addListener((v, o, n) -> {
			if (n)
				view.setMode(DrawView.Mode.View);
			view.showOutlinesProperty().set(n);
		});
		controller.getOutlineEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
		view.modeProperty().addListener((v, o, n) -> {
			if (n != DrawView.Mode.View && view.isShowOutlines())
				controller.getOutlineEdgesMenuItem().setSelected(false);
		});
		MainWindowManager.useDarkThemeProperty().addListener(e -> controller.getOutlineEdgesMenuItem().setSelected(false));

		controller.getCopyMenuItem().setOnAction(e -> {
			if (view.getSelectedOrAllNodes().size() == 1) {
				ClipboardUtils.putString(DrawView.getLabel(view.getSelectedOrAllNodes().iterator().next()).getRawText());
			} else
				ClipboardUtils.putString(NewickUtils.toBracketString(view));
		});
		controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportImageMenuItem().setOnAction(e -> ExportImageDialog.show(window.getFileName(), window.getStage(), window.getDrawView()));
		controller.getExportImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getExportNewickMenuItem().setOnAction(e -> ExportNewick.apply(window));
		controller.getExportNewickMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getCopyImageMenuItem().setOnAction(e -> {
			var params = new SnapshotParameters();
			params.setFill(MainWindowManager.isUseDarkTheme() ? Color.BLACK : Color.WHITE);
			params.setTransform(javafx.scene.transform.Transform.scale(2.0, 2.0));
			var writableImage = new WritableImage((int) (view.getWidth() * 2), (int) (view.getHeight() * 2));
			var snapshot = view.snapshot(params, writableImage);
			ClipboardUtils.putImage(snapshot);
		});
		controller.getCopyImageMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().and(capturePane.hasImageProperty().not()));

		controller.getLabelLeavesABCMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "ABC", "leaves", true)));
		controller.getLabelLeavesABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeaves123MenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "t1t2t3", "leaves", true)));
		controller.getLabelLeaves123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelLeavesMenuItem().setOnAction(c -> LabelLeaves.labelLeaves(window.getStage(), view));
		controller.getLabelLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternalABCMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "ABC", "internal", true)));
		controller.getLabelInternalABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getLabelInternal123MenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "t1t2t3", "internal", true)));
		controller.getLabelInternal123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		controller.getClearLabelsMenuItem().setOnAction(c -> view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, "none", "all", true)));
		controller.getClearLabelsMenuItem().disableProperty().bind(Bindings.isEmpty(view.getNodeLabelsGroup().getChildren()));

		controller.getUseDarkThemeCheckMenuItem().selectedProperty().bindBidirectional(MainWindowManager.useDarkThemeProperty());
		BasicFX.setupFullScreenMenuSupport(window.getStage(), controller.getFullScreenMenuItem());


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
		InvalidationListener selectionInvalidationListener = e -> RunAfterAWhile.applyInFXThread(waitObject, () -> {
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
		});

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
		controller.getResizeModeCheckMenuItem().disableProperty().bind(
				(controller.getResizeModeCheckMenuItem().selectedProperty()
						.or(view.modeProperty().isEqualTo(DrawView.Mode.Sketch))
						.or(view.modeProperty().isEqualTo(DrawView.Mode.Move))
				).not().or(view.getGraphFX().emptyProperty())
		);

		controller.getLayoutLabelMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new LayoutLabelsCommand(view, null, view.getSelectedOrAllNodes())));
		controller.getLayoutLabelMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

		var isRotatingOrFlipping = new SimpleBooleanProperty(this, "isRotatingOrFlipping", false);

		controller.getRotateLeftMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new RotateCommand(view, view.getSelectedOrAllNodes(), false, isRotatingOrFlipping));
		});
		controller.getRotateLeftMenuItem().disableProperty().bind(view.modeProperty().isNotEqualTo(DrawView.Mode.Sketch).or(view.getGraphFX().emptyProperty().or(isRotatingOrFlipping)));
		controller.getRotateRightMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new RotateCommand(view, view.getSelectedOrAllNodes(), true, isRotatingOrFlipping));
		});
		controller.getRotateRightMenuItem().disableProperty().bind(controller.getRotateLeftMenuItem().disableProperty());


		controller.getFlipHorizontalMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new FlipCommand(view, view.getSelectedOrAllNodes(), true, isRotatingOrFlipping));
		});
		controller.getFlipHorizontalMenuItem().disableProperty().bind(controller.getRotateLeftMenuItem().disableProperty());
		controller.getFlipVerticalMenuItem().setOnAction(e -> {
			allowResize.set(false);
			view.getUndoManager().doAndAdd(new FlipCommand(view, view.getSelectedOrAllNodes(), false, isRotatingOrFlipping));
		});
		controller.getFlipVerticalMenuItem().disableProperty().bind(controller.getRotateLeftMenuItem().disableProperty());

		controller.getCheckForUpdatesMenuItem().setOnAction(e -> CheckForUpdate.apply());
		controller.getCheckForUpdatesMenuItem().disableProperty().bind(MainWindowManager.getInstance().sizeProperty().greaterThan(1).or(window.dirtyProperty()));

		SwipeUtils.setConsumeSwipes(controller.getRootPane());

		SetupSelection.apply(view, controller);
		SetupResize.apply(view, allowResize);

		var qrImageView = new SimpleObjectProperty<ImageView>();

		var updateProperty = new SimpleLongProperty(System.currentTimeMillis());
		view.getGraphFX().lastUpdateProperty().addListener(e -> updateProperty.set(System.currentTimeMillis()));
		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e -> updateProperty.set(System.currentTimeMillis()));

		QRViewUtils.setup(controller.getCenterAnchorPane(), updateProperty, () -> NewickUtils.toBracketString(view, 4296),
				qrImageView, controller.getShowQRCode().selectedProperty());
		controller.getShowQRCode().disableProperty().bind(view.getGraphFX().emptyProperty());

		NewickPane.setup(controller.getCenterAnchorPane(), updateProperty, () -> NewickUtils.toBracketString(view, 4296), controller.getShowNewick().selectedProperty());
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

		SetupImport.apply(view, controller.getPasteMenuItem(), (name, s) -> {
					var clean = (view.getGraph().getNumberOfNodes() == 0);
					var lines = StringUtils.getLinesFromString(s, 1);
					if (!lines.isEmpty()) {
						if (lines.get(0).startsWith("graph [")) {
							loadContent(name, s);
						} else if (lines.get(0).startsWith("(nodes [)")) {
							var pasteCommand = new PasteCommand(window, s);
							if (pasteCommand.isRedoable()) {
								view.getUndoManager().doAndAdd(pasteCommand);
								if (!clean)
									allowResize.set(true);
							}
						}
					}
				},
				image -> {
					var pasteCommand = UndoableRedoableCommand.create("image", () -> {
						capturePane.setImage(null);
						view.setMode(DrawView.Mode.Sketch);
					}, () -> {
						view.setMode(DrawView.Mode.Capture);
						capturePane.setImage(image);
					});
					view.getUndoManager().doAndAdd(pasteCommand);
				});


		if (PhyloSketch.isDesktop())
			SetupHelpWindow.apply(window, controller.getShowHelpWindow());

		controller.getLoadCaptureImageItem().setOnAction(e -> {
					if (window.isEmpty() || !PhyloSketch.isDesktop()) {
						loadImageDialog(window.getStage(), image -> {
							window.getDrawView().setMode(DrawView.Mode.Capture);
							window.getPresenter().getCapturePane().setImage(image);
						});
					} else {
						var newWindow = NewWindow.apply();
						Platform.runLater(() -> newWindow.getController().getLoadCaptureImageItem().fire());
					}
				}
		);
		controller.getLoadCaptureImageItem().disableProperty().bind(view.modeProperty().isNotEqualTo(DrawView.Mode.Sketch).and(view.modeProperty().isNotEqualTo(DrawView.Mode.Capture)));

		controller.getFindAgainMenuItem().setOnAction(e -> {
			if (false)
				Clusters.show(view.getGraph());
			if (findToolBar.isShowFindToolBar())
				findToolBar.findAgain();
		});

		var factor = new SimpleDoubleProperty(1.0);

		controller.getIncreaseFontSizeMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.size, null, Double.MAX_VALUE, null)));
		controller.getIncreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(factor.greaterThanOrEqualTo(10.0)));

		controller.getDecreaseFontSizeMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.size, null, Double.MIN_VALUE, null)));
		controller.getDecreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty().or(factor.lessThanOrEqualTo(0.1)));

		controller.getAddLSAEdgeMenuItem().setOnAction(e -> view.getUndoManager().doAndAdd(new AddLSAEdgesCommand(view)));
		controller.getAddLSAEdgeMenuItem().disableProperty().bind(Bindings.createBooleanBinding(() -> view.getGraph().nodeStream().filter(v -> v.getInDegree() == 0).count() != 1, view.getGraphFX().lastUpdateProperty()));

		formatPaneView.getController().getDeclareRootButton().setOnAction(controller.getDeclareRootMenuItem().getOnAction());
		formatPaneView.getController().getDeclareRootButton().disableProperty().bind(controller.getDeclareRootMenuItem().disableProperty());

		formatPaneView.getController().getMergeNodesButton().setOnAction(controller.getMergeNodesMenuItem().getOnAction());
		formatPaneView.getController().getMergeNodesButton().disableProperty().bind(controller.getMergeNodesMenuItem().disableProperty());

		formatPaneView.getController().getReverseEdgesButton().setOnAction(controller.getReverseEdgesMenuItem().getOnAction());
		formatPaneView.getController().getReverseEdgesButton().disableProperty().bind(controller.getReverseEdgesMenuItem().disableProperty());

		formatPaneView.getController().getDeleteThruNodesButton().setOnAction(controller.getDeleteThruNodesMenuItem().getOnAction());
		formatPaneView.getController().getDeleteThruNodesButton().disableProperty().bind(controller.getDeleteThruNodesMenuItem().disableProperty());

		formatPaneView.getController().getCrossEdgesButton().setOnAction(controller.getCrossEdgesMenuItem().getOnAction());
		formatPaneView.getController().getCrossEdgesButton().disableProperty().bind(controller.getCrossEdgesMenuItem().disableProperty());

		formatPaneView.getController().getTransferAcceptorButton().setOnAction(controller.getDeclareTransferAcceptorMenuItem().getOnAction());
		formatPaneView.getController().getTransferAcceptorButton().disableProperty().bind(controller.getDeclareTransferAcceptorMenuItem().disableProperty());

		formatPaneView.getController().getApplyModificationButton().setOnAction(controller.getApplyModificationMenuItem().getOnAction());
		formatPaneView.getController().getApplyModificationButton().disableProperty().bind(controller.getApplyModificationMenuItem().disableProperty());
		controller.getApplyModificationMenuItem().textProperty().addListener((v, o, n) -> formatPaneView.getController().getApplyModificationButton().setTooltip(new Tooltip(n)));

		formatPaneView.getController().getInduceButton().setOnAction(controller.getInduceMenuItem().getOnAction());
		formatPaneView.getController().getInduceButton().disableProperty().bind(controller.getInduceMenuItem().disableProperty());

		setupLayout(controller, formatPaneView.getController());
		setupLayoutScalingPhylogeny(view, controller, formatPaneView.getController());

		if (PhyloSketch.isDesktop()) {
			setupModeHints(view, getCapturePane());
		}
	}

	public void loadContent(String fileName, String content) {
		if (content != null && !content.isBlank()) {
			try {
				var file = (fileName != null ? new File(fileName) :
						FileUtils.getUniqueFileName(ProgramProperties.get("SaveFileDir", System.getProperty("user.dir")), "Untitled", ".psketch"));
				FileUtils.writeLinesToFile(List.of(content), file.getPath(), false);
				var thisWindow = (window.isEmpty() ? window : NewWindow.apply());
				(new FileOpener()).accept(file.getPath(), thisWindow);
				if (false) Platform.runLater(() -> ZoomToFit.apply(thisWindow));
			} catch (IOException ignored) {
			}
		}
	}

	private Searcher<Node> setupSearcher(DrawView view) {
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

	public CapturePane getCapturePane() {
		return capturePane;
	}

	public static void loadImageDialog(Stage stage, Consumer<Image> imageConsumer) {
		var fileChooser = new FileChooser();
		var dir = ProgramProperties.get("LoadImageDirectory", "");
		if (FileUtils.isDirectory(dir))
			fileChooser.setInitialDirectory(new File(dir));
		fileChooser.setTitle("Open Image File");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
		);

		// Show dialog and get file
		var file = fileChooser.showOpenDialog(stage);
		if (file != null && file.exists()) {
			var image = new Image(file.toURI().toString());
			imageConsumer.accept(image);
			if (!file.getParent().isBlank()) {
				ProgramProperties.put("LoadImageDirectory", file.getParent());
			}
		}
	}

	private static void setupLayoutScalingPhylogeny(DrawView view, MainWindowController controller, FormatPaneController formatController) {

		var layout = view.layoutProperty();
		var scaling = view.scalingProperty();

		formatController.getApplyLayoutPhylogenyButton().setOnAction(v -> {
			if (layout.get() != null && scaling.get() != null)
				view.getUndoManager().doAndAdd(new LayoutPhylogenyCommand(view, layout.get(), scaling.get()));
		});
		formatController.getApplyLayoutPhylogenyButton().disableProperty().bind(view.getGraphFX().emptyProperty().or(scaling.isNull()).or(layout.isNull()));
		controller.getLayoutPhylogenyMenuItem().setOnAction(formatController.getApplyLayoutPhylogenyButton().getOnAction());
		controller.getLayoutPhylogenyMenuItem().disableProperty().bind(formatController.getApplyLayoutPhylogenyButton().disableProperty());

		formatController.getLayoutCBox().valueProperty().bindBidirectional(layout);
		controller.getLayoutToggleGroup().selectedToggleProperty().addListener((v, o, n) -> {
			if (n != null && n.getUserData() instanceof LayoutRootedPhylogeny.Layout value)
				layout.set(value);
		});
		layout.addListener((v, o, n) -> {
			if (n == null)
				controller.getLayoutToggleGroup().selectToggle(null);
			else {
				controller.getLayoutToggleGroup().getToggles().stream().filter(t -> t.getUserData() == n).forEach(t -> t.setSelected(true));
				formatController.getApplyLayoutPhylogenyButton().fire();
			}
		});
		ProgramProperties.track(layout, LayoutRootedPhylogeny.Layout::valueOf, LayoutRootedPhylogeny.Layout.Rectangular);

		formatController.getScalingCBox().valueProperty().bindBidirectional(scaling);
		controller.getScalingToggleGroup().selectedToggleProperty().addListener((v, o, n) -> {
			if (n != null && n.getUserData() instanceof LayoutRootedPhylogeny.Scaling value)
				scaling.set(value);
		});
		scaling.addListener((v, o, n) -> {
			if (n == null)
				controller.getScalingToggleGroup().selectToggle(null);
			else {
				controller.getScalingToggleGroup().getToggles().stream().filter(t -> t.getUserData() == n).forEach(t -> t.setSelected(true));
				formatController.getApplyLayoutPhylogenyButton().fire();
			}
		});
		ProgramProperties.track(scaling, LayoutRootedPhylogeny.Scaling::valueOf, LayoutRootedPhylogeny.Scaling.LateBranching);
	}

	private static void setupLayout(MainWindowController controller, FormatPaneController formatController) {
		formatController.getRotateLeftButton().setOnAction(controller.getRotateLeftMenuItem().getOnAction());
		formatController.getRotateLeftButton().disableProperty().bind(controller.getRotateLeftMenuItem().disableProperty());
		formatController.getRotateRightButton().setOnAction(controller.getRotateRightMenuItem().getOnAction());
		formatController.getRotateRightButton().disableProperty().bind(controller.getRotateRightMenuItem().disableProperty());
		formatController.getHorizontalFlipButton().setOnAction(controller.getFlipHorizontalMenuItem().getOnAction());
		formatController.getHorizontalFlipButton().disableProperty().bind(controller.getFlipHorizontalMenuItem().disableProperty());
		formatController.getVerticalFlipButton().setOnAction(controller.getFlipVerticalMenuItem().getOnAction());
		formatController.getVerticalFlipButton().disableProperty().bind(controller.getFlipVerticalMenuItem().disableProperty());
		formatController.getResizeModeButton().selectedProperty().bindBidirectional(controller.getResizeModeCheckMenuItem().selectedProperty());
		formatController.getResizeModeButton().disableProperty().bind(controller.getResizeModeCheckMenuItem().disableProperty());
		formatController.getLayoutLabelsButton().setOnAction(controller.getLayoutLabelMenuItem().getOnAction());
		formatController.getLayoutLabelsButton().disableProperty().bind(controller.getLayoutLabelMenuItem().disableProperty());
	}

	private void setupModeHints(DrawView view, CapturePane capturePane) {
		var message = new Text();
		RunAfterAWhile.applyInFXThread(message, () -> {
			InvalidationListener updateModeMessageListener = e -> {
				RunAfterAWhile.applyInFXThread(message, () -> {
					if (view.getGraph().getNumberOfNodes() == 0 && capturePane.getImageView().getImage() == null) {
						switch (view.getMode()) {
							case Capture -> WindowNotifications.show(controller.getCenterAnchorPane(), """
									Capture mode: To capture a phylogeny from an image:
									(1) Use the Load Image item to load an image, or paste one into the window.
									(2) Use the Place Root item to show the root locator, drag it to the root.
									(3) Click on the root locator to set the relative position of the root (left, right, etc)
									(4) Use the Capture Phylogeny item to run the capture algorithm.
									(5) Set the mode to Sketch and improve the capture interactively.
									""", WindowNotifications.MessageType.INFO);
							case Move ->
									WindowNotifications.showInfo(controller.getCenterAnchorPane(), "Move mode: Click and drag on nodes, edges and labels to move them.");
							case View ->
									WindowNotifications.showInfo(controller.getCenterAnchorPane(), "View mode: View the phylogeny without editing.");
							case Sketch ->
									WindowNotifications.showInfo(controller.getCenterAnchorPane(), "Sketch mode: To creat a node, double-click on the pane. Then press-drag to create edges.");
						}
					}
				});
			};
			view.modeProperty().addListener(updateModeMessageListener);
			view.getGraphFX().getNodeList().addListener(updateModeMessageListener);
			updateModeMessageListener.invalidated(null);
			view.getUndoManager().undoStackSizeProperty().addListener(updateModeMessageListener);
		});
	}
}

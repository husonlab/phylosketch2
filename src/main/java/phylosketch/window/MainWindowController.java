/*
 * MainWindowController.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.phylo.embed.LayoutRootedPhylogeny;
import jloda.fx.util.BasicFX;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.RunAfterAWhile;
import phylosketch.view.DrawView;

public class MainWindowController {
	@FXML
	private MenuItem aboutMenuItem;

	@FXML
	private Menu advancedMenu;

	@FXML
	private FlowPane bottomFlowPane;

	@FXML
	private MenuItem setWindowSizeMenuItem;

	@FXML
	private MenuItem checkForUpdatesMenuItem;

	@FXML
	private CheckMenuItem showHelpWindow;

	@FXML
	private MenuItem clearMenuItem;

	@FXML
	private MenuItem applyModificationMenuItem;

	@FXML
	private MenuItem mergeNodesMenuItem;

	@FXML
	private MenuItem deleteThruNodesMenuItem;

	@FXML
	private MenuItem reverseEdgesMenuItem;

	@FXML
	private Menu captureMenu;

	@FXML
	private MenuItem crossEdgesMenuItem;

	@FXML
	private MenuItem closeMenuItem;

	@FXML
	private MenuItem copyExportMenuItem;

	@FXML
	private MenuItem copyImageExportMenuItem;

	@FXML
	private MenuItem exportExportMenuItem;

	@FXML
	private MenuItem copyMenuItem;

	@FXML
	private MenuItem copyImageMenuItem;

	@FXML
	private MenuItem cutMenuItem;

	@FXML
	private Button zoomOutButton;

	@FXML
	private Button zoomToFitButton;

	@FXML
	private MenuItem decreaseFontSizeMenuItem;

	@FXML
	private Menu editMenu;

	@FXML
	private Menu exportMenu;

	@FXML
	private ToggleButton showSettingsButton;

	@FXML
	private MenuButton exportMenuButton;

	@FXML
	private MenuItem exportNewickMenuItem;

	@FXML
	private MenuItem exportImageMenuItem;

	@FXML
	private Menu fileMenu;

	@FXML
	private MenuItem findAgainMenuItem;

	@FXML
	private MenuItem addLSAEdgeMenuItem;

	@FXML
	private ToggleButton findButton;

	@FXML
	private MenuItem findMenuItem;

	@FXML
	private MenuItem fullScreenMenuItem;

	@FXML
	private Button zoomInButton;

	@FXML
	private MenuItem increaseFontSizeMenuItem;

	@FXML
	private Label memoryUsageLabel;

	@FXML
	private MenuBar menuBar;

	@FXML
	private MenuItem newMenuItem;

	@FXML
	private MenuItem openMenuItem;

	@FXML
	private MenuItem pageSetupMenuItem;

	@FXML
	private MenuItem pasteMenuItem;

	@FXML
	private MenuItem deleteMenuItem;

	@FXML
	private MenuItem printMenuItem;

	@FXML
	private MenuItem quitMenuItem;

	@FXML
	private Menu recentFilesMenu;

	@FXML
	private MenuItem redoMenuItem;

	@FXML
	private AnchorPane rootPane;

	@FXML
	private MenuItem saveMenuItem;

	@FXML
	private Menu selectMenu;

	@FXML
	private MenuItem selectAllMenuItem;


	@FXML
	private MenuItem selectInvertMenuItem;

	@FXML
	private MenuItem extendSelectionMenuItem;

	@FXML
	private MenuItem selectNoneMenuItem;

	@FXML
	private MenuItem selectTreeEdgesMenuItem;

	@FXML
	private MenuItem selectReticulateEdgesMenuItem;

	@FXML
	private MenuItem selectInEdgesMenuItem;

	@FXML
	private MenuItem selectOutEdgesMenuItem;

	@FXML
	private MenuItem selectThruNodesMenuItem;

	@FXML
	private MenuItem selectRootsMenuItem;

	@FXML
	private MenuItem selectLeavesMenuItem;

	@FXML
	private MenuItem selectTreeNodesMenuItem;

	@FXML
	private MenuItem selectReticulateNodesMenuitem;

	@FXML
	private MenuItem selectVisibleNodesMenuItem;

	@FXML
	private MenuItem selectVisibleReticulationsMenuItem;

	@FXML
	private MenuItem selectStableNodesMenuItem;

	@FXML
	private MenuItem selectArticulationNodesMenuItem;

	@FXML
	private MenuItem selectAllBelowMenuItem;

	@FXML
	private MenuItem selectAllAboveMenuItem;

	@FXML
	private MenuItem selectPossibleRootLocationsMenuItem;

	@FXML
	private MenuItem selectLowestStableAncestorMenuItem;

	@FXML
	private MenuItem selectFromPreviousMenuItem;

	@FXML
	private MenuItem labelLeavesABCMenuItem;

	@FXML
	private MenuItem labelLeaves123MenuItem;

	@FXML
	private MenuItem labelLeavesMenuItem;

	@FXML
	private MenuItem labelInternalABCMenuItem;

	@FXML
	private MenuItem labelInternal123MenuItem;

	@FXML
	private MenuItem clearLabelsMenuItem;

	@FXML
	private VBox topVBox;

	@FXML
	private MenuItem undoMenuItem;

	@FXML
	private CheckMenuItem useDarkThemeCheckMenuItem;

	@FXML
	private Menu windowMenu;

	@FXML
	private MenuItem zoomInMenuItem;

	@FXML
	private MenuItem zoomOutMenuItem;

	@FXML
	private MenuItem zoomToFitMenuItem;

	@FXML
	private Button undoButton;

	@FXML
	private Button redoButton;

	@FXML
	private MenuButton selectMenuButton;

	@FXML
	private MenuButton captureMenuButton;

	@FXML
	private Button pasteButton;

	@FXML
	CheckMenuItem showCaptureParametersItem;

	@FXML
	MenuItem loadCaptureImageItem;

	@FXML
	MenuItem clearCaptureImageItem;

	@FXML
	MenuItem saveCaptureImageItem;

	@FXML
	CheckMenuItem showCaptureRootLocationItem;

	@FXML
	Menu rootSideMenu;

	@FXML
	RadioMenuItem leftRootSideMenuItem;

	@FXML
	RadioMenuItem rightRootSideMenuItem;

	@FXML
	RadioMenuItem topRootSideMenuItem;

	@FXML
	RadioMenuItem bottomRootSideMenuItem;

	@FXML
	RadioMenuItem centerRootSideMenuItem;

	@FXML
	CheckMenuItem captureLinesItem;

	@FXML
	CheckMenuItem captureLabelsItem;

	@FXML
	CheckMenuItem capturePhylogenyItem;

	@FXML
	private Menu layoutMenu;

	@FXML
	private MenuItem declareRootMenuItem;

	@FXML
	private MenuItem declareTransferAcceptorMenuItem;

	@FXML
	private MenuItem induceMenuItem;

	@FXML
	private AnchorPane bottomAnchorPane;

	@FXML
	private AnchorPane centerAnchorPane;

	@FXML
	private StackPane centerPane;

	@FXML
	private Menu modeMenu;

	@FXML
	private RadioMenuItem sketchModeItem;

	@FXML
	private RadioMenuItem moveModeItem;

	@FXML
	private RadioMenuItem viewModeItem;

	@FXML
	private RadioMenuItem captureModeItem;

	@FXML
	private CheckMenuItem resizeModeCheckMenuItem;

	@FXML
	private MenuItem layoutLabelMenuItem;

	@FXML
	private MenuItem layoutPhylogenyMenuItem;

	@FXML
	private RadioMenuItem radialLayoutMenuItem;

	@FXML
	private RadioMenuItem rectangularLayoutMenuItem;

	@FXML
	private RadioMenuItem circularLayoutMenuItem;

	@FXML
	private RadioMenuItem phylogramMenuItem;

	@FXML
	private RadioMenuItem cladogramEarlyMenuItem;

	@FXML
	private RadioMenuItem cladogramLateMenuItem;

	@FXML
	private MenuItem rotateLeftMenuItem;

	@FXML
	private MenuItem rotateRightMenuItem;

	@FXML
	private MenuItem flipHorizontalMenuItem;

	@FXML
	private MenuItem flipVerticalMenuItem;

	@FXML
	private MenuButton modeMenuButton;

	@FXML
	private CheckMenuItem showQRCode;

	@FXML
	private CheckMenuItem showNewick;

	@FXML
	private CheckMenuItem outlineEdgesMenuItem;

	@FXML
	private GridPane toolbarGrid;

	@FXML
	private HBox leftBox;

	@FXML
	private HBox rightBox;


	private final ZoomableScrollPane scrollPane = new ZoomableScrollPane(null);

	private final Rectangle selectionRectangle = new Rectangle();

	private final ToggleGroup layoutToggleGroup = new ToggleGroup();

	private final ToggleGroup scalingToggleGroup = new ToggleGroup();


	@FXML
	private void initialize() {
		modeMenuButton.setText("");
		MaterialIcons.setIcon(modeMenuButton, MaterialIcons.edit, "", false);
		MaterialIcons.setIcon(findButton, MaterialIcons.search, "", false);
		MaterialIcons.setIcon(selectMenuButton, MaterialIcons.select_all, "", false);
		MaterialIcons.setIcon(showSettingsButton, MaterialIcons.format_shapes, "", false);

		MaterialIcons.setIcon(undoButton, MaterialIcons.undo);
		MaterialIcons.setIcon(redoButton, MaterialIcons.redo);
		MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in);
		MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out);
		MaterialIcons.setIcon(zoomToFitButton, MaterialIcons.fit_screen);
		MaterialIcons.setIcon(exportMenuButton, MaterialIcons.ios_share);

		MaterialIcons.setIcon(pasteButton, MaterialIcons.file_download);

		MaterialIcons.setIcon(captureMenuButton, MaterialIcons.image);
		captureMenuButton.setStyle("-fx-background-color: transparent;");
		captureMenuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		toolbarGrid.widthProperty().addListener(e -> RunAfterAWhile.applyInFXThread(toolbarGrid, this::updateToolbarLayout));
		updateToolbarLayout();

		increaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));
		decreaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));

		// if we are running on MacOS, put the specific menu items in the right places
		if (ProgramProperties.isMacOS()) {
			getMenuBar().setUseSystemMenuBar(true);
			fileMenu.getItems().remove(getQuitMenuItem());
			// windowMenu.getItems().remove(getAboutMenuItem());
			//editMenu.getItems().remove(getPreferencesMenuItem());
		}

		captureMenu.getItems().addAll(BasicFX.copyMenu(captureMenuButton.getItems()));

		{
			zoomInButton.setOnAction(e -> zoomInMenuItem.getOnAction().handle(e));
			zoomInButton.disableProperty().bindBidirectional(zoomInMenuItem.disableProperty());
			zoomOutButton.setOnAction(e -> zoomOutMenuItem.getOnAction().handle(e));
			zoomOutButton.disableProperty().bindBidirectional(zoomOutMenuItem.disableProperty());
			zoomToFitButton.setOnAction(e -> zoomToFitMenuItem.getOnAction().handle(e));
			zoomToFitButton.disableProperty().bind(zoomToFitMenuItem.disableProperty());

			undoButton.setOnAction(e->undoMenuItem.getOnAction().handle(e));
			undoButton.disableProperty().bindBidirectional(undoMenuItem.disableProperty());
			redoButton.setOnAction(e->redoMenuItem.getOnAction().handle(e));
			redoButton.disableProperty().bindBidirectional(redoMenuItem.disableProperty());
		}

		scrollPane.setPadding(new Insets(10));
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setPannable(true);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		scrollPane.setLockAspectRatio(true);
		scrollPane.setRequireShiftOrControlToZoom(true);

		selectMenuButton.getItems().addAll(BasicFX.copyMenu(selectMenu.getItems()));

		var layoutItem = new MenuItem("Layout Phylogeny");
		layoutItem.setOnAction(e -> layoutPhylogenyMenuItem.fire());
		layoutItem.disableProperty().bind(layoutPhylogenyMenuItem.disableProperty());

		copyExportMenuItem.setOnAction(e->copyMenuItem.getOnAction().handle(e));
		copyExportMenuItem.disableProperty().bind(copyMenuItem.disableProperty());

		copyImageExportMenuItem.setOnAction(e->copyImageMenuItem.getOnAction().handle(e));
		copyImageExportMenuItem.disableProperty().bind(copyImageMenuItem.disableProperty());

		centerPane.getChildren().add(scrollPane);

		centerAnchorPane.setMinWidth(AnchorPane.USE_PREF_SIZE);
		centerAnchorPane.setMaxWidth(AnchorPane.USE_PREF_SIZE);
		centerAnchorPane.prefWidthProperty().bind(rootPane.widthProperty());

		bottomAnchorPane.setMinWidth(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.setMaxWidth(AnchorPane.USE_PREF_SIZE);

		bottomAnchorPane.setMinHeight(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.setMaxHeight(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.prefHeightProperty().bind(bottomFlowPane.heightProperty());
		bottomAnchorPane.prefWidthProperty().bind(rootPane.widthProperty());

		selectionRectangle.setStroke(Color.GREEN);
		selectionRectangle.setFill(Color.TRANSPARENT);
		selectionRectangle.getStrokeDashArray().setAll(3.0, 3.0);

		showCaptureParametersItem.setGraphic(MaterialIcons.graphic(MaterialIcons.settings));
		loadCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.file_open));
		clearCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.hide_image));
		saveCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.save_as));
		showCaptureRootLocationItem.setGraphic(MaterialIcons.graphic(MaterialIcons.rocket, "-fx-rotate: 90;"));

		rootSideMenu.setGraphic(MaterialIcons.graphic(MaterialIcons.zoom_out_map, "-fx-rotate: 45;"));

		captureLinesItem.setGraphic(MaterialIcons.graphic(MaterialIcons.water));
		captureLabelsItem.setGraphic(MaterialIcons.graphic(MaterialIcons.text_fields));
		capturePhylogenyItem.setGraphic(MaterialIcons.graphic(MaterialIcons.account_tree));

		(new ToggleGroup()).getToggles().addAll(leftRootSideMenuItem, rightRootSideMenuItem, topRootSideMenuItem, bottomRootSideMenuItem, centerRootSideMenuItem);

		layoutToggleGroup.getToggles().addAll(radialLayoutMenuItem, circularLayoutMenuItem, rectangularLayoutMenuItem);
		radialLayoutMenuItem.setUserData(LayoutRootedPhylogeny.Layout.Radial);
		circularLayoutMenuItem.setUserData(LayoutRootedPhylogeny.Layout.Circular);
		rectangularLayoutMenuItem.setUserData(LayoutRootedPhylogeny.Layout.Rectangular);

		scalingToggleGroup.getToggles().addAll(phylogramMenuItem, cladogramEarlyMenuItem, cladogramLateMenuItem);
		phylogramMenuItem.setUserData(LayoutRootedPhylogeny.Scaling.ToScale);
		cladogramEarlyMenuItem.setUserData(LayoutRootedPhylogeny.Scaling.EarlyBranching);
		cladogramLateMenuItem.setUserData(LayoutRootedPhylogeny.Scaling.LateBranching);
	}

	public void updateToolbarLayout() {
		var wrap = (toolbarGrid.getWidth() < 600);
		if (!wrap) {
			// Wide: right group in row 0, col 1
			GridPane.setRowIndex(rightBox, 0);
			GridPane.setColumnIndex(rightBox, 1);
			GridPane.setColumnSpan(rightBox, 1);

			rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
			} else {
			// Narrow: move right group to row 1 spanning both columns
			GridPane.setRowIndex(rightBox, 1);
			GridPane.setColumnIndex(rightBox, 0);
			GridPane.setColumnSpan(rightBox, 2);

			rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
		}
	}

	public static MaterialIcons getIcon(DrawView.Mode mode) {
		return switch (mode) {
			case Sketch -> MaterialIcons.edit;
			case Move -> MaterialIcons.transform;
			case Capture -> MaterialIcons.image;
			case View -> MaterialIcons.lock;
		};
	}

	public void setupModeMenu(ObjectProperty<DrawView.Mode> modeProperty) {
		var toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(sketchModeItem, moveModeItem, viewModeItem, captureModeItem);
		modeProperty.addListener((v, o, n) -> {
			switch (n) {
				case Sketch -> sketchModeItem.setSelected(true);
				case Move -> moveModeItem.setSelected(true);
				case View -> viewModeItem.setSelected(true);
				case Capture -> captureModeItem.setSelected(true);
			}
		});
		modeProperty.set(DrawView.Mode.View);
		Platform.runLater(() -> modeProperty.set(DrawView.Mode.Sketch));

	}

	private void relayout() {
	}

	public MenuItem getAboutMenuItem() {
		return aboutMenuItem;
	}

	public Menu getAdvancedMenu() {
		return advancedMenu;
	}

	public FlowPane getBottomFlowPane() {
		return bottomFlowPane;
	}

	public MenuItem getCheckForUpdatesMenuItem() {
		return checkForUpdatesMenuItem;
	}

	public MenuItem getClearMenuItem() {
		return clearMenuItem;
	}

	public MenuItem getCloseMenuItem() {
		return closeMenuItem;
	}

	public MenuItem getExportExportMenuItem() {
		return exportExportMenuItem;
	}

	public MenuItem getCopyMenuItem() {
		return copyMenuItem;
	}

	public MenuItem getCopyImageMenuItem() {
		return copyImageMenuItem;
	}

	public MenuItem getCutMenuItem() {
		return cutMenuItem;
	}

	public Button getZoomOutButton() {
		return zoomOutButton;
	}

	public MenuItem getDecreaseFontSizeMenuItem() {
		return decreaseFontSizeMenuItem;
	}

	public Menu getEditMenu() {
		return editMenu;
	}

	public Menu getExportMenu() {
		return exportMenu;
	}

	public MenuButton getExportMenuButton() {
		return exportMenuButton;
	}

	public MenuItem getExportNewickMenuItem() {
		return exportNewickMenuItem;
	}

	public MenuItem getExportImageMenuItem() {
		return exportImageMenuItem;
	}

	public Menu getFileMenu() {
		return fileMenu;
	}

	public MenuItem getFindAgainMenuItem() {
		return findAgainMenuItem;
	}

	public ToggleButton getFindButton() {
		return findButton;
	}

	public MenuItem getFindMenuItem() {
		return findMenuItem;
	}

	public MenuItem getAddLSAEdgeMenuItem() {
		return addLSAEdgeMenuItem;
	}

	public MenuItem getFullScreenMenuItem() {
		return fullScreenMenuItem;
	}


	public Button getZoomInButton() {
		return zoomInButton;
	}

	public MenuItem getIncreaseFontSizeMenuItem() {
		return increaseFontSizeMenuItem;
	}

	public Label getMemoryUsageLabel() {
		return memoryUsageLabel;
	}

	public MenuBar getMenuBar() {
		return menuBar;
	}

	public MenuItem getNewMenuItem() {
		return newMenuItem;
	}

	public MenuItem getOpenMenuItem() {
		return openMenuItem;
	}

	public MenuItem getPageSetupMenuItem() {
		return pageSetupMenuItem;
	}

	public MenuItem getPasteMenuItem() {
		return pasteMenuItem;
	}

	public MenuItem getDeleteMenuItem() {
		return deleteMenuItem;
	}

	public MenuItem getPrintMenuItem() {
		return printMenuItem;
	}

	public MenuItem getQuitMenuItem() {
		return quitMenuItem;
	}

	public Menu getRecentFilesMenu() {
		return recentFilesMenu;
	}

	public MenuItem getRedoMenuItem() {
		return redoMenuItem;
	}

	public AnchorPane getRootPane() {
		return rootPane;
	}

	public MenuItem getSaveMenuItem() {
		return saveMenuItem;
	}

	public MenuItem getSelectAllMenuItem() {
		return selectAllMenuItem;
	}

	public MenuItem getSelectFromPreviousMenuItem() {
		return selectFromPreviousMenuItem;
	}

	public Menu getSelectMenu() {
		return selectMenu;
	}

	public MenuItem getSelectNoneMenuItem() {
		return selectNoneMenuItem;
	}

	public VBox getTopVBox() {
		return topVBox;
	}

	public MenuItem getUndoMenuItem() {
		return undoMenuItem;
	}

	public CheckMenuItem getUseDarkThemeCheckMenuItem() {
		return useDarkThemeCheckMenuItem;
	}

	public Menu getWindowMenu() {
		return windowMenu;
	}

	public MenuItem getZoomInMenuItem() {
		return zoomInMenuItem;
	}

	public MenuItem getZoomOutMenuItem() {
		return zoomOutMenuItem;
	}

	public MenuItem getZoomToFitMenuItem() {
		return zoomToFitMenuItem;
	}

	public ZoomableScrollPane getScrollPane() {
		return scrollPane;
	}

	public MenuItem getLabelLeavesABCMenuItem() {
		return labelLeavesABCMenuItem;
	}

	public MenuItem getLabelLeaves123MenuItem() {
		return labelLeaves123MenuItem;
	}

	public MenuItem getLabelLeavesMenuItem() {
		return labelLeavesMenuItem;
	}

	public MenuItem getLabelInternalABCMenuItem() {
		return labelInternalABCMenuItem;
	}

	public MenuItem getLabelInternal123MenuItem() {
		return labelInternal123MenuItem;
	}

	public MenuItem getClearLabelsMenuItem() {
		return clearLabelsMenuItem;
	}

	public MenuItem getSelectTreeEdgesMenuItem() {
		return selectTreeEdgesMenuItem;
	}

	public MenuItem getSelectReticulateEdgesMenuItem() {
		return selectReticulateEdgesMenuItem;
	}

	public MenuItem getSelectInEdgesMenuItem() {
		return selectInEdgesMenuItem;
	}

	public MenuItem getSelectOutEdgesMenuItem() {
		return selectOutEdgesMenuItem;
	}

	public MenuItem getSelectThruNodesMenuItem() {
		return selectThruNodesMenuItem;
	}

	public MenuItem getSelectRootsMenuItem() {
		return selectRootsMenuItem;
	}

	public MenuItem getSelectLeavesMenuItem() {
		return selectLeavesMenuItem;
	}

	public MenuItem getSelectTreeNodesMenuItem() {
		return selectTreeNodesMenuItem;
	}

	public MenuItem getSelectReticulateNodesMenuitem() {
		return selectReticulateNodesMenuitem;
	}

	public MenuItem getSelectVisibleNodesMenuItem() {
		return selectVisibleNodesMenuItem;
	}

	public MenuItem getSelectVisibleReticulationsMenuItem() {
		return selectVisibleReticulationsMenuItem;
	}

	public MenuItem getSelectStableNodesMenuItem() {
		return selectStableNodesMenuItem;
	}

	public MenuItem getSelectArticulationNodesMenuItem() {
		return selectArticulationNodesMenuItem;
	}

	public MenuItem getSelectAllBelowMenuItem() {
		return selectAllBelowMenuItem;
	}

	public MenuItem getSelectAllAboveMenuItem() {
		return selectAllAboveMenuItem;
	}

	public MenuItem getSelectPossibleRootLocationsMenuItem() {
		return selectPossibleRootLocationsMenuItem;
	}

	public MenuItem getSelectLowestStableAncestorMenuItem() {
		return selectLowestStableAncestorMenuItem;
	}

	public MenuItem getSelectInvertMenuItem() {
		return selectInvertMenuItem;
	}

	public MenuItem getExtendSelectionMenuItem() {
		return extendSelectionMenuItem;
	}

	public MenuItem getDeclareRootMenuItem() {
		return declareRootMenuItem;
	}

	public MenuItem getDeclareTransferAcceptorMenuItem() {
		return declareTransferAcceptorMenuItem;
	}

	public MenuItem getInduceMenuItem() {
		return induceMenuItem;
	}

	public CheckMenuItem getResizeModeCheckMenuItem() {
		return resizeModeCheckMenuItem;
	}

	public MenuItem getLayoutLabelMenuItem() {
		return layoutLabelMenuItem;
	}

	public MenuItem getRotateLeftMenuItem() {
		return rotateLeftMenuItem;
	}

	public MenuItem getRotateRightMenuItem() {
		return rotateRightMenuItem;
	}

	public MenuItem getFlipHorizontalMenuItem() {
		return flipHorizontalMenuItem;
	}

	public MenuItem getFlipVerticalMenuItem() {
		return flipVerticalMenuItem;
	}

	public MenuButton getModeMenuButton() {
		return modeMenuButton;
	}

	public Menu getModeMenu() {
		return modeMenu;
	}

	public RadioMenuItem getCaptureModeItem() {
		return captureModeItem;
	}

	public MenuItem getApplyModificationMenuItem() {
		return applyModificationMenuItem;
	}

	public MenuItem getMergeNodesMenuItem() {
		return mergeNodesMenuItem;
	}

	public MenuItem getDeleteThruNodesMenuItem() {
		return deleteThruNodesMenuItem;
	}

	public MenuItem getReverseEdgesMenuItem() {
		return reverseEdgesMenuItem;
	}

	public MenuItem getCrossEdgesMenuItem() {
		return crossEdgesMenuItem;
	}

	public AnchorPane getCenterAnchorPane() {
		return centerAnchorPane;
	}

	public AnchorPane getBottomAnchorPane() {
		return bottomAnchorPane;
	}

	public CheckMenuItem getShowQRCode() {
		return showQRCode;
	}

	public CheckMenuItem getShowNewick() {
		return showNewick;
	}

	public ToggleButton getShowSettingsButton() {
		return showSettingsButton;
	}

	public CheckMenuItem getOutlineEdgesMenuItem() {
		return outlineEdgesMenuItem;
	}

	public MenuItem getSetWindowSizeMenuItem() {
		return setWindowSizeMenuItem;
	}

	public CheckMenuItem getShowHelpWindow() {
		return showHelpWindow;
	}

	public MenuButton getCaptureMenuButton() {
		return captureMenuButton;
	}

	public CheckMenuItem getShowCaptureParametersItem() {
		return showCaptureParametersItem;
	}

	public MenuItem getLoadCaptureImageItem() {
		return loadCaptureImageItem;
	}

	public MenuItem getClearCaptureImageItem() {
		return clearCaptureImageItem;
	}

	public MenuItem getSaveCaptureImageItem() {
		return saveCaptureImageItem;
	}

	public CheckMenuItem getShowCaptureRootLocationItem() {
		return showCaptureRootLocationItem;
	}

	public CheckMenuItem getCaptureLinesItem() {
		return captureLinesItem;
	}

	public CheckMenuItem getCaptureLabelsItem() {
		return captureLabelsItem;
	}

	public CheckMenuItem getCapturePhylogenyItem() {
		return capturePhylogenyItem;
	}

	public Rectangle getSelectionRectangle() {
		return selectionRectangle;
	}

	public RadioMenuItem getCenterRootSideMenuItem() {
		return centerRootSideMenuItem;
	}

	public RadioMenuItem getBottomRootSideMenuItem() {
		return bottomRootSideMenuItem;
	}

	public RadioMenuItem getTopRootSideMenuItem() {
		return topRootSideMenuItem;
	}

	public RadioMenuItem getRightRootSideMenuItem() {
		return rightRootSideMenuItem;
	}

	public RadioMenuItem getLeftRootSideMenuItem() {
		return leftRootSideMenuItem;
	}

	public Menu getRootSideMenu() {
		return rootSideMenu;
	}

	public MenuItem getLayoutPhylogenyMenuItem() {
		return layoutPhylogenyMenuItem;
	}

	public ToggleGroup getLayoutToggleGroup() {
		return layoutToggleGroup;
	}

	public ToggleGroup getScalingToggleGroup() {
		return scalingToggleGroup;
	}

	public StackPane getCenterPane() {
		return centerPane;
	}

	public GridPane getToolbarGrid() {
		return toolbarGrid;
	}

	public HBox getLeftBox() {
		return leftBox;
	}

	public HBox getRightBox() {
		return rightBox;
	}

	public Button getPasteButton() {
		return pasteButton;
	}
}

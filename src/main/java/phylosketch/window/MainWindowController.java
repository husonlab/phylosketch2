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

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.BasicFX;
import jloda.fx.util.ProgramProperties;
import jloda.fx.window.MainWindowManager;
import phylosketch.view.DrawView;

import java.util.ArrayList;
import java.util.List;

public class MainWindowController {
	@FXML
	private MenuItem aboutMenuItem;

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
	private MenuItem mergeNodesMenuItem;

	@FXML
	private MenuItem deleteThruNodesMenuItem;

	@FXML
	private MenuItem reverseEdgesMenuItem;

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
	private MenuItem openImageFileItem;

	@FXML
	private Menu fileMenu;

	@FXML
	private MenuButton fileMenuButton;

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
	private MenuItem newRecentFileMenuItem;

	@FXML
	private MenuItem openMenuItem;

	@FXML
	private MenuItem openRecentFileMenuItem;

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
	private Menu labelsMenu;


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
	private Button selectButton;

	@FXML
	private MenuButton selectMenuButton;

	@FXML
	private MenuButton layoutMenuButton;

	@FXML
	private Button deleteButton;

	@FXML
	private MenuButton captureMenuButton;

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
	private RadioMenuItem editModeItem;

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
	private MenuItem rectangularCladogramMenuItem;

	@FXML
	private MenuItem circularCladogramMenuItem;

	@FXML
	private MenuItem rectangularPhylogramMenuItem;

	@FXML
	private MenuItem circularPhylogramMenuItem;

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
	private Button importButton;

	@FXML
	private BorderPane toolBorderPane;

	@FXML
	private HBox leftHBox;
	@FXML
	private HBox rightHBox;
	@FXML
	private HBox overflowHBox;

	@FXML
	private CheckMenuItem showQRCode;

	@FXML
	private CheckMenuItem showNewick;

	@FXML
	private CheckMenuItem outlineEdgesMenuItem;

	@FXML
	private VBox firstVBox;

	private final ZoomableScrollPane scrollPane = new ZoomableScrollPane(null);

	private final Rectangle selectionRectangle = new Rectangle();

	@FXML
	private void initialize() {


		MaterialIcons.setIcon(fileMenuButton, MaterialIcons.file_open);
		MaterialIcons.setIcon(modeMenuButton, MaterialIcons.edit_off);

		MaterialIcons.setIcon(exportMenuButton, MaterialIcons.ios_share);

		MaterialIcons.setIcon(undoButton, MaterialIcons.undo);
		MaterialIcons.setIcon(redoButton, MaterialIcons.redo);

		MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in);
		MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out);

		MaterialIcons.setIcon(findButton, MaterialIcons.search);
		MaterialIcons.setIcon(selectButton, MaterialIcons.select_all);

		MaterialIcons.setIcon(importButton, MaterialIcons.file_download);

		MaterialIcons.setIcon(selectMenuButton, MaterialIcons.checklist_rtl);
		MaterialIcons.setIcon(layoutMenuButton, MaterialIcons.shape_line);
		MaterialIcons.setIcon(deleteButton, MaterialIcons.backspace);

		MaterialIcons.setIcon(showSettingsButton, MaterialIcons.format_shapes);

		MaterialIcons.setIcon(captureMenuButton, MaterialIcons.rocket_launch);

		increaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));
		decreaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("/", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));

		// if we are running on MacOS, put the specific menu items in the right places
		if (ProgramProperties.isMacOS()) {
			getMenuBar().setUseSystemMenuBar(true);
			fileMenu.getItems().remove(getQuitMenuItem());
			// windowMenu.getItems().remove(getAboutMenuItem());
			//editMenu.getItems().remove(getPreferencesMenuItem());
		}

		final var originalWindowMenuItems = new ArrayList<>(windowMenu.getItems());

		final InvalidationListener invalidationListener = observable -> {
			windowMenu.getItems().setAll(originalWindowMenuItems);
			var count = 0;
			for (var mainWindow : MainWindowManager.getInstance().getMainWindows()) {
				if (mainWindow.getStage() != null) {
					var title = mainWindow.getStage().getTitle();
					if (title != null) {
						try {
							var menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
							menuItem.setOnAction(e -> mainWindow.getStage().toFront());
							menuItem.setAccelerator(new KeyCharacterCombination("" + (++count), KeyCombination.SHORTCUT_DOWN));
							windowMenu.getItems().add(menuItem);
						} catch (Exception ignored) {
						}
					}
				}
				if (MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow) != null) {
					for (var auxStage : MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow)) {
						var title = auxStage.getTitle();
						if (title != null) {
							try {
								var menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
							menuItem.setOnAction(e -> auxStage.toFront());
							windowMenu.getItems().add(menuItem);
							} catch (Exception ignored) {
							}
						}
					}
				}
			}
		};
		MainWindowManager.getInstance().changedProperty().addListener(invalidationListener);
		invalidationListener.invalidated(null);

		{
			newRecentFileMenuItem.setOnAction(e -> newMenuItem.getOnAction().handle(e));
			newRecentFileMenuItem.disableProperty().bind(newMenuItem.disableProperty());
			openRecentFileMenuItem.setOnAction(e -> openMenuItem.getOnAction().handle(e));
			openRecentFileMenuItem.disableProperty().bind(openMenuItem.disableProperty());

			var keep = new ArrayList<>(fileMenuButton.getItems());

			recentFilesMenu.getItems().addListener((InvalidationListener) e -> {
				fileMenuButton.getItems().setAll(keep);
				fileMenuButton.getItems().addAll(BasicFX.copyMenu(recentFilesMenu.getItems()));
			});
		}

		{
			zoomInButton.setOnAction(e -> zoomInMenuItem.getOnAction().handle(e));
			zoomInButton.disableProperty().bindBidirectional(zoomInMenuItem.disableProperty());
			zoomOutButton.setOnAction(e -> zoomOutMenuItem.getOnAction().handle(e));
			zoomOutButton.disableProperty().bindBidirectional(zoomOutMenuItem.disableProperty());

			undoButton.setOnAction(e->undoMenuItem.getOnAction().handle(e));
			undoButton.disableProperty().bindBidirectional(undoMenuItem.disableProperty());
			redoButton.setOnAction(e->redoMenuItem.getOnAction().handle(e));
			redoButton.disableProperty().bindBidirectional(redoMenuItem.disableProperty());

			deleteButton.setOnAction(e -> deleteMenuItem.getOnAction().handle(e));
			deleteButton.disableProperty().bind(deleteMenuItem.disableProperty());
		}

		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setPannable(true);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		scrollPane.setLockAspectRatio(true);
		scrollPane.setRequireShiftOrControlToZoom(true);

		selectMenuButton.getItems().addAll(BasicFX.copyMenu(selectMenu.getItems()));
		layoutMenuButton.getItems().addAll(BasicFX.copyMenu(layoutMenu.getItems()));
		layoutMenuButton.getItems().add(new SeparatorMenuItem());
		layoutMenuButton.getItems().addAll(BasicFX.copyMenu(List.of(mergeNodesMenuItem, deleteThruNodesMenuItem, reverseEdgesMenuItem, crossEdgesMenuItem, declareRootMenuItem, declareTransferAcceptorMenuItem, induceMenuItem)));

		copyExportMenuItem.setOnAction(e->copyMenuItem.getOnAction().handle(e));
		copyExportMenuItem.disableProperty().bind(copyMenuItem.disableProperty());

		copyImageExportMenuItem.setOnAction(e->copyImageMenuItem.getOnAction().handle(e));
		copyImageExportMenuItem.disableProperty().bind(copyImageMenuItem.disableProperty());

		centerPane.getChildren().add(scrollPane);

		overflowHBox.setMinHeight(HBox.USE_PREF_SIZE);
		overflowHBox.setMaxHeight(HBox.USE_PREF_SIZE);
		overflowHBox.setPrefHeight(0);
		overflowHBox.setVisible(false);

		centerAnchorPane.setMinWidth(AnchorPane.USE_PREF_SIZE);
		centerAnchorPane.setMaxWidth(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.setMinWidth(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.setMaxWidth(AnchorPane.USE_PREF_SIZE);

		bottomAnchorPane.setMinHeight(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.setMaxHeight(AnchorPane.USE_PREF_SIZE);
		bottomAnchorPane.prefHeightProperty().bind(bottomFlowPane.heightProperty());

		rootPane.widthProperty().addListener(getWidthChangeListener());

		selectionRectangle.setStroke(Color.GREEN);
		selectionRectangle.setFill(Color.TRANSPARENT);
		selectionRectangle.getStrokeDashArray().setAll(3.0, 3.0);

		showCaptureParametersItem.setGraphic(MaterialIcons.graphic(MaterialIcons.settings));
		loadCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.file_open));
		clearCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.hide_image));
		saveCaptureImageItem.setGraphic(MaterialIcons.graphic(MaterialIcons.save_as));
		showCaptureRootLocationItem.setGraphic(MaterialIcons.graphic(MaterialIcons.rocket, "-fx-rotate: 90;"));
		captureLinesItem.setGraphic(MaterialIcons.graphic(MaterialIcons.water));
		captureLabelsItem.setGraphic(MaterialIcons.graphic(MaterialIcons.text_fields));
		capturePhylogenyItem.setGraphic(MaterialIcons.graphic(MaterialIcons.account_tree));

		(new ToggleGroup()).getToggles().addAll(leftRootSideMenuItem, rightRootSideMenuItem, topRootSideMenuItem, bottomRootSideMenuItem, centerRootSideMenuItem);
	}

	public static MaterialIcons getIcon(DrawView.Mode mode) {
		return switch (mode) {
			case Edit -> MaterialIcons.edit;
			case Move -> MaterialIcons.transform;
			case Capture -> MaterialIcons.image;
			case View -> MaterialIcons.lock;
		};
	}

	public void setupModeMenu(ObjectProperty<DrawView.Mode> modeProperty) {
		var toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(editModeItem, moveModeItem, viewModeItem, captureModeItem);
		modeProperty.addListener((v, o, n) -> {
			switch (n) {
				case Edit -> editModeItem.setSelected(true);
				case Move -> moveModeItem.setSelected(true);
				case View -> viewModeItem.setSelected(true);
				case Capture -> captureModeItem.setSelected(true);
			}
		});
	}

	public ChangeListener<Number> getWidthChangeListener() {
		return (v, o, n) -> {
			if (n.doubleValue() < 600 && !overflowHBox.isVisible()) {
				toolBorderPane.getChildren().remove(rightHBox);
				overflowHBox.getChildren().add(rightHBox);
				overflowHBox.setVisible(true);
				overflowHBox.setPrefHeight(32);
			}
			if (n.doubleValue() > 600 && overflowHBox.isVisible()) {
				overflowHBox.getChildren().remove(rightHBox);
				toolBorderPane.setRight(rightHBox);
				overflowHBox.setVisible(false);
				overflowHBox.setPrefHeight(0);
			}

			if (n.doubleValue() >= 375) {
				centerAnchorPane.setPrefWidth(n.doubleValue());
				bottomAnchorPane.setPrefWidth(n.doubleValue());
			}
		};
	}

	public MenuItem getAboutMenuItem() {
		return aboutMenuItem;
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

	public MenuButton getFileMenuButton() {
		return fileMenuButton;
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

	public MenuItem getNewRecentFileMenuItem() {
		return newRecentFileMenuItem;
	}

	public MenuItem getOpenMenuItem() {
		return openMenuItem;
	}

	public MenuItem getOpenRecentFileMenuItem() {
		return openRecentFileMenuItem;
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

	public Button getSelectButton() {
		return selectButton;
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

	public MenuItem getRectangularCladogramMenuItem() {
		return rectangularCladogramMenuItem;
	}

	public MenuItem getCircularCladogramMenuItem() {
		return circularCladogramMenuItem;
	}

	public MenuItem getRectangularPhylogramMenuItem() {
		return rectangularPhylogramMenuItem;
	}

	public MenuItem getCircularPhylogramMenuItem() {
		return circularPhylogramMenuItem;
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

	public Button getImportButton() {
		return importButton;
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

	public HBox getLeftHBox() {
		return leftHBox;
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

	public MenuItem getOpenImageFileItem() {
		return openImageFileItem;
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
}

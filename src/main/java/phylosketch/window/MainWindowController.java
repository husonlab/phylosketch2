/*
 *  MainWindowController.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.BasicFX;
import jloda.fx.util.ProgramProperties;
import jloda.fx.window.MainWindowManager;

import java.util.ArrayList;
import java.util.List;

public class MainWindowController {
	@FXML
	private MenuItem aboutMenuItem;

	@FXML
	private FlowPane bottomFlowPane;

	@FXML
	private MenuItem checkForUpdatesMenuItem;

	@FXML
	private MenuItem clearMenuItem;

	@FXML
	private MenuItem removeThruNodesMenuItem;

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
	private MenuButton exportMenuButton;

	@FXML
	private MenuItem exportNewickMenuItem;

	@FXML
	private MenuItem exportImageMenuItem;

	@FXML
	private Menu fileMenu;

	@FXML
	private MenuButton fileMenuButton;

	@FXML
	private MenuItem findAgainMenuItem;

	@FXML
	private ToggleButton findButton;

	@FXML
	private MenuItem findMenuItem;

	@FXML
	private MenuItem fullScreenMenuItem;

	@FXML
	private Button importButton;

	@FXML
	private MenuItem importMenuItem;

	@FXML
	private Button zoomInButton;

	@FXML
	private MenuItem increaseFontSizeMenuItem;

	@FXML
	private AnchorPane innerAnchorPane;

	@FXML
	private ToolBar mainToolBar;

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
	private MenuItem deleteRecentFilesMenuItem;

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
	private CheckMenuItem editEdgeLabelsMenuItem;

	@FXML
	private AnchorPane editEdgesAnchorPane;

	@FXML
	private ToolBar edgeLabelsToolBar;

	@FXML
	private TextField edgeWeightTextField;

	@FXML
	private Button measureWeightsButton;

	@FXML
	private TextField edgeConfidenceTextField;

	@FXML
	private TextField edgeProbabilityTextField;

	@FXML
	private Button closeEdgeToolBar;

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
	private CheckMenuItem labelEdgeByWeightsMenuItem;

	@FXML
	private CheckMenuItem labelEdgeByConfidenceMenuItem;

	@FXML
	private CheckMenuItem labelEdgeByProbabilityMenuItem;

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
	private MenuButton settingsMenuButton;

	@FXML
	private MenuButton layoutMenuButton;

	@FXML
	private Button deleteButton;

	@FXML
	private Menu layoutMenu;

	@FXML
	private CheckMenuItem arrowsMenuItem;

	@FXML
	private CheckMenuItem outlineEdgesMenuItem;

	@FXML
	private MenuItem smoothMenuItem;

	@FXML
	private MenuItem straightenMenuItem;

	@FXML
	private MenuItem rerootMenuItem;

	@FXML
	private MenuItem rectangularMenuItem;

	@FXML
	private StackPane centerPane;

	@FXML
	private Menu modeMenu;

	@FXML
	private CheckMenuItem editModeCheckMenuItem;

	@FXML
	private CheckMenuItem moveModeCheckMenuItem;

	@FXML
	private CheckMenuItem resizeModeCheckMenuItem;

	@FXML
	private ToggleButton editModeToggleButton;

	@FXML
	private ToggleButton resizeModeToggleButton;


	@FXML
	private final ZoomableScrollPane scrollPane = new ZoomableScrollPane(null);

	@FXML
	private void initialize() {
		MaterialIcons.setIcon(fileMenuButton, MaterialIcons.file_open);
		MaterialIcons.setIcon(importButton, MaterialIcons.download);
		MaterialIcons.setIcon(editModeToggleButton, MaterialIcons.edit_off);
		MaterialIcons.setIcon(resizeModeToggleButton, MaterialIcons.open_in_full, "-fx-rotate: 90;", true);

		MaterialIcons.setIcon(exportMenuButton, MaterialIcons.ios_share);

		MaterialIcons.setIcon(undoButton, MaterialIcons.undo);
		MaterialIcons.setIcon(redoButton, MaterialIcons.redo);

		MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in);
		MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out);

		MaterialIcons.setIcon(findButton, MaterialIcons.search);
		MaterialIcons.setIcon(selectButton, MaterialIcons.select_all);

		MaterialIcons.setIcon(selectMenuButton,MaterialIcons.checklist);
		MaterialIcons.setIcon(settingsMenuButton, MaterialIcons.new_label);
		MaterialIcons.setIcon(layoutMenuButton, MaterialIcons.shape_line);
		MaterialIcons.setIcon(deleteButton, MaterialIcons.disabled_by_default);

		MaterialIcons.setIcon(closeEdgeToolBar, MaterialIcons.clear);
		MaterialIcons.setIcon(measureWeightsButton, MaterialIcons.square_foot);

		increaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));
		decreaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("/", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));

		edgeLabelsToolBar.visibleProperty().addListener((v, o, n) -> {
			editEdgesAnchorPane.setPrefHeight(n ? 32 : 0);
			editEdgesAnchorPane.setMinHeight(ToolBar.USE_PREF_SIZE);
			editEdgesAnchorPane.setMaxHeight(ToolBar.USE_PREF_SIZE);
			edgeLabelsToolBar.setPrefHeight(n ? 32 : 0);
			edgeLabelsToolBar.setMinHeight(ToolBar.USE_PREF_SIZE);
			edgeLabelsToolBar.setMaxHeight(ToolBar.USE_PREF_SIZE);

			closeEdgeToolBar.setVisible(n);
		});
		edgeLabelsToolBar.setVisible(false);
		editEdgeLabelsMenuItem.selectedProperty().bindBidirectional(edgeLabelsToolBar.visibleProperty());
		closeEdgeToolBar.setOnAction(e -> edgeLabelsToolBar.setVisible(false));

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
						var menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
						menuItem.setOnAction(e -> mainWindow.getStage().toFront());
						menuItem.setAccelerator(new KeyCharacterCombination("" + (++count), KeyCombination.SHORTCUT_DOWN));
						windowMenu.getItems().add(menuItem);
					}
				}
				if (MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow) != null) {
					for (var auxStage : MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow)) {
						var title = auxStage.getTitle();
						if (title != null) {
							var menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
							menuItem.setOnAction(e -> auxStage.toFront());
							windowMenu.getItems().add(menuItem);
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
			deleteRecentFilesMenuItem.setOnAction(e-> deleteMenuItem.getOnAction().handle(e));
			deleteRecentFilesMenuItem.disableProperty().bind(deleteMenuItem.disableProperty());

			recentFilesMenu.getItems().addListener((InvalidationListener) e -> {
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
		settingsMenuButton.getItems().addAll(BasicFX.copyMenu(labelsMenu.getItems()));
		layoutMenuButton.getItems().addAll(BasicFX.copyMenu(layoutMenu.getItems()));
		layoutMenuButton.getItems().add(new SeparatorMenuItem());
		layoutMenuButton.getItems().addAll(BasicFX.copyMenu(List.of(removeThruNodesMenuItem, rerootMenuItem)));

		copyExportMenuItem.setOnAction(e->copyMenuItem.getOnAction().handle(e));
		copyExportMenuItem.disableProperty().bind(copyMenuItem.disableProperty());

		copyImageExportMenuItem.setOnAction(e->copyImageMenuItem.getOnAction().handle(e));
		copyImageExportMenuItem.disableProperty().bind(copyImageMenuItem.disableProperty());

		centerPane.getChildren().add(scrollPane);
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

	public MenuItem getFullScreenMenuItem() {
		return fullScreenMenuItem;
	}

	public Button getImportButton() {
		return importButton;
	}

	public MenuItem getImportMenuItem() {
		return importMenuItem;
	}

	public Button getZoomInButton() {
		return zoomInButton;
	}

	public MenuItem getIncreaseFontSizeMenuItem() {
		return increaseFontSizeMenuItem;
	}

	public AnchorPane getInnerAnchorPane() {
		return innerAnchorPane;
	}

	public ToolBar getMainToolBar() {
		return mainToolBar;
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

	public CheckMenuItem getArrowsMenuItem() {
		return arrowsMenuItem;
	}

	public CheckMenuItem getOutlineEdgesMenuItem() {
		return outlineEdgesMenuItem;
	}

	public MenuItem getSmoothMenuItem() {
		return smoothMenuItem;
	}

	public MenuItem getStraightenMenuItem() {
		return straightenMenuItem;
	}

	public MenuItem getRerootMenuItem() {
		return rerootMenuItem;
	}

	public MenuItem getRectangularMenuItem() {
		return rectangularMenuItem;
	}

	public CheckMenuItem getEditModeCheckMenuItem() {
		return editModeCheckMenuItem;
	}

	public CheckMenuItem getMoveModeCheckMenuItem() {
		return moveModeCheckMenuItem;
	}

	public CheckMenuItem getResizeModeCheckMenuItem() {
		return resizeModeCheckMenuItem;
	}

	public ToggleButton getEditModeToggleButton() {
		return editModeToggleButton;
	}

	public ToggleButton getResizeModeToggleButton() {
		return resizeModeToggleButton;
	}

	public MenuItem getRemoveThruNodesMenuItem() {
		return removeThruNodesMenuItem;
	}

	public CheckMenuItem getLabelEdgeByWeightsMenuItem() {
		return labelEdgeByWeightsMenuItem;
	}

	public CheckMenuItem getLabelEdgeByConfidenceMenuItem() {
		return labelEdgeByConfidenceMenuItem;
	}

	public CheckMenuItem getLabelEdgeByProbabilityMenuItem() {
		return labelEdgeByProbabilityMenuItem;
	}

	public TextField getEdgeWeightTextField() {
		return edgeWeightTextField;
	}

	public TextField getEdgeConfidenceTextField() {
		return edgeConfidenceTextField;
	}

	public TextField getEdgeProbabilityTextField() {
		return edgeProbabilityTextField;
	}

	public Button getMeasureWeightsButton() {
		return measureWeightsButton;
	}
}

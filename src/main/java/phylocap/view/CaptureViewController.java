/*
 * CaptureViewController.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.ComboBoxUtils;

public class CaptureViewController {

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private MenuButton runMenuButton;

	@FXML
	private MenuItem detectAllMenuItem;

	@FXML
	private MenuItem clearMenuItem;

	@FXML
	private CheckMenuItem detectWordsMenuItem;

	@FXML
	private CheckMenuItem detectSegmentsMenuItem;

	@FXML
	private CheckMenuItem removeDustMenuItem;

	@FXML
	private CheckMenuItem detectPhylogenyMenuItem;

	@FXML
	private Pane centerPane;

	@FXML
	private Button importButton;

	@FXML
	private Label infoLabel;

	@FXML
	private ComboBox<Integer> maxLabelHeightCBox;

	@FXML
	private ToggleButton setRootLocationToggleButton;

	@FXML
	private ComboBox<Double> whiteThresholdCBox;

	@FXML
	private ComboBox<Integer> dustMaxDistCBox;
	@FXML
	private ComboBox<Integer> dustMinExtentCBox;

	@FXML
	private FlowPane statusPane;

	@FXML
	private Button zoomInButton;

	@FXML
	private Button zoomOutButton;

	@FXML
	private ToggleButton settingsButton;

	@FXML
	private ToolBar settingsToolBar;

	@FXML
	private MenuButton selectMenuButton;

	@FXML
	private MenuItem selectAllMenuItem;

	@FXML
	private MenuItem selectNoneMenuItem;

	@FXML
	private MenuItem selectInvertMenuItem;

	@FXML
	private MenuItem deleteMenuItem;


	@FXML
	private void initialize() {
		MaterialIcons.setIcon(importButton, MaterialIcons.download);
		MaterialIcons.setIcon(settingsButton, MaterialIcons.settings_brightness);
		MaterialIcons.setIcon(selectMenuButton, MaterialIcons.checklist_rtl);

		MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in);
		MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out);

		MaterialIcons.setIcon(runMenuButton, MaterialIcons.play_arrow);

		whiteThresholdCBox.getItems().addAll(0.5, 0.6, 0.7, 0.8, 0.9);
		whiteThresholdCBox.setValue(0.9);
		ComboBoxUtils.ensureDoubleInput(whiteThresholdCBox);

		dustMaxDistCBox.getItems().addAll(5, 10, 20, 50);
		dustMaxDistCBox.setValue(20);
		ComboBoxUtils.ensureIntegerInput(dustMaxDistCBox);

		dustMinExtentCBox.getItems().addAll(20, 40, 50, 80, 100, 160);
		dustMinExtentCBox.setValue(50);
		ComboBoxUtils.ensureIntegerInput(dustMinExtentCBox);


		maxLabelHeightCBox.getItems().addAll(12, 16, 24, 36, 48, 60, 100);
		maxLabelHeightCBox.setValue(24);
		ComboBoxUtils.ensureIntegerInput(maxLabelHeightCBox);

		if (settingsToolBar.getParent() instanceof Pane parentPane) {
			parentPane.getChildren().remove(settingsToolBar);
			settingsButton.selectedProperty().addListener((v, o, n) -> {
				if (n)
					parentPane.getChildren().add(settingsToolBar);
				else parentPane.getChildren().remove(settingsToolBar);
			});
		}
	}

	public Pane getCenterPane() {
		return centerPane;
	}

	public Button getImportButton() {
		return importButton;
	}

	public Label getInfoLabel() {
		return infoLabel;
	}

	public ComboBox<Integer> getMaxLabelHeightCBox() {
		return maxLabelHeightCBox;
	}

	public ToggleButton getSetRootLocationToggleButton() {
		return setRootLocationToggleButton;
	}

	public ComboBox<Double> getWhiteThresholdCBox() {
		return whiteThresholdCBox;
	}

	public FlowPane getStatusPane() {
		return statusPane;
	}

	public Button getZoomInButton() {
		return zoomInButton;
	}

	public Button getZoomOutButton() {
		return zoomOutButton;
	}

	public ScrollPane getScrollPane() {
		return scrollPane;
	}

	public MenuButton getRunMenuButton() {
		return runMenuButton;
	}

	public MenuItem getDetectAllMenuItem() {
		return detectAllMenuItem;
	}

	public MenuItem getClearMenuItem() {
		return clearMenuItem;
	}

	public CheckMenuItem getDetectWordsMenuItem() {
		return detectWordsMenuItem;
	}

	public CheckMenuItem getDetectSegmentsMenuItem() {
		return detectSegmentsMenuItem;
	}

	public CheckMenuItem getRemoveDustMenuItem() {
		return removeDustMenuItem;
	}

	public CheckMenuItem getDetectPhylogenyMenuItem() {
		return detectPhylogenyMenuItem;
	}

	public MenuItem getSelectAllMenuItem() {
		return selectAllMenuItem;
	}

	public MenuItem getSelectNoneMenuItem() {
		return selectNoneMenuItem;
	}

	public MenuItem getSelectInvertMenuItem() {
		return selectInvertMenuItem;
	}

	public MenuItem getDeleteMenuItem() {
		return deleteMenuItem;
	}

	public ComboBox<Integer> getDustMaxDistCBox() {
		return dustMaxDistCBox;
	}

	public ComboBox<Integer> getDustMinExtentCBox() {
		return dustMinExtentCBox;
	}
}

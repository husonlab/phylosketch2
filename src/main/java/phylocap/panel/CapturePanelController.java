/*
 * CapturePanelController.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.panel;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import jloda.fx.icons.MaterialIcons;

import java.util.Arrays;

public class CapturePanelController {

	@FXML
	private MenuItem clearMenuItem;

	@FXML
	private MenuItem deleteMenuItem;

	@FXML
	private MenuItem detectAllMenuItem;

	@FXML
	private CheckMenuItem detectPhylogenyMenuItem;

	@FXML
	private CheckMenuItem detectSegmentsMenuItem;

	@FXML
	private CheckMenuItem detectWordsMenuItem;

	@FXML
	private ChoiceBox<Parameters.Key> parameterCBox;

	@FXML
	private TextField parameterTextField;

	@FXML
	private ToggleButton placeRootToggle;

	@FXML
	private CheckMenuItem removeDustMenuItem;

	@FXML
	private Button resetButton;

	@FXML
	private MenuButton runMenuButton;

	@FXML
	private MenuButton selectMenuButton;

	@FXML
	private MenuItem selectAllMenuItem;

	@FXML
	private MenuItem selectInvertMenuItem;

	@FXML
	private MenuItem selectNoneMenuItem;

	@FXML
	private void initialize() {
		MaterialIcons.setIcon(selectMenuButton, MaterialIcons.checklist_rtl);
		MaterialIcons.setIcon(runMenuButton, MaterialIcons.play_arrow);

		MaterialIcons.setIcon(resetButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);

		parameterCBox.getItems().addAll(Parameters.Key.values());
	}

	public MenuItem getClearMenuItem() {
		return clearMenuItem;
	}

	public MenuItem getDeleteMenuItem() {
		return deleteMenuItem;
	}

	public MenuItem getDetectAllMenuItem() {
		return detectAllMenuItem;
	}

	public CheckMenuItem getDetectPhylogenyMenuItem() {
		return detectPhylogenyMenuItem;
	}

	public CheckMenuItem getDetectSegmentsMenuItem() {
		return detectSegmentsMenuItem;
	}

	public CheckMenuItem getDetectWordsMenuItem() {
		return detectWordsMenuItem;
	}

	public ChoiceBox<Parameters.Key> getParameterCBox() {
		return parameterCBox;
	}

	public TextField getParameterTextField() {
		return parameterTextField;
	}

	public ToggleButton getPlaceRootToggle() {
		return placeRootToggle;
	}

	public CheckMenuItem getRemoveDustMenuItem() {
		return removeDustMenuItem;
	}

	public Button getResetButton() {
		return resetButton;
	}

	public MenuButton getRunMenuButton() {
		return runMenuButton;
	}

	public MenuButton getSelectMenuButton() {
		return selectMenuButton;
	}

	public MenuItem getSelectAllMenuItem() {
		return selectAllMenuItem;
	}

	public MenuItem getSelectInvertMenuItem() {
		return selectInvertMenuItem;
	}

	public MenuItem getSelectNoneMenuItem() {
		return selectNoneMenuItem;
	}
}

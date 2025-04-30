/*
 * TreesNetController.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.treesnet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class TreesNetController {

	@FXML
	private Pane centerPane;

	@FXML
	private Button doneButton;

	@FXML
	private AnchorPane mainAnchorPane;

	@FXML
	private BorderPane mainBorderPane;

	@FXML
	private ButtonBar mainButtonBar;

	@FXML
	private ToolBar mainTollBar;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private ToggleButton toScaleToggle;

	@FXML
	private void initialize() {
	}

	public Pane getCenterPane() {
		return centerPane;
	}

	public Button getDoneButton() {
		return doneButton;
	}

	public AnchorPane getMainAnchorPane() {
		return mainAnchorPane;
	}

	public BorderPane getMainBorderPane() {
		return mainBorderPane;
	}

	public ButtonBar getMainButtonBar() {
		return mainButtonBar;
	}

	public ToolBar getMainTollBar() {
		return mainTollBar;
	}

	public ScrollPane getScrollPane() {
		return scrollPane;
	}

	public ToggleButton getToScaleToggle() {
		return toScaleToggle;
	}
}

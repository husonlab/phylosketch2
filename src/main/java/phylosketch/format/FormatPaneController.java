/*
 * FormatPaneController.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.format;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import jloda.fx.icons.MaterialIcons;
import phylosketch.utils.ComboBoxUtils;
import phylosketch.view.LineType;

/**
 * format pane controller
 * Daniel Huson, 11.2024
 */
public class FormatPaneController {
	@FXML
	private Button closeButton;

	@FXML
	private ColorPicker edgeColorPicker;

	@FXML
	private Button clearEdgeColorButton;

	@FXML
	private ToggleButton showArrowsButton;

	@FXML
	private Button edgeCurvedButton;

	@FXML
	private ColorPicker edgeLabelBackgroundColorPicker;

	@FXML
	private Button clearEdgeLabelBackgroundColorButton;

	@FXML
	private Button edgeLabelBoldButton;

	@FXML
	private ColorPicker edgeLabelColorPicker;

	@FXML
	private Button clearEdgeLabelColorButton;


	@FXML
	private ChoiceBox<String> edgeLabelFontChoiceBox;

	@FXML
	private Button clearEdgeLabelFontButton;

	@FXML
	private Button edgeLabelItalicButton;

	@FXML
	private ComboBox<Double> edgeLabelSizeCBox;

	@FXML
	private Button edgeLabelUnderlineButton;

	@FXML
	private Button clearEdgeLabelStyleButton;

	@FXML
	private ChoiceBox<LineType> edgeLineChoiceBox;

	@FXML
	private Button edgeRectangularButton;

	@FXML
	private Button edgeStraightButton;

	@FXML
	private ComboBox<Double> edgeWidthCBox;

	@FXML
	private ToggleButton showWeightToggleButton;

	@FXML
	private ToggleButton showSupportToggleButton;

	@FXML
	private ToggleButton showProbabilityToggleButton;


	@FXML
	private ColorPicker nodeColorPicker;

	@FXML
	private Button clearNodeColorButton;

	@FXML
	private Button nodeLabelBoldButton;

	@FXML
	private ColorPicker nodeLabelBackgroundColorPicker;

	@FXML
	private Button clearNodeLabelBackgroundColorButton;

	@FXML
	private ColorPicker nodeLabelColorPicker;

	@FXML
	private Button clearNodeLabelColorButton;

	@FXML
	private ChoiceBox<String> nodeLabelFontChoiceBox;

	@FXML
	private Button clearNodeLabelFontButton;

	@FXML
	private ComboBox<Double> nodeLabelSizeCBox;

	@FXML
	private Button nodeLabelItalicButton;

	@FXML
	private Button clearNodeLabelStyleButton;

	@FXML
	private Button nodeLabelUnderlineButton;

	@FXML
	private ChoiceBox<?> nodeShapeChoiceBox;

	@FXML
	private ComboBox<Double> nodeSizeCBox;

	@FXML
	private VBox rootPane;

	@FXML
	private Button smoothButton;

	@FXML
	private TextField edgeWeightTextField;

	@FXML
	private TextField edgeSupportTextField;

	@FXML
	private TextField edgeProbabilityTextField;

	@FXML
	private Button measureEdgeWeightsButton;

	@FXML
	private ChoiceBox<String> howToLabelNodesCBox;

	@FXML
	private ChoiceBox<String> useNodesToLabelCBox;

	@FXML
	private ToggleButton uniqueLabelsToggleButton;

	@FXML
	private TextField nodeLabelTextField;

	@FXML
	private void initialize() {
		MaterialIcons.setIcon(nodeLabelBoldButton, MaterialIcons.format_bold);
		MaterialIcons.setIcon(nodeLabelItalicButton, MaterialIcons.format_italic);
		MaterialIcons.setIcon(nodeLabelUnderlineButton, MaterialIcons.format_underline);

		MaterialIcons.setIcon(edgeRectangularButton, MaterialIcons.keyboard_return, "-fx-scale-x: -1;-fx-scale-y: -1;", true);
		MaterialIcons.setIcon(edgeStraightButton, MaterialIcons.arrow_right_alt);
		MaterialIcons.setIcon(edgeCurvedButton, MaterialIcons.redo, "-fx-scale-y: -1;", true);

		MaterialIcons.setIcon(edgeLabelBoldButton, MaterialIcons.format_bold);
		MaterialIcons.setIcon(edgeLabelItalicButton, MaterialIcons.format_italic);
		MaterialIcons.setIcon(edgeLabelUnderlineButton, MaterialIcons.format_underline);

		MaterialIcons.setIcon(closeButton, MaterialIcons.close);

		MaterialIcons.setIcon(measureEdgeWeightsButton, MaterialIcons.square_foot);
		MaterialIcons.setIcon(showWeightToggleButton, MaterialIcons.visibility);
		MaterialIcons.setIcon(showSupportToggleButton, MaterialIcons.visibility);
		MaterialIcons.setIcon(showProbabilityToggleButton, MaterialIcons.visibility);

		MaterialIcons.setIcon(uniqueLabelsToggleButton, MaterialIcons.flash_on);

		MaterialIcons.setIcon(clearNodeColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearNodeLabelFontButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearNodeLabelStyleButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearNodeLabelColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearNodeLabelBackgroundColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearEdgeColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearEdgeLabelFontButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearEdgeLabelStyleButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearEdgeLabelColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		MaterialIcons.setIcon(clearEdgeLabelBackgroundColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);

		nodeLabelSizeCBox.getItems().addAll(6.0, 8.0, 10.0, 12.0, 14.0, 18.0, 24.0, 48.0);
		ComboBoxUtils.ensureDoubleInput(nodeLabelSizeCBox);
		edgeLabelSizeCBox.getItems().addAll(6.0, 8.0, 10.0, 12.0, 14.0, 18.0, 24.0, 48.0);
		ComboBoxUtils.ensureDoubleInput(edgeLabelSizeCBox);
		nodeSizeCBox.getItems().addAll(1.0, 2.0, 3.0, 4.0, 5.0, 8.0, 10.0, 16.0, 24.0);
		ComboBoxUtils.ensureDoubleInput(nodeSizeCBox);
		edgeWidthCBox.getItems().addAll(1.0, 2.0, 3.0, 4.0, 5.0, 8.0, 10.0, 16.0, 24.0);
		ComboBoxUtils.ensureDoubleInput(edgeWidthCBox);

		getEdgeLineChoiceBox().getItems().addAll(LineType.values());
		getEdgeLineChoiceBox().setValue(LineType.Solid);

		nodeLabelFontChoiceBox.getItems().addAll("", "SansSerif", "Serif", "Monospaced");
		edgeLabelFontChoiceBox.getItems().addAll("", "SansSerif", "Serif", "Monospaced");

		edgeWeightTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
		edgeSupportTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
		edgeProbabilityTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));

		howToLabelNodesCBox.getItems().addAll("A,B,C", "a,b,c", "t1,2,3", "x1,2,3", "None");
		howToLabelNodesCBox.setValue(null);
		useNodesToLabelCBox.getItems().addAll("Leaves", "Internal", "All");
		useNodesToLabelCBox.setValue("Leaves");

		uniqueLabelsToggleButton.setSelected(true);
	}


	public ColorPicker getEdgeColorPicker() {
		return edgeColorPicker;
	}

	public Button getEdgeCurvedButton() {
		return edgeCurvedButton;
	}

	public ColorPicker getEdgeLabelBackgroundColorPicker() {
		return edgeLabelBackgroundColorPicker;
	}

	public Button getEdgeLabelBoldButton() {
		return edgeLabelBoldButton;
	}

	public ColorPicker getEdgeLabelColorPicker() {
		return edgeLabelColorPicker;
	}

	public ChoiceBox<String> getEdgeLabelFontChoiceBox() {
		return edgeLabelFontChoiceBox;
	}

	public Button getClearEdgeLabelFontButton() {
		return clearEdgeLabelFontButton;
	}

	public Button getEdgeLabelItalicButton() {
		return edgeLabelItalicButton;
	}

	public ComboBox<Double> getEdgeLabelSizeCBox() {
		return edgeLabelSizeCBox;
	}

	public Button getEdgeLabelUnderlineButton() {
		return edgeLabelUnderlineButton;
	}

	public Button getClearEdgeLabelStyleButton() {
		return clearEdgeLabelStyleButton;
	}

	public ChoiceBox<LineType> getEdgeLineChoiceBox() {
		return edgeLineChoiceBox;
	}

	public Button getEdgeRectangularButton() {
		return edgeRectangularButton;
	}

	public Button getEdgeStraightButton() {
		return edgeStraightButton;
	}

	public ComboBox<Double> getEdgeWidthCBox() {
		return edgeWidthCBox;
	}

	public ColorPicker getNodeColorPicker() {
		return nodeColorPicker;
	}

	public Button getClearNodeColorButton() {
		return clearNodeColorButton;
	}

	public Button getNodeLabelBoldButton() {
		return nodeLabelBoldButton;
	}

	public ColorPicker getNodeLabelBackgroundColorPicker() {
		return nodeLabelBackgroundColorPicker;
	}

	public Button getClearNodeLabelBackgroundColorButton() {
		return clearNodeLabelBackgroundColorButton;
	}

	public ColorPicker getNodeLabelColorPicker() {
		return nodeLabelColorPicker;
	}

	public Button getClearNodeLabelColorButton() {
		return clearNodeLabelColorButton;
	}

	public ChoiceBox<String> getNodeLabelFontChoiceBox() {
		return nodeLabelFontChoiceBox;
	}

	public Button getClearNodeLabelFontButton() {
		return clearNodeLabelFontButton;
	}

	public ComboBox<Double> getNodeLabelSizeCBox() {
		return nodeLabelSizeCBox;
	}

	public Button getNodeLabelItalicButton() {
		return nodeLabelItalicButton;
	}

	public Button getClearNodeLabelStyleButton() {
		return clearNodeLabelStyleButton;
	}

	public Button getNodeLabelUnderlineButton() {
		return nodeLabelUnderlineButton;
	}

	public ChoiceBox<?> getNodeShapeChoiceBox() {
		return nodeShapeChoiceBox;
	}

	public ComboBox<Double> getNodeSizeCBox() {
		return nodeSizeCBox;
	}

	public VBox getRootPane() {
		return rootPane;
	}

	public Button getSmoothButton() {
		return smoothButton;
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public ToggleButton getShowWeightToggleButton() {
		return showWeightToggleButton;
	}

	public ToggleButton getShowSupportToggleButton() {
		return showSupportToggleButton;
	}

	public ToggleButton getShowProbabilityToggleButton() {
		return showProbabilityToggleButton;
	}

	public TextField getEdgeWeightTextField() {
		return edgeWeightTextField;
	}

	public TextField getEdgeSupportTextField() {
		return edgeSupportTextField;
	}

	public TextField getEdgeProbabilityTextField() {
		return edgeProbabilityTextField;
	}

	public Button getMeasureEdgeWeightsButton() {
		return measureEdgeWeightsButton;
	}

	public ToggleButton getShowArrowsButton() {
		return showArrowsButton;
	}

	public ChoiceBox<String> getHowToLabelNodesCBox() {
		return howToLabelNodesCBox;
	}

	public ChoiceBox<String> getUseNodesToLabelCBox() {
		return useNodesToLabelCBox;
	}

	public ToggleButton getUniqueLabelsToggleButton() {
		return uniqueLabelsToggleButton;
	}

	public TextField getNodeLabelTextField() {
		return nodeLabelTextField;
	}

	public Button getClearEdgeColorButton() {
		return clearEdgeColorButton;
	}

	public Button getClearEdgeLabelBackgroundColorButton() {
		return clearEdgeLabelBackgroundColorButton;
	}

	public Button getClearEdgeLabelColorButton() {
		return clearEdgeLabelColorButton;
	}
}

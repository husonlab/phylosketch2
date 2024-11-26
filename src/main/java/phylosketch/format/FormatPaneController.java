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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.BasicFX;
import jloda.util.Counter;
import phylosketch.view.LineType;

public class FormatPaneController {

	@FXML
	private Button closeButton;

	@FXML
	private ColorPicker edgeColorPicker;

	@FXML
	private ToggleButton showArrowsButton;

	@FXML
	private Button edgeCurvedButton;

	@FXML
	private ColorPicker edgeLabelBackgroundColorPicker;

	@FXML
	private Button edgeLabelBoldButton;

	@FXML
	private ColorPicker edgeLabelColorPicker;

	@FXML
	private ChoiceBox<String> edgeLabelFontChoiceBox;

	@FXML
	private Button edgeLabelItalicButton;

	@FXML
	private ChoiceBox<Double> edgeLabelSizeChoiceBox;

	@FXML
	private Button edgeLabelStrikeButton;

	@FXML
	private Button edgeLabelUnderlineButton;

	@FXML
	private Accordion edgeLabelStyleAccordion;

	@FXML
	private ChoiceBox<LineType> edgeLineChoiceBox;

	@FXML
	private Button edgeRectangularButton;

	@FXML
	private Button edgeStraightButton;

	@FXML
	private ChoiceBox<Double> edgeWidthChoiceBox;

	@FXML
	private Accordion edgeStyleAccordion;

	@FXML
	private Accordion edgeValuesAccordion;

	@FXML
	private ToggleButton showWeightToggleButton;

	@FXML
	private ToggleButton showSupportToggleButton;

	@FXML
	private ToggleButton showProbabilityToggleButton;


	@FXML
	private ColorPicker nodeColorPicker;

	@FXML
	private Button nodeLabelBoldButton;

	@FXML
	private ColorPicker nodeLabelBackgroundColorPicker;

	@FXML
	private ColorPicker nodeLabelColorPicker;

	@FXML
	private ChoiceBox<String> nodeLabelFontChoiceBox;

	@FXML
	private ChoiceBox<Double> nodeLabelSizeChoiceBox;

	@FXML
	private Button nodeLabelItalicButton;

	@FXML
	private Button nodeLabelStrikeButton;

	@FXML
	private Button nodeLabelUnderlineButton;

	@FXML
	private Accordion nodeLabelStyleAccordion;

	@FXML
	private ChoiceBox<?> nodeShapeChoiceBox;

	@FXML
	private ChoiceBox<Double> nodeSizeChoiceBox;

	@FXML
	private Accordion nodeStyleAccordion;

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
	private Accordion nodeLabelsAccordion;

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
		MaterialIcons.setIcon(nodeLabelStrikeButton, MaterialIcons.format_strikethrough);

		MaterialIcons.setIcon(edgeRectangularButton, MaterialIcons.keyboard_return, "-fx-scale-x: -1;-fx-scale-y: -1;", true);
		MaterialIcons.setIcon(edgeStraightButton, MaterialIcons.arrow_right_alt);
		MaterialIcons.setIcon(edgeCurvedButton, MaterialIcons.redo, "-fx-scale-y: -1;", true);

		MaterialIcons.setIcon(edgeLabelBoldButton, MaterialIcons.format_bold);
		MaterialIcons.setIcon(edgeLabelItalicButton, MaterialIcons.format_italic);
		MaterialIcons.setIcon(edgeLabelUnderlineButton, MaterialIcons.format_underline);
		MaterialIcons.setIcon(edgeLabelStrikeButton, MaterialIcons.format_strikethrough);

		MaterialIcons.setIcon(closeButton, MaterialIcons.close);

		MaterialIcons.setIcon(measureEdgeWeightsButton, MaterialIcons.square_foot);
		MaterialIcons.setIcon(showWeightToggleButton, MaterialIcons.visibility);
		MaterialIcons.setIcon(showSupportToggleButton, MaterialIcons.visibility);
		MaterialIcons.setIcon(showProbabilityToggleButton, MaterialIcons.visibility);

		MaterialIcons.setIcon(uniqueLabelsToggleButton, MaterialIcons.flash_on);


		var accordions = BasicFX.getAllRecursively(rootPane, Accordion.class).toArray(new Accordion[0]);
		for (var accordion : accordions) {
			accordion.expandedPaneProperty().addListener(e -> adjustVBoxHeight(rootPane, accordions));
		}
		adjustVBoxHeight(rootPane);

		nodeLabelSizeChoiceBox.getItems().addAll(6.0, 8.0, 10.0, 12.0, 14.0, 18.0, 24.0, 48.0);
		edgeLabelSizeChoiceBox.getItems().addAll(6.0, 8.0, 10.0, 12.0, 14.0, 18.0, 24.0, 48.0);

		nodeSizeChoiceBox.getItems().addAll(1.0, 2.0, 4.0, 6.0, 10.0, 16.0, 24.0);
		edgeWidthChoiceBox.getItems().addAll(1.0, 2.0, 4.0, 6.0, 10.0, 16.0, 24.0);

		getEdgeLineChoiceBox().getItems().addAll(LineType.values());
		getEdgeLineChoiceBox().setValue(LineType.Solid);

		nodeLabelFontChoiceBox.getItems().addAll("", "Serif", "SansSerif", "Monospaced");
		edgeLabelFontChoiceBox.getItems().addAll("", "Serif", "SansSerif", "Monospaced");

		edgeWeightTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
		edgeSupportTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
		edgeProbabilityTextField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));

		howToLabelNodesCBox.getItems().addAll("A,B,C", "a,b,c", "t1,2,3", "x1,2,3", "None");
		howToLabelNodesCBox.setValue(null);
		useNodesToLabelCBox.getItems().addAll("Leaves", "Internal", "All");
		useNodesToLabelCBox.setValue(null);

		uniqueLabelsToggleButton.setSelected(true);
	}

	private void adjustVBoxHeight(VBox vbox, Accordion... accordions) {
		if (accordions.length > 0) {
			var totalHeight = new Counter(0);

			var last = accordions[accordions.length - 1];

			for (var accordion : accordions) {
				var expandedPane = accordion.getExpandedPane();
				if (expandedPane != null) {
					totalHeight.add((long) Math.ceil(expandedPane.getHeight()));
				}
				totalHeight.add((long) Math.ceil(accordion.getHeight()));
			}
			Platform.runLater(() -> vbox.setPrefHeight(totalHeight.get()));
		}
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

	public Button getEdgeLabelItalicButton() {
		return edgeLabelItalicButton;
	}

	public ChoiceBox<Double> getEdgeLabelSizeChoiceBox() {
		return edgeLabelSizeChoiceBox;
	}

	public Button getEdgeLabelStrikeButton() {
		return edgeLabelStrikeButton;
	}

	public Button getEdgeLabelUnderlineButton() {
		return edgeLabelUnderlineButton;
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

	public ChoiceBox<Double> getEdgeWidthChoiceBox() {
		return edgeWidthChoiceBox;
	}

	public ColorPicker getNodeColorPicker() {
		return nodeColorPicker;
	}

	public Button getNodeLabelBoldButton() {
		return nodeLabelBoldButton;
	}

	public ColorPicker getNodeLabelBackgroundColorPicker() {
		return nodeLabelBackgroundColorPicker;
	}

	public ColorPicker getNodeLabelColorPicker() {
		return nodeLabelColorPicker;
	}

	public ChoiceBox<String> getNodeLabelFontChoiceBox() {
		return nodeLabelFontChoiceBox;
	}

	public ChoiceBox<Double> getNodeLabelSizeChoiceBox() {
		return nodeLabelSizeChoiceBox;
	}

	public Button getNodeLabelItalicButton() {
		return nodeLabelItalicButton;
	}

	public Button getNodeLabelStrikeButton() {
		return nodeLabelStrikeButton;
	}

	public Button getNodeLabelUnderlineButton() {
		return nodeLabelUnderlineButton;
	}

	public ChoiceBox<?> getNodeShapeChoiceBox() {
		return nodeShapeChoiceBox;
	}

	public ChoiceBox<Double> getNodeSizeChoiceBox() {
		return nodeSizeChoiceBox;
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
}

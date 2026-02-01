/*
 * FormatPaneController.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import jloda.fx.control.EditableMenuButton;
import jloda.fx.icons.MaterialIcons;
import jloda.phylogeny.layout.LayoutRootedPhylogeny;
import phylosketch.view.LineType;
import phylosketch.view.NodeShape;

import java.util.List;

/**
 * format pane controller
 * Daniel Huson, 11.2024
 */
public class FormatPaneController {
	@FXML
	private ColorPicker edgeColorPicker;

	@FXML
	private Button clearEdgeColorButton;

	@FXML
	private Button showArrowsButton;

	@FXML
	private Button showReticulateButton;

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
	private MenuButton edgeLabelSizeMenuButton;

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
	private MenuButton edgeWidthMenuButton;

	@FXML
	private ToggleButton showWeightToggleButton;

	@FXML
	private ToggleButton showSupportToggleButton;

	@FXML
	private ToggleButton showProbabilityToggleButton;

	@FXML
	private ColorPicker nodeFillPicker;

	@FXML
	private Button clearNodeFillButton;

	@FXML
	private ColorPicker nodeStrokePicker;

	@FXML
	private Button clearNodeStrokeButton;

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
	private MenuButton nodeLabelSizeMenuButton;

	@FXML
	private Button nodeLabelItalicButton;

	@FXML
	private Button clearNodeLabelStyleButton;

	@FXML
	private Button nodeLabelUnderlineButton;

	@FXML
	private ChoiceBox<NodeShape.Type> nodeShapeChoiceBox;

	@FXML
	private MenuButton nodeSizeMenuButton;

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
	private Button declareRootButton;

	@FXML
	private Button mergeNodesButton;

	@FXML
	private Button deleteThruNodesButton;

	@FXML
	private Button reverseEdgesButton;

	@FXML
	private Button crossEdgesButton;

	@FXML
	private Button deleteButton;

	@FXML
	private Button transferAcceptorButton;

	@FXML
	private Button induceButton;

	@FXML
	private ChoiceBox<LayoutRootedPhylogeny.Layout> layoutCBox;

	@FXML
	private ChoiceBox<LayoutRootedPhylogeny.Scaling> scalingCBox;

	@FXML
	private Button applyLayoutPhylogenyButton;

	@FXML
	private Button verticalFlipButton;

	@FXML
	private Button horizontalFlipButton;

	@FXML
	private Button rotateLeftButton;

	@FXML
	private Button rotateRightButton;

	@FXML
	private Button layoutLabelsButton;

	@FXML
	private ToggleButton resizeModeButton;

	@FXML
	private Button swapSidesButton;

	@FXML
	private TitledPane nodeStylePane;
	@FXML
	private TitledPane nodeLabelsPane;
	@FXML
	private TitledPane nodeLabelsStylePane;
	@FXML
	private TitledPane edgeStylePane;
	@FXML
	private TitledPane edgeLabelsPane;
	@FXML
	private TitledPane edgeLabelStylePane;
	@FXML
	private TitledPane layoutPane;
	@FXML
	private TitledPane transformPane;
	@FXML
	private TitledPane editPane;

	@FXML
	private Button zoomInButton;

	@FXML
	private Button zoomOutButton;

	@FXML
	private VBox vBox;

	private final DoubleProperty nodeSize = new SimpleDoubleProperty(this, "nodeSize", 1.0);
	private final DoubleProperty nodeLabelSize = new SimpleDoubleProperty(this, "nodeLabelSize", 1.0);

	private final DoubleProperty edgeWidth = new SimpleDoubleProperty(this, "edgeWidth", 1.0);
	private final DoubleProperty edgeLabelSize = new SimpleDoubleProperty(this, "edgeLabelSize", 1.0);

	@FXML
	private void initialize() {
		Platform.runLater(() -> {
			//MaterialIcons.setIcon(nodeLabelBoldButton, MaterialIcons.format_bold);
			//MaterialIcons.setIcon(nodeLabelItalicButton, MaterialIcons.format_italic);
			//MaterialIcons.setIcon(nodeLabelUnderlineButton, MaterialIcons.format_underline);

			MaterialIcons.setIcon(edgeRectangularButton, MaterialIcons.keyboard_return, "-fx-scale-x: -1;-fx-scale-y: -1;", true);
			MaterialIcons.setIcon(edgeStraightButton, MaterialIcons.arrow_right_alt);
			MaterialIcons.setIcon(edgeCurvedButton, MaterialIcons.redo, "-fx-scale-y: -1;", true);

			//MaterialIcons.setIcon(edgeLabelBoldButton, MaterialIcons.format_bold);
			//MaterialIcons.setIcon(edgeLabelItalicButton, MaterialIcons.format_italic);
			//MaterialIcons.setIcon(edgeLabelUnderlineButton, MaterialIcons.format_underline);

			MaterialIcons.setIcon(swapSidesButton, MaterialIcons.import_export, "-fx-rotate: 90;", true);
			swapSidesButton.setPrefHeight(18);

			MaterialIcons.setIcon(deleteButton, MaterialIcons.backspace, "", false);

			MaterialIcons.setIcon(measureEdgeWeightsButton, MaterialIcons.square_foot);
			MaterialIcons.setIcon(showWeightToggleButton, MaterialIcons.visibility);
			MaterialIcons.setIcon(showSupportToggleButton, MaterialIcons.visibility);
			MaterialIcons.setIcon(showProbabilityToggleButton, MaterialIcons.visibility);

			MaterialIcons.setIcon(uniqueLabelsToggleButton, MaterialIcons.flash_on);

			MaterialIcons.setIcon(clearNodeFillButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearNodeStrokeButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearNodeLabelFontButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearNodeLabelStyleButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearNodeLabelColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearNodeLabelBackgroundColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearEdgeColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearEdgeLabelFontButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearEdgeLabelStyleButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearEdgeLabelColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
			MaterialIcons.setIcon(clearEdgeLabelBackgroundColorButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);

			MaterialIcons.setIcon(applyLayoutPhylogenyButton, MaterialIcons.play_circle);

			MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in, "", false);
			MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out, "", false);
			MaterialIcons.setIcon(verticalFlipButton, MaterialIcons.swap_vert, "", false);
			MaterialIcons.setIcon(horizontalFlipButton, MaterialIcons.swap_horiz, "", false);
			MaterialIcons.setIcon(rotateLeftButton, MaterialIcons.rotate_left, "", false);
			MaterialIcons.setIcon(rotateRightButton, MaterialIcons.rotate_right, "", false);
			MaterialIcons.setIcon(layoutLabelsButton, MaterialIcons.text_rotation_none, "", false);
			MaterialIcons.setIcon(resizeModeButton, MaterialIcons.grid_goldenratio, "", false);
		});

		EditableMenuButton.setup(nodeSizeMenuButton, List.of("1", "2", "3", "4", "5", "8", "10", "16", "24"), true, nodeSize);
		EditableMenuButton.setup(nodeLabelSizeMenuButton, List.of("6", "8", "10", "12", "14", "18", "24", "48"), true, nodeLabelSize);
		EditableMenuButton.setup(edgeLabelSizeMenuButton, List.of("6", "8", "10", "12", "14", "18", "24", "48"), true, edgeLabelSize);
		EditableMenuButton.setup(edgeWidthMenuButton, List.of("1", "2", "3", "4", "5", "8", "10", "16", "24"), true, edgeWidth);

		edgeLineChoiceBox.getItems().addAll(LineType.values());
		edgeLineChoiceBox.setValue(LineType.Solid);

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

		layoutCBox.getItems().addAll(LayoutRootedPhylogeny.Layout.values());
		scalingCBox.getItems().addAll(LayoutRootedPhylogeny.Scaling.values());

		nodeShapeChoiceBox.getItems().addAll(NodeShape.Type.values());
		nodeShapeChoiceBox.setValue(NodeShape.Type.Circle);
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

	public MenuButton getEdgeLabelSizeMenuButton() {
		return edgeLabelSizeMenuButton;
	}

	public Button getEdgeLabelUnderlineButton() {
		return edgeLabelUnderlineButton;
	}

	public Button getClearEdgeLabelStyleButton() {
		return clearEdgeLabelStyleButton;
	}


	public Button getEdgeRectangularButton() {
		return edgeRectangularButton;
	}

	public Button getEdgeStraightButton() {
		return edgeStraightButton;
	}


	public ColorPicker getNodeFillPicker() {
		return nodeFillPicker;
	}

	public Button getClearNodeFillButton() {
		return clearNodeFillButton;
	}

	public ColorPicker getNodeStrokePicker() {
		return nodeStrokePicker;
	}

	public Button getClearNodeStrokeButton() {
		return clearNodeStrokeButton;
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

	public Button getNodeLabelItalicButton() {
		return nodeLabelItalicButton;
	}

	public Button getClearNodeLabelStyleButton() {
		return clearNodeLabelStyleButton;
	}

	public Button getNodeLabelUnderlineButton() {
		return nodeLabelUnderlineButton;
	}

	public ChoiceBox<NodeShape.Type> getNodeShapeChoiceBox() {
		return nodeShapeChoiceBox;
	}

	public MenuButton getNodeSizeMenuButton() {
		return nodeSizeMenuButton;
	}

	public Button getSmoothButton() {
		return smoothButton;
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

	public Button getShowArrowsButton() {
		return showArrowsButton;
	}

	public Button getShowReticulateButton() {
		return showReticulateButton;
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

	public Button getDeclareRootButton() {
		return declareRootButton;
	}

	public Button getMergeNodesButton() {
		return mergeNodesButton;
	}

	public Button getDeleteThruNodesButton() {
		return deleteThruNodesButton;
	}

	public Button getReverseEdgesButton() {
		return reverseEdgesButton;
	}

	public Button getTransferAcceptorButton() {
		return transferAcceptorButton;
	}

	public Button getCrossEdgesButton() {
		return crossEdgesButton;
	}

	public Button getInduceButton() {
		return induceButton;
	}

	public ChoiceBox<LayoutRootedPhylogeny.Layout> getLayoutCBox() {
		return layoutCBox;
	}

	public ChoiceBox<LayoutRootedPhylogeny.Scaling> getScalingCBox() {
		return scalingCBox;
	}

	public Button getApplyLayoutPhylogenyButton() {
		return applyLayoutPhylogenyButton;
	}

	public Button getVerticalFlipButton() {
		return verticalFlipButton;
	}

	public Button getHorizontalFlipButton() {
		return horizontalFlipButton;
	}

	public Button getRotateLeftButton() {
		return rotateLeftButton;
	}

	public Button getRotateRightButton() {
		return rotateRightButton;
	}

	public Button getLayoutLabelsButton() {
		return layoutLabelsButton;
	}

	public ToggleButton getResizeModeButton() {
		return resizeModeButton;
	}

	public Button getSwapSidesButton() {
		return swapSidesButton;
	}

	public Button getDeleteButton() {
		return deleteButton;
	}

	public TitledPane getNodeStylePane() {
		return nodeStylePane;
	}

	public TitledPane getNodeLabelsPane() {
		return nodeLabelsPane;
	}

	public TitledPane getNodeLabelsStylePane() {
		return nodeLabelsStylePane;
	}

	public TitledPane getEdgeStylePane() {
		return edgeStylePane;
	}

	public TitledPane getEdgeLabelsPane() {
		return edgeLabelsPane;
	}

	public TitledPane getEdgeLabelStylePane() {
		return edgeLabelStylePane;
	}

	public TitledPane getLayoutPane() {
		return layoutPane;
	}

	public TitledPane getTransformPane() {
		return transformPane;
	}

	public TitledPane getEditPane() {
		return editPane;
	}

	public Button getZoomInButton() {
		return zoomInButton;
	}

	public Button getZoomOutButton() {
		return zoomOutButton;
	}

	public ChoiceBox<LineType> getEdgeLineChoiceBox() {
		return edgeLineChoiceBox;
	}

	public double getNodeLabelSize() {
		return nodeLabelSize.get();
	}

	public DoubleProperty nodeLabelSizeProperty() {
		return nodeLabelSize;
	}

	public double getEdgeWidth() {
		return edgeWidth.get();
	}

	public DoubleProperty edgeWidthProperty() {
		return edgeWidth;
	}

	public double getEdgeLabelSize() {
		return edgeLabelSize.get();
	}

	public DoubleProperty edgeLabelSizeProperty() {
		return edgeLabelSize;
	}

	public double getNodeSize() {
		return nodeSize.get();
	}

	public DoubleProperty nodeSizeProperty() {
		return nodeSize;
	}
}

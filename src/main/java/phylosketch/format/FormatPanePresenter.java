/*
 * FormatPanePresenter.java Copyright (C) 2024 Daniel H. Huson
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
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import jloda.fx.undo.CompositeCommand;
import jloda.util.NumberUtils;
import phylosketch.commands.*;
import phylosketch.view.DrawPane;

import java.util.Objects;

public class FormatPanePresenter {
	private boolean canUpdate = true;

	public FormatPanePresenter(DrawPane view, FormatPaneController controller, BooleanProperty show) {
		controller.getCloseButton().setOnAction(e -> show.set(false));


		controller.getHowToLabelNodesCBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				if (controller.getHowToLabelNodesCBox().getValue() != null && controller.getUseNodesToLabelCBox().getValue() != null) {
					view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, controller.getHowToLabelNodesCBox().getValue(), controller.getUseNodesToLabelCBox().getValue(), controller.getUniqueLabelsToggleButton().isSelected()));
				}
			}
		});
		controller.getUseNodesToLabelCBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				if (controller.getHowToLabelNodesCBox().getValue() != null && controller.getUseNodesToLabelCBox().getValue() != null) {
					view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, controller.getHowToLabelNodesCBox().getValue(), controller.getUseNodesToLabelCBox().getValue(), controller.getUniqueLabelsToggleButton().isSelected()));
				}
			}
		});

		controller.getNodeLabelTextField().setFocusTraversable(false);
		controller.getNodeLabelTextField().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, controller.getNodeLabelTextField().getText()));
			}
		});
		setupTriggerOnEnter(controller.getNodeLabelTextField());
		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e -> {
			String label = null;
			for (var v : view.getNodeSelection().getSelectedItems()) {
				if (label == null)
					label = view.getLabel(v).getText();
				else if (!label.equals(view.getLabel(v).getText())) {
					label = null;
					break;
				}
			}
			var finalLabel = label;
			Platform.runLater(() -> controller.getNodeLabelTextField().setText(finalLabel));
		});


		controller.getNodeLabelFontChoiceBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.font, null, null, n));
			}
		});
		controller.getClearNodeLabelFontButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.font, null, null, null));
			}
		});

		controller.getNodeLabelBoldButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(view::getLabel).anyMatch(t -> t != null && !t.isBold());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.bold, select, null, null));
			}
		});
		controller.getNodeLabelItalicButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(view::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isItalic());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.italic, select, null, null));
			}
		});
		controller.getNodeLabelUnderlineButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(view::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isUnderline());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.underlined, select, null, null));
			}
		});

		controller.getClearNodeLabelStyleButton().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new NodeLabelsClearStyleCommand(view));
		});

		controller.getNodeLabelSizeChoiceBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null) {
					view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, NodeLabelFormatCommand.Which.size, null, n, null));
				}
			}
		});

		controller.getNodeSizeChoiceBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null) {
					view.getUndoManager().doAndAdd(new NodeSizeCommand(view, n));
				}
			}
		});

		controller.getNodeColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.fill, n));
			}
		});

		controller.getClearNodeColorButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getNodeColorPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.fill, null));
			}
		});

		controller.getNodeLabelColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelColorCommand(view, NodeLabelColorCommand.Which.textFill, n));
			}
		});

		controller.getClearNodeLabelColorButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getNodeLabelColorPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new NodeLabelColorCommand(view, NodeLabelColorCommand.Which.textFill, null));
			}
		});

		controller.getNodeLabelBackgroundColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelColorCommand(view, NodeLabelColorCommand.Which.background, n));
			}
		});
		controller.getClearNodeLabelBackgroundColorButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getNodeLabelBackgroundColorPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new NodeLabelColorCommand(view, NodeLabelColorCommand.Which.background, null));
			}
		});

		{
			InvalidationListener invalidationListener = e -> {
				canUpdate = false;
				try {
					controller.getNodeLabelTextField().setText(null);
					controller.getNodeSizeChoiceBox().setValue(null);
					controller.getNodeColorPicker().setValue(null);
					controller.getNodeShapeChoiceBox().setValue(null);
					controller.getNodeLabelSizeChoiceBox().setValue(null);
					controller.getNodeLabelColorPicker().setValue(null);
					controller.getNodeLabelBackgroundColorPicker().setValue(null);
					controller.getNodeLabelFontChoiceBox().setValue(null);
					controller.getNodeLabelSizeChoiceBox().setValue(null);
					controller.getHowToLabelNodesCBox().setValue(null);
					controller.getUseNodesToLabelCBox().setValue(null);
				} finally {
					canUpdate = true;
				}
			};
			view.getNodeSelection().getSelectedItems().addListener(invalidationListener);
			view.getGraphFX().getNodeList().addListener(invalidationListener);
		}

		{
			InvalidationListener invalidationListener = e -> {
				canUpdate = false;
				try {
					controller.getEdgeWidthChoiceBox().setValue(null);
					controller.getEdgeColorPicker().setValue(null);
					controller.getEdgeLineChoiceBox().setValue(null);
					controller.getEdgeLabelSizeChoiceBox().setValue(null);
					controller.getEdgeLabelColorPicker().setValue(null);
					controller.getEdgeLabelBackgroundColorPicker().setValue(null);
					controller.getEdgeLabelFontChoiceBox().setValue(null);
					controller.getEdgeLabelSizeChoiceBox().setValue(null);
					controller.getEdgeWeightTextField().setText(null);
					controller.getEdgeSupportTextField().setText(null);
					controller.getEdgeProbabilityTextField().setText(null);
					if (false) {
						controller.getShowWeightToggleButton().setSelected(false);
						controller.getShowSupportToggleButton().setSelected(false);
						controller.getShowProbabilityToggleButton().setSelected(false);
					}
				} finally {
					canUpdate = true;
				}
			};
			view.getEdgeSelection().getSelectedItems().addListener(invalidationListener);
			view.getGraphFX().getEdgeList().addListener(invalidationListener);
		}

		controller.getShowWeightToggleButton().selectedProperty().addListener((v, o, n) -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new ShowEdgeValueCommand(view, n, null, null));
		});

		controller.getShowSupportToggleButton().selectedProperty().addListener((v, o, n) -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new ShowEdgeValueCommand(view, null, n, null));
		});

		controller.getShowProbabilityToggleButton().selectedProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new ShowEdgeValueCommand(view, null, null, n));
			}
		});

		controller.getEdgeLabelFontChoiceBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.font, null, null, n));
			}
		});
		controller.getClearEdgeLabelFontButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.font, null, null, null));
			}
		});


		controller.getEdgeLabelBoldButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllEdges().stream().map(view::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isBold());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.bold, select, null, null));
			}
		});
		controller.getEdgeLabelItalicButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllEdges().stream().map(view::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isItalic());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.italic, select, null, null));
			}
		});
		controller.getEdgeLabelUnderlineButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllEdges().stream().map(view::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isUnderline());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.underlined, select, null, null));
			}
		});

		controller.getEdgeLabelSizeChoiceBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null)
					view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.size, null, n, null));
			}
		});

		controller.getClearEdgeLabelStyleButton().setOnAction(e -> {
			view.getUndoManager().doAndAdd(new EdgeLabelsClearStyleCommand(view));
		});

		controller.getEdgeWidthChoiceBox().valueProperty().addListener((var, o, n) -> {
			if (n != null)
				view.getUndoManager().doAndAdd(new EdgeWidthCommand(view, n));
		});

		controller.getEdgeColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeColorCommand(view, n));
			}
		});

		controller.getClearEdgeColorButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeColorCommand(view, null));
			}
		});

		controller.getEdgeLineChoiceBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				if (n != null)
					view.getUndoManager().doAndAdd(new EdgeLineTypeCommand(view, n.strokeDashArray.toArray(new Double[0])));
			}
		});

		controller.getSmoothButton().setOnAction(a -> view.getUndoManager().doAndAdd(new SmoothCommand(view.getGraph(), view.getSelectedOrAllEdges())));
		controller.getSmoothButton().disableProperty().bind(Bindings.isEmpty(view.getGraphFX().getEdgeList()));

		controller.getEdgeStraightButton().setOnAction(e -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new StraightenCommand(view.getGraph(), view.getSelectedOrAllEdges()));
		});
		controller.getEdgeStraightButton().disableProperty().bind(controller.getSmoothButton().disableProperty());

		controller.getEdgeCurvedButton().setOnAction(e ->
		{
			if (canUpdate)
				view.getUndoManager().doAndAdd(new QuadraticCurveCommand(view.getGraph(), view.getSelectedOrAllEdges()));
		});
		controller.getEdgeCurvedButton().disableProperty().bind(controller.getSmoothButton().disableProperty());

		controller.getEdgeRectangularButton().setOnAction(e -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new RectangularCommand(view.getGraph(), view.getSelectedOrAllEdges()));
		});
		controller.getEdgeRectangularButton().disableProperty().bind(controller.getSmoothButton().disableProperty());

		controller.getShowArrowsButton().setOnAction(a -> {
			if (canUpdate) {
				var select = !view.getSelectedOrAllEdges().stream().allMatch(view::isShowArrow);
				controller.getShowArrowsButton().setSelected(select);
				view.getUndoManager().doAndAdd(new ShowArrowsCommand(view, select));
			}
		});

		var disableSetEdges = new SimpleBooleanProperty();
		disableSetEdges.bind(Bindings.isEmpty(view.getGraphFX().getEdgeList()));

		controller.getMeasureEdgeWeightsButton().setOnAction(e -> {
			if (!controller.getShowWeightToggleButton().isSelected())
				controller.getShowWeightToggleButton().fire();
			view.getUndoManager().doAndAdd(new CompositeCommand("set weights", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, -1),
					new ShowEdgeValueCommand(view, true, null, null)));
		});
		controller.getMeasureEdgeWeightsButton().disableProperty().bind(disableSetEdges);

		controller.getEdgeWeightTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeWeightTextField().getText())) {
				if (!controller.getShowWeightToggleButton().isSelected())
					controller.getShowWeightToggleButton().fire();
				var value = NumberUtils.parseDouble(controller.getEdgeWeightTextField().getText());
				view.getUndoManager().doAndAdd(new CompositeCommand("weights", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, value),
						new ShowEdgeValueCommand(view, true, null, null)));
			}
		});

		setupTriggerOnEnter(controller.getEdgeWeightTextField());
		controller.getEdgeWeightTextField().disableProperty().bind(disableSetEdges);

		controller.getEdgeSupportTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeSupportTextField().getText())) {
				if (!controller.getShowSupportToggleButton().isSelected())
					controller.getShowSupportToggleButton().fire();
				var value = NumberUtils.parseDouble(controller.getEdgeSupportTextField().getText());
				view.getUndoManager().doAndAdd(new CompositeCommand("set support", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Confidence, value),
						new ShowEdgeValueCommand(view, null, true, null)));
			}
		});
		setupTriggerOnEnter(controller.getEdgeSupportTextField());
		controller.getEdgeSupportTextField().disableProperty().bind(disableSetEdges);

		controller.getEdgeProbabilityTextField().setOnAction(a -> {
			if (NumberUtils.isDouble(controller.getEdgeProbabilityTextField().getText())) {
				if (!controller.getShowProbabilityToggleButton().isSelected())
					controller.getShowProbabilityToggleButton().fire();
				var value = NumberUtils.parseDouble(controller.getEdgeProbabilityTextField().getText());
				view.getUndoManager().doAndAdd(new CompositeCommand("set probabilities", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Probability, value),
						new ShowEdgeValueCommand(view, null, null, true)));
			}
		});
		setupTriggerOnEnter(controller.getEdgeProbabilityTextField());
		controller.getEdgeProbabilityTextField().disableProperty().bind(disableSetEdges);

		controller.getEdgeLabelColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeLabelColorCommand(view, EdgeLabelColorCommand.Which.textFill, n));
			}
		});

		controller.getClearEdgeLabelColorButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getEdgeLabelColorPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new EdgeLabelColorCommand(view, EdgeLabelColorCommand.Which.textFill, null));
			}
		});

		controller.getEdgeLabelBackgroundColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeLabelColorCommand(view, EdgeLabelColorCommand.Which.background, n));
			}
		});
		controller.getClearEdgeLabelBackgroundColorButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getEdgeLabelBackgroundColorPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new EdgeLabelColorCommand(view, EdgeLabelColorCommand.Which.background, null));
			}
		});
	}

	public static void setupTriggerOnEnter(TextField textField) {
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				event.consume();
				textField.getOnAction().handle(null);
			}
		});
	}


}
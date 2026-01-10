/*
 * FormatPanePresenter.java Copyright (C) 2025 Daniel H. Huson
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
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.CompositeCommand;
import jloda.fx.util.BasicFX;
import jloda.fx.util.RunAfterAWhile;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import phylosketch.commands.*;
import phylosketch.view.DrawView;
import phylosketch.view.LineType;
import phylosketch.view.NodeShape;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * format pane presenter
 * Daniel Huson, 11.2024
 */
public class FormatPanePresenter {
	private boolean canUpdate = true;

	public FormatPanePresenter(DrawView view, FormatPaneController controller, BooleanProperty show) {
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
				view.getUndoManager().doAndAdd(new SetNodeLabelsCommand(view, null, view.getSelectedOrAllNodes(), controller.getNodeLabelTextField().getText()));
			}
		});
		setupTriggerOnEnter(controller.getNodeLabelTextField());

		view.getNodeSelection().getSelectedItems().addListener((InvalidationListener) e -> {
			if (canUpdate) {
				String label = null;
				for (var v : view.getNodeSelection().getSelectedItems()) {
					if (label == null)
						label = DrawView.getLabel(v).getText();
					else if (!label.equals(DrawView.getLabel(v).getText())) {
						label = null;
						break;
					}

				}
				var finalLabel = label;
				Platform.runLater(() -> controller.getNodeLabelTextField().setText(finalLabel));
			}
		});

		controller.getNodeLabelFontChoiceBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.font, null, null, n));
			}
		});
		controller.getClearNodeLabelFontButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.font, null, null, null));
			}
		});

		controller.getNodeLabelBoldButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(DrawView::getLabel).anyMatch(t -> t != null && !t.isBold());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.bold, select, null, null));
			}
		});
		controller.getNodeLabelItalicButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(DrawView::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isItalic());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.italic, select, null, null));
			}
		});
		controller.getNodeLabelUnderlineButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllNodes().stream().map(DrawView::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isUnderline());
				view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.underlined, select, null, null));
			}
		});

		controller.getClearNodeLabelStyleButton().setOnAction(e -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeLabelsClearStyleCommand(view));
			}
		});

		controller.getNodeLabelSizeCBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null) {
					view.getUndoManager().doAndAdd(new NodeLabelFormatCommand(view, view.getSelectedOrAllNodes(), NodeLabelFormatCommand.Which.size, null, n, null));
				}
			}
		});

		controller.getNodeSizeCBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null) {
					view.getUndoManager().doAndAdd(new NodeSizeCommand(view, n));
				}
			}
		});

		controller.getNodeShapeChoiceBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null) {
					view.getUndoManager().doAndAdd(new NodeShapeCommand(view, n));
				}
			}
		});

		controller.getNodeFillPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.fill, n));
			}
		});

		controller.getClearNodeFillButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getNodeFillPicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.fill, null));
			}
		});

		controller.getNodeStrokePicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.stroke, n));
			}
		});

		controller.getClearNodeStrokeButton().setOnAction(a -> {
			if (canUpdate) {
				canUpdate = false;
				controller.getNodeStrokePicker().setValue(null);
				canUpdate = true;
				view.getUndoManager().doAndAdd(new NodeColorCommand(view, NodeColorCommand.Which.stroke, null));
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
			var object = new Object();
			InvalidationListener updateShowNodesListener = e -> {
				var nodeSize = exactlyOne(view.getSelectedOrAllNodes(), v -> (notNullOrElse(v, DrawView::getShape, () -> new NodeShape(NodeShape.Type.Circle))).getSize());
				var nodeShape = exactlyOne(view.getSelectedOrAllNodes(), v -> (notNullOrElse(v, DrawView::getShape, () -> new NodeShape(NodeShape.Type.Circle))));
				var fillColor = exactlyOne(view.getSelectedOrAllNodes(), v -> (Color) notNullOrElse(v, DrawView::getShape, () -> new NodeShape(NodeShape.Type.Circle)).getFill());
				var strokeColor = exactlyOne(view.getSelectedOrAllNodes(), v -> (Color) notNullOrElse(v, DrawView::getShape, () -> new NodeShape(NodeShape.Type.Circle)).getStroke());
				var label = exactlyOne(view.getSelectedOrAllNodes(), v -> notNullOrElse(v, DrawView::getLabel, RichTextLabel::new).getText());
				var labelFont = exactlyOne(view.getSelectedOrAllNodes(), v -> notNullOrElse(v, DrawView::getLabel, RichTextLabel::new).getFontFamily());
				var labelSize = exactlyOne(view.getSelectedOrAllNodes(), v -> notNullOrElse(v, DrawView::getLabel, RichTextLabel::new).getFontSize());
				var labelColor = exactlyOne(view.getSelectedOrAllNodes(), v -> (Color) notNullOrElse(v, DrawView::getLabel, RichTextLabel::new).getTextFill());
				var labelBackground = exactlyOne(view.getSelectedOrAllNodes(), v -> (Color) notNullOrElse(v, DrawView::getLabel, RichTextLabel::new).getBackgroundColor());
				RunAfterAWhile.applyInFXThread(object, () -> {
					canUpdate = false;
					try {
						controller.getNodeShapeChoiceBox().setValue(null);

						controller.getNodeShapeChoiceBox().setValue(nodeShape != null ? nodeShape.getType() : null);
						controller.getNodeSizeCBox().setValue(nodeSize);
						controller.getNodeFillPicker().setValue(fillColor);
						controller.getNodeStrokePicker().setValue(strokeColor);
						controller.getNodeLabelTextField().setText(label);
						controller.getNodeLabelFontChoiceBox().setValue(labelFont);
						controller.getNodeLabelSizeCBox().setValue(labelSize);
						controller.getNodeLabelColorPicker().setValue(labelColor);
						controller.getNodeLabelBackgroundColorPicker().setValue(labelBackground);
					} finally {
						canUpdate = true;
					}
				});
			};
			view.getNodeSelection().getSelectedItems().addListener(updateShowNodesListener);
		}

		{
			var object = new Object();
			InvalidationListener updateShowEdgesListener = a -> {
				var lineType = exactlyOne(view.getSelectedOrAllEdges(), e -> LineType.fromShape(notNullOrElse(e, DrawView::getPath, Path::new)));
				var edgeWidth = exactlyOne(view.getSelectedOrAllEdges(), e -> notNullOrElse(e, DrawView::getPath, Path::new).getStrokeWidth());
				var color = exactlyOne(view.getSelectedOrAllEdges(), e -> (Color) notNullOrElse(e, DrawView::getPath, Path::new).getFill());
				var weight = exactlyOne(view.getSelectedOrAllEdges(), e -> view.getGraph().hasEdgeWeights() ? view.getGraph().getWeight(e) : null);
				var support = exactlyOne(view.getSelectedOrAllEdges(), e -> view.getGraph().hasEdgeWeights() ? view.getGraph().getConfidence(e) : null);
				var probability = exactlyOne(view.getSelectedOrAllEdges(), e -> view.getGraph().hasEdgeWeights() ? view.getGraph().getProbability(e) : null);
				var labelFont = exactlyOne(view.getSelectedOrAllEdges(), e -> notNullOrElse(e, DrawView::getLabel, RichTextLabel::new).getFontFamily());
				var labelSize = exactlyOne(view.getSelectedOrAllEdges(), e -> notNullOrElse(e, DrawView::getLabel, RichTextLabel::new).getFontSize());
				var labelColor = exactlyOne(view.getSelectedOrAllEdges(), e -> (Color) notNullOrElse(e, DrawView::getLabel, RichTextLabel::new).getTextFill());
				var labelBackground = exactlyOne(view.getSelectedOrAllEdges(), e -> (Color) notNullOrElse(e, DrawView::getLabel, RichTextLabel::new).getBackgroundColor());
				RunAfterAWhile.applyInFXThread(object, () -> {
					canUpdate = false;
					try {
						controller.getEdgeLineChoiceBox().setValue(lineType);
						controller.getEdgeWidthCBox().setValue(edgeWidth);
						controller.getEdgeLabelFontChoiceBox().setValue(labelFont);
						controller.getEdgeLabelSizeCBox().setValue(labelSize);
						controller.getEdgeColorPicker().setValue(color);
						if (weight != null)
							controller.getEdgeWeightTextField().setText(StringUtils.removeTrailingZerosAfterDot(weight));
						if (support != null)
							controller.getEdgeSupportTextField().setText(StringUtils.removeTrailingZerosAfterDot(support));
						if (probability != null)
							controller.getEdgeProbabilityTextField().setText(StringUtils.removeTrailingZerosAfterDot(probability));
						controller.getEdgeLabelColorPicker().setValue(labelColor);
						controller.getEdgeLabelBackgroundColorPicker().setValue(labelBackground);
					} finally {
						canUpdate = true;
					}
				});
			};
			view.getEdgeSelection().getSelectedItems().addListener(updateShowEdgesListener);
			view.getGraphFX().getEdgeList().addListener(updateShowEdgesListener);
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
				var select = view.getSelectedOrAllEdges().stream().map(DrawView::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isBold());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.bold, select, null, null));
			}
		});
		controller.getEdgeLabelItalicButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllEdges().stream().map(DrawView::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isItalic());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.italic, select, null, null));
			}
		});
		controller.getEdgeLabelUnderlineButton().setOnAction(e -> {
			if (canUpdate) {
				var select = view.getSelectedOrAllEdges().stream().map(DrawView::getLabel).filter(Objects::nonNull).anyMatch(label -> !label.isUnderline());
				view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.underlined, select, null, null));
			}
		});

		controller.getEdgeLabelSizeCBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null)
					view.getUndoManager().doAndAdd(new EdgeLabelFormatCommand(view, EdgeLabelFormatCommand.Which.size, null, n, null));
			}
		});

		controller.getClearEdgeLabelStyleButton().setOnAction(e -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeLabelsClearStyleCommand(view));
			}
		});

		controller.getEdgeWidthCBox().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				if (n != null)
					view.getUndoManager().doAndAdd(new EdgeWidthCommand(view, n));
			}
		});

		controller.getEdgeColorPicker().valueProperty().addListener((var, o, n) -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeColorCommand(view, view.getSelectedOrAllEdges(), n));
			}
		});

		controller.getClearEdgeColorButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new EdgeColorCommand(view, view.getSelectedOrAllEdges(), null));
			}
		});

		controller.getEdgeLineChoiceBox().valueProperty().addListener((v, o, n) -> {
			if (canUpdate) {
				if (n != null)
					view.getUndoManager().doAndAdd(new EdgeLineTypeCommand(view, n.strokeDashArray.toArray(new Double[0])));
			}
		});

		controller.getSmoothButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new SmoothCommand(view, view.getSelectedOrAllEdges()));
			}
		});

		controller.getEdgeStraightButton().setOnAction(e -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new StraightenCommand(view.getGraph(), view.getSelectedOrAllEdges()));
		});

		controller.getEdgeCurvedButton().setOnAction(e ->
		{
			if (canUpdate)
				view.getUndoManager().doAndAdd(new QuadraticCurveCommand(view, view.getSelectedOrAllEdges()));
		});

		controller.getEdgeRectangularButton().setOnAction(e -> {
			if (canUpdate)
				view.getUndoManager().doAndAdd(new RectangularCommand(view, view.getSelectedOrAllEdges()));
		});

		controller.getShowArrowsButton().setOnAction(a -> {
			if (canUpdate) {
				var select = !view.getSelectedOrAllEdges().stream().allMatch(view::isShowArrow);
				view.getUndoManager().doAndAdd(new ShowArrowsCommand(view, view.getSelectedOrAllEdges(), select));
			}
		});

		controller.getShowReticulateButton().setOnAction(a -> {
			if (canUpdate) {
				view.getUndoManager().doAndAdd(new ToggleReticulateEdgeCommand(view));
			}
		});

		controller.getMeasureEdgeWeightsButton().setOnAction(e -> {
			if (canUpdate) {
				if (!controller.getShowWeightToggleButton().isSelected())
					controller.getShowWeightToggleButton().fire();
				view.getUndoManager().doAndAdd(new CompositeCommand("set weights", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, -1),
						new ShowEdgeValueCommand(view, true, null, null)));
			}
		});

		controller.getEdgeWeightTextField().setOnAction(a -> {
			if (canUpdate) {
				var text = controller.getEdgeWeightTextField().getText();
				if (text.isBlank() || NumberUtils.isDouble(text)) {
					if (NumberUtils.isDouble(text) && !controller.getShowWeightToggleButton().isSelected())
						controller.getShowWeightToggleButton().fire();
					var value = (text.isBlank() ? SetEdgeValueCommand.UNSET : NumberUtils.parseDouble(text));
					view.getUndoManager().doAndAdd(new CompositeCommand("weights", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Weight, value),
							new ShowEdgeValueCommand(view, true, null, null)));
				}
			}
		});

		setupTriggerOnEnter(controller.getEdgeWeightTextField());

		controller.getEdgeSupportTextField().setOnAction(a -> {
			if (canUpdate) {
				var text = controller.getEdgeSupportTextField().getText();
				if (text.isBlank() || NumberUtils.isDouble(text)) {
					if (NumberUtils.isDouble(text) && !controller.getShowSupportToggleButton().isSelected())
						controller.getShowSupportToggleButton().fire();
					var value = (text.isBlank() ? SetEdgeValueCommand.UNSET : NumberUtils.parseDouble(text));
					view.getUndoManager().doAndAdd(new CompositeCommand("set support", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Confidence, value),
							new ShowEdgeValueCommand(view, null, true, null)));
				}
			}
		});
		setupTriggerOnEnter(controller.getEdgeSupportTextField());

		controller.getEdgeProbabilityTextField().setOnAction(a -> {
			if (canUpdate) {
				var text = controller.getEdgeProbabilityTextField().getText();
				if (text.isBlank() || NumberUtils.isDouble(text)) {
					if (NumberUtils.isDouble(text) && !controller.getShowProbabilityToggleButton().isSelected())
						controller.getShowProbabilityToggleButton().fire();
					var value = (text.isBlank() ? SetEdgeValueCommand.UNSET : NumberUtils.parseDouble(text));
					view.getUndoManager().doAndAdd(new CompositeCommand("set probabilities", new SetEdgeValueCommand(view, SetEdgeValueCommand.What.Probability, value),
							new ShowEdgeValueCommand(view, null, null, true)));
				}
			}
		});
		setupTriggerOnEnter(controller.getEdgeProbabilityTextField());

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

		controller.getDragButton().setOnAction(e -> {
			var rootPane = controller.getRootPane();
			var left = AnchorPane.getLeftAnchor(rootPane);
			var right = AnchorPane.getRightAnchor(rootPane);
			if (left != null) {
				AnchorPane.setLeftAnchor(rootPane, null);
				AnchorPane.setRightAnchor(rootPane, left);
			} else if (right != null) {
				AnchorPane.setLeftAnchor(rootPane, right);
				AnchorPane.setRightAnchor(rootPane, null);
			}
		});

		AccordionManager.apply(controller.getRootPane(), BasicFX.getAllRecursively(controller.getRootPane(), Accordion.class), 3);

		InvalidationListener listener = (e -> {
			var sketch = (view.getMode() == DrawView.Mode.Sketch);
			for (var titledPane : List.of(controller.getNodeStylePane(),
					controller.getNodeLabelsPane(),
					controller.getNodeLabelsStylePane(),
					controller.getEdgeStylePane(),
					controller.getEdgeLabelsPane(),
					controller.getEdgeLabelStylePane(),
					controller.getStructurePane())) {
				for (var control : BasicFX.getAllRecursively(titledPane, Control.class)) {
					if (!control.disableProperty().isBound()) {
						control.setDisable(!sketch || view.getGraphFX().isEmpty());
					}
				}
			}
		});
		view.getGraphFX().getNodeList().addListener(listener);
		view.modeProperty().addListener(listener);
	}

	private static <S, T> T notNullOrElse(S key, Function<? super S, ? extends T> function, Supplier<? extends T> alternative) {
		if (key != null) {
			var result = function.apply(key);
			if (result != null)
				return result;
		}
		return alternative.get();
	}

	private <S, T> T exactlyOne(Collection<S> nodes, Function<S, T> function) {
		T first = null;
		for (var v : nodes) {
			if (v != null) {
				var value = function.apply(v);
				if (value != null) {
					if (first == null) {
						first = value;
					} else if (!first.equals(value)) {
						return null;
					}
				}
			}
		}
		return first;
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

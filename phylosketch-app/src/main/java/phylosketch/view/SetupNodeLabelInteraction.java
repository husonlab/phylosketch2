package phylosketch.view;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import jloda.fx.control.RichTextLabel;
import jloda.graph.algorithms.ConnectedComponents;
import phylosketch.commands.LayoutLabelsCommand;

public class SetupNodeLabelInteraction {
	/**
	 * setup node label interactions
	 * Note that creation of new nodes is set up in SetupPaneInteraction
	 *
	 * @param view
	 */
	public static void apply(DrawView view, ReadOnlyBooleanProperty multiTouch) {
		var allowMove = new SimpleBooleanProperty(false);
		allowMove.bind((multiTouch.not()).and(view.movableProperty()));

		view.getNodeLabelsGroup().getChildren().addListener((ListChangeListener<? super javafx.scene.Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (var node : c.getAddedSubList()) {
						if (node instanceof RichTextLabel label) {
							if (label.getUserData() instanceof Integer vId) {
								var v = view.getGraph().findNodeById(vId);
								if (v != null) {
									var shape = DrawView.getShape(v);

									label.setOnMouseClicked(e -> shape.getOnMouseClicked().handle(e));

									// todo: do label layout in bulk, not just for one node...
									var labelLayout = LayoutLabelsCommand.computeLabelLayout(RootPosition.compute(ConnectedComponents.component(v)), v, label);
									label.setLayoutX(labelLayout.getX());
									label.setLayoutY(labelLayout.getY());

									LabelUtils.makeDraggable(label, allowMove, view, view.getUndoManager());
								}
							}
						}
					}
				}
			}
		});
	}
}

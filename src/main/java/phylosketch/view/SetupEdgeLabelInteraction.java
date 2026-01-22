package phylosketch.view;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.ProgramProperties;

public class SetupEdgeLabelInteraction {
	/**
	 * setup node label interactions
	 * Note that creation of new nodes is set up in SetupPaneInteraction
	 *
	 * @param view
	 */
	public static void apply(DrawView view, ReadOnlyBooleanProperty multiTouch) {
		var allowMove = new SimpleBooleanProperty(false);
		allowMove.bind((multiTouch.not()).and(view.movableProperty()));

		view.getEdgeLabelsGroup().getChildren().addListener((ListChangeListener<? super javafx.scene.Node>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (var node : c.getAddedSubList()) {
						if (node instanceof RichTextLabel label) {
							if (label.getUserData() instanceof Integer eId) {
								var e = view.getGraph().findEdgeById(eId);
								if (e != null) {

									label.setOnMouseClicked(a -> {
										if (!a.isShiftDown() && ProgramProperties.isDesktop()) {
											view.getNodeSelection().clearSelection();
											view.getEdgeSelection().clearSelection();
										}
										view.getEdgeSelection().toggleSelection(e);
									});
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

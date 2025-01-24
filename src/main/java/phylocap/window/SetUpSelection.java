/*
 * SetUpSelection.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.window;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.SetChangeListener;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jloda.fx.selection.SelectionModel;
import jloda.fx.util.BasicFX;
import jloda.fx.util.RunAfterAWhile;
import jloda.fx.util.SelectionEffect;
import net.sourceforge.tess4j.Word;
import phylocap.capture.CaptureService;
import phylocap.capture.Segment;
import phylosketch.main.PhyloSketch;


public class SetUpSelection {
	private static Rectangle selectionRectangle;

	public static void apply(CaptureView view, SelectionModel<Word> wordSelection, Group wordsGroup, SelectionModel<Segment> segmentSelection, Group segmentsGroup, Group topGroup) {
		var controller = view.getController();
		var service = view.getService();

		var canSelect = new SimpleBooleanProperty();
		canSelect.bind(service.runningProperty().not()
				.and(service.phaseProperty().greaterThanOrEqualTo(CaptureService.WORDS).and((service.phaseProperty().lessThanOrEqualTo(CaptureService.DUSTED)))));

		controller.getSelectAllMenuItem().setOnAction(e -> {
			if (service.getPhase() == CaptureService.WORDS) {
				for (var word : service.getWords()) {
					wordSelection.select(word);
				}
			} else if (service.getPhase() == CaptureService.SEGMENTS) {
				for (var segment : service.getSegments()) {
					segmentSelection.select(segment);
				}
			}
		});
		controller.getSelectAllMenuItem().disableProperty().bind(canSelect.not());

		controller.getSelectNoneMenuItem().setOnAction(e -> {
			if (service.getPhase() == CaptureService.WORDS) {
				wordSelection.clearSelection();
			} else if (service.getPhase() == CaptureService.SEGMENTS) {
				segmentSelection.clearSelection();
			}
		});
		controller.getSelectNoneMenuItem().disableProperty().bind(canSelect.not());

		controller.getSelectInvertMenuItem().setOnAction(e -> {
			if (service.getPhase() == CaptureService.WORDS) {
				for (var word : service.getWords()) {
					wordSelection.toggleSelection(word);
				}
			} else if (service.getPhase() == CaptureService.SEGMENTS) {
				for (var segment : service.getSegments()) {
					segmentSelection.toggleSelection(segment);
				}
			}
		});
		controller.getSelectInvertMenuItem().disableProperty().bind(canSelect.not());

		controller.getDeleteMenuItem().setOnAction(e -> {
			if (service.getPhase() == CaptureService.WORDS) {
				service.getWords().removeAll(wordSelection.getSelectedItems());
				wordsGroup.getChildren().removeAll(wordsGroup.getChildren().stream().filter(a -> a.getUserData() instanceof Word w && wordSelection.isSelected(w)).toList());
				wordSelection.clearSelection();
			} else if (service.getPhase() == CaptureService.SEGMENTS || service.getPhase() == CaptureService.DUSTED) {
				service.getSegments().removeAll(segmentSelection.getSelectedItems());
				segmentsGroup.getChildren().removeAll(segmentsGroup.getChildren().stream().filter(a -> a.getUserData() instanceof Segment s && segmentSelection.isSelected(s)).toList());
				segmentSelection.clearSelection();
			}
		});
		controller.getDeleteMenuItem().disableProperty().bind(service.runningProperty()
				.or(service.phaseProperty().isEqualTo(CaptureService.WORDS).and(wordSelection.sizeProperty().isEqualTo(0)))
				.or((service.phaseProperty().isEqualTo(CaptureService.SEGMENTS).or(service.phaseProperty().isEqualTo(CaptureService.DUSTED)).and(segmentSelection.sizeProperty().isEqualTo(0)))));

		// setup rubber band selection:
		var pane = controller.getCenterPane();
		pane.setOnMouseClicked(e -> {
			if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
				controller.getSelectNoneMenuItem().fire();
			}
			e.consume();
		});

		pane.setOnMousePressed(e -> {
			if (canSelect.get()) {
				if (selectionRectangle == null) {
					selectionRectangle = new Rectangle();
					selectionRectangle.setFill(Color.TRANSPARENT);
					selectionRectangle.setStroke(Color.DARKGREEN);
					selectionRectangle.getStrokeDashArray().setAll(3.0, 3.0);
				}

				var location = topGroup.screenToLocal(e.getScreenX(), e.getScreenY());
				selectionRectangle.setX(location.getX());
				selectionRectangle.setY(location.getY());
				selectionRectangle.setWidth(0.1);
				selectionRectangle.setHeight(0.1);
				e.consume();
			}
		});

		pane.setOnMouseDragged(e -> {
			if (canSelect.get()) {
				if (!topGroup.getChildren().contains(selectionRectangle)) {
					if (!e.isShiftDown() && PhyloSketch.isDesktop())
						controller.getSelectNoneMenuItem().fire();
					topGroup.getChildren().add(selectionRectangle);
				}

				var location = topGroup.screenToLocal(e.getScreenX(), e.getScreenY());
				var dx = location.getX() - selectionRectangle.getX();
				if (dx > 0)
					selectionRectangle.setWidth(dx);
				else {
					selectionRectangle.setWidth(selectionRectangle.getWidth() - dx);
					selectionRectangle.setX(location.getX());
				}

				var dy = location.getY() - selectionRectangle.getY();
				if (dy > 0)
					selectionRectangle.setHeight(dy);
				else {
					selectionRectangle.setHeight(selectionRectangle.getHeight() - dy);
					selectionRectangle.setY(location.getY());
				}
				e.consume();
			}
		});

		pane.setOnMouseReleased(e -> {
			if (topGroup.getChildren().contains(selectionRectangle)) {
				if (!e.isStillSincePress()) {
					var selectionBounds = selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal());
					if (service.getPhase() == CaptureService.WORDS) {
						for (var shape : BasicFX.getAllRecursively(wordsGroup, s -> s.getUserData() instanceof Word)) {
							var word = (Word) shape.getUserData();
							if (!wordSelection.isSelected(word)) {
								var shapeBounds = shape.localToScreen(shape.getBoundsInLocal());
								if (selectionBounds.intersects(shapeBounds))
									wordSelection.select(word);
							}
						}
					} else if (service.getPhase() == CaptureService.SEGMENTS || service.getPhase() == CaptureService.DUSTED) {
						for (var shape : BasicFX.getAllRecursively(segmentsGroup, s -> s.getUserData() instanceof Segment)) {
							var segment = (Segment) shape.getUserData();
							if (!segmentSelection.isSelected(segment)) {
								var shapeBounds = shape.localToScreen(shape.getBoundsInLocal());
								if (selectionBounds.intersects(shapeBounds))
									segmentSelection.select(segment);
							}
						}
					}
				}
				topGroup.getChildren().remove(selectionRectangle);
				e.consume();
			}
		});

		wordSelection.getSelectedItems().addListener((SetChangeListener<? super Word>) e -> {
			RunAfterAWhile.applyInFXThread(wordSelection, () -> {
				for (var node : BasicFX.getAllRecursively(wordsGroup, s -> s.getUserData() instanceof Word)) {
					var word = (Word) node.getUserData();
					node.setEffect(wordSelection.isSelected(word) ? SelectionEffect.getInstance() : null);
				}
			});
		});

		segmentSelection.getSelectedItems().addListener((SetChangeListener<? super Segment>) e -> {
			RunAfterAWhile.applyInFXThread(segmentSelection, () -> {
				for (var node : BasicFX.getAllRecursively(segmentsGroup, s -> s.getUserData() instanceof Segment)) {
					var segment = (Segment) node.getUserData();
					node.setEffect(segmentSelection.isSelected(segment) ? SelectionEffect.getInstance() : null);
				}
			});
		});

	}
}

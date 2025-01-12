/*
 * CaptureViewPresenter.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.view;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import jloda.fx.selection.SelectionModel;
import jloda.fx.selection.SetSelectionModel;
import jloda.fx.util.RunAfterAWhile;
import jloda.fx.window.NotificationManager;
import net.sourceforge.tess4j.Word;
import phylocap.capture.CaptureService;
import phylocap.capture.DrawUtils;
import phylocap.capture.Segment;
import phylocap.capture.SetupRootTool;
import phylosketch.view.ImportButtonUtils;

/**
 * capture view presenter
 * Daniel Huson, 1.2025
 */
public class CaptureViewPresenter {
	private final Group imageGroup = new Group();
	private final Group wordsGroup = new Group();
	private final Group pointsGroup = new Group();
	private final Group segmentsGroup = new Group();
	private final Group rootGroup = new Group();
	private final Group phylogenyGroup = new Group();
	private final Group topGroup = new Group();
	private final Group worldGroup = new Group();

	private final SelectionModel<Word> wordSelection = new SetSelectionModel<>();
	private final SelectionModel<Segment> segmentSelection = new SetSelectionModel<>();

	public CaptureViewPresenter(CaptureView captureView) {
		var controller = captureView.getController();
		var service = captureView.getService();

		worldGroup.getChildren().addAll(imageGroup, wordsGroup, pointsGroup, segmentsGroup, rootGroup, phylogenyGroup, topGroup);
		controller.getCenterPane().getChildren().add(worldGroup);

		service.setProgressParentPane(controller.getStatusPane());

		SetUpSelection.apply(captureView, wordSelection, wordsGroup, segmentSelection, segmentsGroup, topGroup);

		controller.getInfoLabel().setText("");

		SetupRootTool.apply(controller, service, rootGroup);

		controller.getZoomInButton().setOnAction(e -> {
			worldGroup.setScaleX(1.1 * worldGroup.getScaleX());
			worldGroup.setScaleY(1.1 * worldGroup.getScaleY());
		});

		controller.getZoomOutButton().setOnAction(e -> {
			worldGroup.setScaleX(1 / 1.1 * worldGroup.getScaleX());
			worldGroup.setScaleY(1 / 1.1 * worldGroup.getScaleY());
		});

		ImportButtonUtils.setup(null, controller.getImportButton(), k -> {
		}, service::setInputImage);

		controller.getWhiteThresholdCBox().valueProperty().addListener((v, o, n) -> {
			if (n > 0 && n < 1)
				service.setWhiteThreshold(n);
			else {
				controller.getWhiteThresholdCBox().setValue(o);
			}
		});
		controller.getWhiteThresholdCBox().disableProperty().bind(service.runningProperty());

		controller.getMaxLabelHeightCBox().valueProperty().addListener((v, o, n) -> {
			if (n > 0 && n < 512)
				service.setMaxTextHeight(n);
			else {
				controller.getMaxLabelHeightCBox().setValue(o);
			}
		});
		controller.getMaxLabelHeightCBox().disableProperty().bind(service.runningProperty());

		controller.getDustMaxDistCBox().valueProperty().addListener((v, o, n) -> service.setMaxDustDistance(n));
		service.maxDustDistanceProperty().addListener((v, o, n) -> controller.getDustMaxDistCBox().setValue(n.intValue()));

		controller.getDustMinExtentCBox().valueProperty().addListener((v, o, n) -> service.setMinDustExtent(n));
		service.minDustExtentProperty().addListener((v, o, n) -> controller.getDustMinExtentCBox().setValue(n.intValue()));

		controller.getSetRootLocationToggleButton().disableProperty().bind(service.runningProperty().or(service.phaseProperty().isEqualTo(CaptureService.NONE)));

		controller.getClearMenuItem().setOnAction(e -> service.clearData());
		controller.getClearMenuItem().disableProperty().bind(service.phaseProperty().isEqualTo(CaptureService.NONE).or(service.runningProperty()));

		controller.getDetectAllMenuItem().setOnAction(e -> {
			service.clearData();
			service.run(CaptureService.PHYLOGENY);
		});
		controller.getDetectAllMenuItem().disableProperty().bind(service.phaseProperty().isEqualTo(CaptureService.NONE).or(service.runningProperty()));

		controller.getDetectWordsMenuItem().setOnAction(e -> service.run(CaptureService.WORDS));
		service.phaseProperty().addListener((v, o, n) -> controller.getDetectWordsMenuItem().setSelected(n.intValue() >= CaptureService.WORDS));
		controller.getDetectWordsMenuItem().disableProperty().bind(controller.getDetectWordsMenuItem().selectedProperty().or(service.runningProperty())
				.or(service.phaseProperty().isEqualTo(CaptureService.NONE)));

		controller.getDetectSegmentsMenuItem().setOnAction(e -> service.run(CaptureService.SEGMENTS));
		service.phaseProperty().addListener((v, o, n) -> controller.getDetectSegmentsMenuItem().setSelected(n.intValue() >= CaptureService.SEGMENTS));
		controller.getDetectSegmentsMenuItem().disableProperty().bind(controller.getDetectSegmentsMenuItem().selectedProperty().or(service.runningProperty())
				.or(service.phaseProperty().isEqualTo(CaptureService.NONE)));

		controller.getRemoveDustMenuItem().setOnAction(e -> service.run(CaptureService.DUSTED));
		service.phaseProperty().addListener((v, o, n) -> controller.getRemoveDustMenuItem().setSelected(n.intValue() >= CaptureService.DUSTED));
		controller.getRemoveDustMenuItem().disableProperty().bind(controller.getRemoveDustMenuItem().selectedProperty().or(service.runningProperty())
				.or(service.phaseProperty().isEqualTo(CaptureService.NONE)));

		controller.getDetectPhylogenyMenuItem().setOnAction(e -> service.run(CaptureService.PHYLOGENY));
		service.phaseProperty().addListener((v, o, n) -> controller.getDetectPhylogenyMenuItem().setSelected(n.intValue() >= CaptureService.PHYLOGENY));
		controller.getDetectPhylogenyMenuItem().disableProperty().bind(controller.getDetectPhylogenyMenuItem().selectedProperty().or(service.runningProperty())
				.or(service.phaseProperty().isEqualTo(CaptureService.NONE)));

		service.phaseProperty().addListener((v, o, n) -> {
			var phase = n.intValue();
			if (phase == CaptureService.NONE) {
				imageGroup.getChildren().clear();
				wordsGroup.getChildren().clear();
				segmentsGroup.getChildren().clear();
				phylogenyGroup.getChildren().clear();
				pointsGroup.getChildren().clear();
				wordSelection.clearSelection();
				segmentSelection.clearSelection();
			} else if (phase == CaptureService.IMAGE) {
				imageGroup.getChildren().setAll(new ImageView(service.getInputImage()));
			} else if (phase == CaptureService.WORDS) {
				imageGroup.getChildren().setAll(new ImageView(service.getGreyScaleImage()));
				DrawUtils.createWordShapes(service.getWords(), wordSelection, () -> service.getPhase() == CaptureService.WORDS, wordsGroup);
			} else if (phase == CaptureService.SEGMENTS) {
				imageGroup.getChildren().setAll(new ImageView(service.getMaskedImage()));
				RunAfterAWhile.applyInFXThread(pointsGroup, () -> DrawUtils.createCircles(service.getEndPoints(), pointsGroup));
				RunAfterAWhile.applyInFXThread(segmentsGroup, () -> DrawUtils.createPaths(service.getSegments(), segmentSelection,
						() -> service.getPhase() == CaptureService.SEGMENTS, segmentsGroup));
			} else if (phase == CaptureService.DUSTED) {
				RunAfterAWhile.applyInFXThread(segmentsGroup, () -> DrawUtils.createPaths(service.getDustedSegments(), segmentSelection,
						() -> service.getPhase() == CaptureService.DUSTED, segmentsGroup));
			} else if (phase == CaptureService.PHYLOGENY) {

			}
		});

		service.setOnFailed(e -> NotificationManager.showError("Capture failed: " + service.getException().getMessage()));

		service.stateProperty().addListener((v, o, n) -> {
			switch (n) {
				case FAILED ->
						Platform.runLater(() -> controller.getInfoLabel().setText("Failed: " + service.getException().getMessage()));
				case SCHEDULED -> {
					controller.getInfoLabel().setText("");
				}
				case SUCCEEDED -> {
					controller.getInfoLabel().setText("Labels: %d Segments: %d".formatted(service.getWords().size(), service.getSegments().size()));
				}
			}
		});
	}
}

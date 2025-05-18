/*
 * SetupCaptureService.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.pane;

import javafx.collections.SetChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import jloda.fx.selection.SelectionModel;
import jloda.fx.selection.SetSelectionModel;
import jloda.fx.util.BasicFX;
import jloda.fx.util.SelectionEffectBlue;
import phylosketch.capturepane.capture.CaptureService;
import phylosketch.capturepane.capture.Point;
import phylosketch.capturepane.capture.Segment;
import phylosketch.capturepane.capture.Word;
import phylosketch.utils.ScrollPaneUtils;
import phylosketch.view.DrawView;
import phylosketch.window.MainWindowController;

import java.util.ArrayList;
import java.util.List;

/**
 * sets up the capture service
 * Daniel Huson, 1.2025
 */
public class SetupCaptureService {
	/**
	 * sets up the capture service
	 *
	 * @param controller  the main controller
	 * @param view        the main view
	 * @param capturePane the capture pane
	 * @return the service
	 */
	public static CaptureService apply(MainWindowController controller, DrawView view, CapturePane capturePane) {
		final CaptureService service = new CaptureService();
		final SelectionModel<Word> wordSelection = new SetSelectionModel<>();
		final SelectionModel<Segment> pathSelection = new SetSelectionModel<>();

		wordSelection.getSelectedItems().addListener((SetChangeListener<? super Word>) e -> {
			if (e.wasAdded()) {
				for (var shape : BasicFX.findRecursively(capturePane.getWordsGroup(), s -> s.getUserData() == e.getElementAdded())) {
					shape.setEffect(SelectionEffectBlue.getInstance());
				}
			} else if (e.wasRemoved()) {
				for (var shape : BasicFX.findRecursively(capturePane.getWordsGroup(), s -> s.getUserData() == e.getElementRemoved())) {
					shape.setEffect(null);
				}
			}
		});

		pathSelection.getSelectedItems().addListener((SetChangeListener<? super Segment>) e -> {
			if (e.wasAdded()) {
				for (var shape : BasicFX.findRecursively(capturePane.getPathsGroup(), s -> s.getUserData() == e.getElementAdded())) {
					shape.setEffect(SelectionEffectBlue.getInstance());
				}
			} else if (e.wasRemoved()) {
				for (var shape : BasicFX.findRecursively(capturePane.getPathsGroup(), s -> s.getUserData() == e.getElementRemoved())) {
					shape.setEffect(null);
				}
			}
		});

		service.setProgressParentPane(controller.getBottomFlowPane());

		var skeletonImageView = new ImageView();

		var phylogenyCapture = new PhylogenyCapture();

		capturePane.getPropertySettingPane().addProperties(service.getParameters().getAll());
		capturePane.getPropertySettingPane().addProperties(phylogenyCapture.getAll());

		view.modeProperty().addListener((v, o, n) -> {
			capturePane.reset();
			skeletonImageView.setImage(null);
		});

		service.setOnSucceeded(e -> {
			ScrollPaneUtils.runRemoveAndKeepScrollPositions(capturePane, () -> capturePane.getMainPane().getChildren().remove(skeletonImageView));

			var words = new ArrayList<Word>();
			var segments = new ArrayList<Segment>();
			if (service.getPhase() >= CaptureService.SEGMENTS) {
				if (true) {
					skeletonImageView.setImage(service.getSkeletonImage());
					skeletonImageView.fitWidthProperty().bind(capturePane.getImageView().fitWidthProperty());
					skeletonImageView.fitHeightProperty().bind(capturePane.getImageView().fitHeightProperty());
					capturePane.getMainPane().getChildren().add(0, skeletonImageView);
				}
				segments.addAll(transformSegments(capturePane, service.getSegments()));
				capturePane.getPathsGroup().getChildren().clear();
				if (service.getPhase() < CaptureService.DUSTED) {
					capturePane.getPathsGroup().getChildren().addAll(DrawUtils.createCircles(service.getEndPoints()));
				}
				capturePane.getPathsGroup().getChildren().addAll(DrawUtils.createPaths(segments, pathSelection, () -> true));
			}
			if (service.getPhase() >= CaptureService.WORDS) {
				words.addAll(transformWords(capturePane, service.getWords()));
				DrawUtils.createWordShapes(words, wordSelection, () -> true, capturePane.getWordsGroup());
			}
			if (service.getPhase() == CaptureService.PHYLOGENY) {
				var rootLocation = screenToImage(capturePane.getRootLocationOnScreen(), capturePane.getImageView());
				view.getUndoManager().add(phylogenyCapture.apply(view, rootLocation, capturePane.getRootSide(), segments, words));
				capturePane.reset();
			}
		});
		return service;
	}

	public static List<Word> transformWords(CapturePane capturePane, ArrayList<Word> words) {
		var imageView = capturePane.getImageView();
		var list = new ArrayList<Word>();
		for (var word : words) {
			var bbox = word.boundingBox();
			var aPt = imageToPaneCoordinates(imageView, bbox.getMinX(), bbox.getMinY());
			var bPt = imageToPaneCoordinates(imageView, bbox.getMaxX(), bbox.getMaxY());
			var x = (int) Math.min(aPt.getX(), bPt.getX());
			var width = (int) Math.abs(aPt.getX() - bPt.getX());
			var y = (int) Math.min(aPt.getY(), bPt.getY());
			var height = (int) Math.abs(aPt.getY() - bPt.getY());
			list.add(new Word(word.text(), word.confidence(), new java.awt.Rectangle(x, y, width, height)));
		}
		return list;
	}

	public static List<Segment> transformSegments(CapturePane capturePane, ArrayList<Segment> segments) {
		var imageView = capturePane.getImageView();
		var list = new ArrayList<Segment>();
		for (var segment : segments) {
			var copy = new Segment();
			copy.points().addAll(segment.points().stream()
					.map(p -> imageToPaneCoordinates(imageView, p.x(), p.y()))
					.map(p -> new Point((int) p.getX(), (int) p.getY())).toList());
			list.add(copy);
		}
		return list;
	}

	public static Point2D imageToPaneCoordinates(ImageView imageView, double imageX, double imageY) {
		double viewX = (imageX / imageView.getImage().getWidth()) * imageView.getBoundsInLocal().getWidth();
		double viewY = (imageY / imageView.getImage().getHeight()) * imageView.getBoundsInLocal().getHeight();
		return imageView.localToParent(viewX, viewY);
	}

	public static Point2D screenToImage(Point2D screenLocation, ImageView imageView) {
		var image = imageView.getImage();
		var sceneCoords = imageView.screenToLocal(screenLocation);

		double viewWidth = imageView.getBoundsInLocal().getWidth();
		double viewHeight = imageView.getBoundsInLocal().getHeight();

		double imageWidth = image.getWidth();
		double imageHeight = image.getHeight();

		double imageX = (sceneCoords.getX() / viewWidth) * imageWidth;
		double imageY = (sceneCoords.getY() / viewHeight) * imageHeight;

		return new Point2D(imageX, imageY);
	}
}

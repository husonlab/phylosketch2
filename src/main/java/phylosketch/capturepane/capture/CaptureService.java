/*
 * CaptureService.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.capture;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import jloda.fx.util.AService;
import jloda.fx.window.NotificationManager;
import jloda.util.Basic;

import java.awt.*;
import java.util.ArrayList;

/**
 * captures segments and words from a picture of a phylogentic tree or network
 * Daniel Huson, 1.2025
 */
public class CaptureService extends AService<Boolean> {
	// phases:
	public static final int NONE = 0;
	public static final int IMAGE = 1;
	public static final int SEGMENTS = 2;
	public static final int WORDS = 3;
	public static final int DUSTED = 4;
	public static final int PHYLOGENY = 5;

	private final Parameters parameters = new Parameters();

	private Image inputImage;
	private Image skeletonImage;
	private Image greyScaleImage;

	private final ArrayList<Word> allWords = new ArrayList<>();
	private final ArrayList<Word> words = new ArrayList<>();

	private final ArrayList<Point> endPoints = new ArrayList<>();
	private final ArrayList<Segment> segments = new ArrayList<>();

	private final IntegerProperty phase = new SimpleIntegerProperty(this, "status", NONE);
	private final IntegerProperty goal = new SimpleIntegerProperty(this, "goal", NONE);

	/**
	 * constructor
	 */
	public CaptureService() {
		setOnFailed(e -> {
			Basic.caught(getException());
			NotificationManager.showError("Image capture failed: " + getException());
		});

		setCallable(() -> {
			if (getGoal() == NONE)
				return false;

			getProgressListener().setTasks("Capture", "OCR");

			var theStatus = getPhase();

			if (getInputImage() == null) {
				theStatus = NONE;
				updatePhase(theStatus);
				throw new IllegalArgumentException("No input image set");
			}

			if (getGoal() < getPhase()) {
				clearData();
				theStatus = IMAGE;
				updatePhase(theStatus);
				Platform.runLater(this::clear);
			}

			if (getGoal() >= SEGMENTS && theStatus < SEGMENTS) {
				var matrix = ImageUtils.convertToBinaryArray(getInputImage());

				if (true) {
					if (ImageUtils.tooMuchBlack(matrix, 0.30)) {
						throw new RuntimeException("Image has too much foreground");
					}
				}
				Skeletonization.apply(matrix);

				if (true)
					DotConnector.apply(matrix);

				skeletonImage = ImageUtils.convertToImage(matrix, Color.HOTPINK);

				CapturePointsSegments.apply(getProgressListener(), matrix, 0, endPoints, segments);
				theStatus = SEGMENTS;
				updatePhase(theStatus);
			}

			if (getGoal() >= WORDS && theStatus < WORDS) {
				greyScaleImage = ImageUtils.convertToGrayScale(getInputImage());

				allWords.clear();
				allWords.addAll(OCR.getWords(greyScaleImage));
				words.clear();
				words.addAll(CaptureWords.joinConsecutiveWords(CaptureWords.filter(allWords, parameters.getMinWordLength(), parameters.getMinTextHeight(),
						parameters.getMaxTextHeight(), parameters.isMustStartAlphaNumeric(), parameters.isMustEndAlphaNumeric(), parameters.isMustContainLetter())));
				theStatus = WORDS;
				updatePhase(theStatus);
			}

			if (getGoal() >= DUSTED && theStatus < DUSTED) {
				getProgressListener().setTasks("Capture", "Dust removal");
				var dustedSegments = DustRemoval.apply(getProgressListener(), segments, parameters.getMaxDustDistance(), parameters.getMinDustExtent());
				System.err.println("Dusting by size: " + segments.size() + " -> " + dustedSegments.size());

				// keep every segment that are not covered by a word box
				segments.clear();
				for (var segment : dustedSegments) {
					var bbox = shrink(segment.computeBoundingBox(), 10);
					if (allWords.stream().noneMatch(word -> word.boundingBox().contains(bbox)))
						segments.add(segment);
				}
				System.err.println("Dusting by words: " + dustedSegments.size() + " -> " + segments.size());
				theStatus = DUSTED;
				updatePhase(theStatus);
			}

			if (getGoal() >= PHYLOGENY && theStatus < PHYLOGENY) {
				theStatus = PHYLOGENY;
				updatePhase(theStatus);
			}
			return true;
		});
	}

	public static Rectangle shrink(Rectangle rect, int inset) {
		if (rect == null) {
			throw new IllegalArgumentException("Rectangle cannot be null");
		}
		if (inset < 0) {
			throw new IllegalArgumentException("Inset must be non-negative");
		}

		// Ensure x and y do not extend beyond their limits
		int newX = Math.min(rect.x + inset, rect.x + rect.width);
		int newY = Math.min(rect.y + inset, rect.y + rect.height);

		// Ensure width and height do not go negative
		int newWidth = Math.max(rect.width - 2 * inset, 1);
		int newHeight = Math.max(rect.height - 2 * inset, 1);

		return new Rectangle(newX, newY, newWidth, newHeight);
	}

	public void clear() {
		inputImage = null;
		clearData();
	}

	public void clearData() {
		updatePhase(getInputImage() != null ? IMAGE : NONE);
		greyScaleImage = null;
		words.clear();
		endPoints.clear();
		segments.clear();
	}

	public int getPhase() {
		return phase.get();
	}

	public ReadOnlyIntegerProperty phaseProperty() {
		return phase;
	}

	private void updatePhase(int phase) {
		Platform.runLater(() -> this.phase.set(phase));
	}

	/**
	 * set the input image from which to capturepane the phylogeny
	 *
	 * @param image input image
	 */
	public void setInputImage(Image image) {
		inputImage = image;
		clearData();
	}


	public Image getInputImage() {
		return inputImage;
	}

	/**
	 * get the gray scale image
	 *
	 * @return gray scale image
	 */
	public Image getGreyScaleImage() {
		return greyScaleImage;
	}

	public Image getSkeletonImage() {
		return skeletonImage;
	}

	/**
	 * get the list of detected words
	 *
	 * @return words list
	 */
	public ArrayList<Word> getWords() {
		return words;
	}

	public void removeWord(Word word) {
		if (!isRunning()) {
			this.words.remove(word);
		}
	}

	/**
	 * get the list of detected terminal points
	 *
	 * @return terminal points
	 */
	public ArrayList<Point> getEndPoints() {
		return endPoints;
	}

	/**
	 * get the list of detected path segments
	 *
	 * @return paths
	 */
	public ArrayList<Segment> getSegments() {
		return segments;
	}

	public int getGoal() {
		return goal.get();
	}

	public ReadOnlyIntegerProperty goalProperty() {
		return goal;
	}

	public void setGoal(int goal) {
		this.goal.set(goal);
	}

	public void run(int goal) {
		if (goal > getPhase()) {
			setGoal(goal);
			restart();
		}
	}

	public Parameters getParameters() {
		return parameters;
	}
}

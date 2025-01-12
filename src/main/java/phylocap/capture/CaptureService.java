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

package phylocap.capture;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import jloda.fx.util.AService;
import jloda.fx.util.ProgramProperties;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import java.util.ArrayList;


/**
 * phylogenetic tree or network capture service
 * Daniel Huson, 1.2025
 */
public class CaptureService extends AService<Boolean> {
	// phases:
	public static final int NONE = 0;
	public static final int IMAGE = 1;
	public static final int WORDS = 2;
	public static final int SEGMENTS = 3;
	public static final int DUSTED = 4;
	public static final int PHYLOGENY = 5;

	private final DoubleProperty whiteThreshold = new SimpleDoubleProperty(this, "whiteThreshold");
	private final IntegerProperty maxTextHeight = new SimpleIntegerProperty(this, "maxTextHeight");
	private final IntegerProperty maxDustDistance = new SimpleIntegerProperty(this, "maxDustDistance");
	private final IntegerProperty minDustExtent = new SimpleIntegerProperty(this, "minDustExtent");


	private Point2D rootLocation;

	private Image inputImage;
	private Image workingImage;
	private Image greyScaleImage;
	private Image maskedImage;

	private final ArrayList<Word> words = new ArrayList<>();
	private final ArrayList<Point> endPoints = new ArrayList<>();
	private final ArrayList<Segment> segments = new ArrayList<>();
	private final ArrayList<Segment> dustedSegments = new ArrayList<>();

	private final IntegerProperty phase = new SimpleIntegerProperty(this, "status", NONE);
	private final IntegerProperty goal = new SimpleIntegerProperty(this, "goal", NONE);

	/**
	 * constructor
	 */
	public CaptureService() {
		ProgramProperties.track(whiteThreshold, 0.9);
		ProgramProperties.track(maxTextHeight, 64);
		ProgramProperties.track(maxDustDistance, 20);
		ProgramProperties.track(minDustExtent, 50);

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

			if (getGoal() >= WORDS && theStatus < WORDS) {
				greyScaleImage = ImageUtils.convertToGrayScale(getInputImage());
				workingImage = ImageUtils.replaceTransparentBackground(greyScaleImage);
				var binaryMatrix = ImageUtils.convertToBinaryArray(workingImage, 0.95);
				if (ImageUtils.tooMuchBlack(binaryMatrix, 0.30))
					throw new RuntimeException("Image has too much foreground");

				var awtBufferedImage = ImageUtils.convertToBufferedImage(greyScaleImage);

				var tesseract = new Tesseract();
				tesseract.setDatapath("tessdata");
				tesseract.setLanguage("eng");

				if (false) {
					var text = tesseract.doOCR(awtBufferedImage);
					System.err.println("Extracted Text:\n" + text);
				}

				// filter words and update rectangles:
				{
					words.clear();
					for (var word : tesseract.getWords(awtBufferedImage, ITessAPI.TessPageIteratorLevel.RIL_WORD)) {
						var awtRect = word.getBoundingBox();
						if (word.getConfidence() > 0.3 && awtRect.getHeight() <= getMaxTextHeight()) {
							ProcessingUtils.tightenRect(binaryMatrix, awtRect);
							words.add(new Word(word.getText(), word.getConfidence(), awtRect));
						}
					}
				}
				theStatus = WORDS;
				updatePhase(theStatus);
			}

			if (getGoal() >= SEGMENTS && theStatus < SEGMENTS) {
				getProgressListener().setTasks("Capture", "Detecting segments");

				endPoints.clear();
				segments.clear();

				// detect terminal points and paths:
				{
					var theMaskedImage = ImageUtils.replaceRectanglesWithWhite(workingImage, words.stream().map(Word::getBoundingBox).toList(), 2);
					var matrix = ImageUtils.convertToBinaryArray(theMaskedImage, 0.95);
					Skeletonization.apply(matrix);
					var points = ProcessingUtils.detectEndPoints(matrix);
					points.addAll(ProcessingUtils.detectBranchPoints(matrix));
					points = ProcessingUtils.removeClosePoints(points, 5);

					endPoints.addAll(points);

					maskedImage = ImageUtils.convertToImage(matrix);
					segments.addAll(ProcessingUtils.findPaths(matrix, points));
				}
				theStatus = SEGMENTS;
				updatePhase(theStatus);
			}

			if (getGoal() >= DUSTED && theStatus < DUSTED) {
				getProgressListener().setTasks("Capture", "Dust removal");
				dustedSegments.clear();
				dustedSegments.addAll(DustRemoval.apply(getProgressListener(), segments, 10, 30));
				theStatus = DUSTED;
				updatePhase(theStatus);
			}

			if (getGoal() >= PHYLOGENY && theStatus < PHYLOGENY) {
				getProgressListener().setTasks("Capture", "Extracting phylogeny");
				theStatus = PHYLOGENY;
				updatePhase(theStatus);
			}
			return true;
		});
	}

	public void clear() {
		inputImage = null;
		clearData();
	}

	public void clearData() {
		updatePhase(getInputImage() != null ? IMAGE : NONE);
		maskedImage = null;
		greyScaleImage = null;
		words.clear();
		endPoints.clear();
		segments.clear();
		dustedSegments.clear();
	}


	/**
	 * set the brightness threshold between 0-1 to use to convert to black-and-white image
	 *
	 * @param whiteThreshold brightness threshold
	 */
	public void setWhiteThreshold(double whiteThreshold) {
		if (whiteThreshold > 0 && whiteThreshold < 1) {
			this.whiteThreshold.set(whiteThreshold);
		} else throw new IllegalArgumentException("whiteThreshold %f: out of bounds (0-1)".formatted(whiteThreshold));
	}

	public double getWhiteThreshold() {
		return whiteThreshold.get();
	}

	public ReadOnlyDoubleProperty whiteThresholdProperty() {
		return whiteThreshold;
	}

	/**
	 * set the maximum text height, to avoid parts of the actual tree or network being converted to text
	 *
	 * @param maxTextHeight max text height used in OCR
	 */
	public void setMaxTextHeight(int maxTextHeight) {
		if (maxTextHeight > 0) {
			this.maxTextHeight.set(maxTextHeight);
		} else throw new IllegalArgumentException("maxTextHeight %d: must be positive".formatted(maxTextHeight));
	}

	public int getMaxTextHeight() {
		return maxTextHeight.get();
	}

	public ReadOnlyIntegerProperty maxTextHeightProperty() {
		return maxTextHeight;
	}

	public int getMaxDustDistance() {
		return maxDustDistance.get();
	}

	public IntegerProperty maxDustDistanceProperty() {
		return maxDustDistance;
	}

	public void setMaxDustDistance(int value) {
		maxDustDistance.set(value);
	}

	public int getMinDustExtent() {
		return minDustExtent.get();
	}

	public IntegerProperty minDustExtentProperty() {
		return minDustExtent;
	}

	public void setMinDustExtent(int value) {
		minDustExtent.set(value);
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
	 * set the input image from which to capture the phylogeny
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

	/**
	 * get the masked image with OCR text removed
	 *
	 * @return masked image
	 */
	public Image getMaskedImage() {
		return maskedImage;
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

	public ArrayList<Segment> getDustedSegments() {
		return dustedSegments;
	}

	public Point2D getRootLocation() {
		return rootLocation;
	}


	public void setRootLocation(Point2D rootLocation) {
		this.rootLocation = rootLocation;
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

}

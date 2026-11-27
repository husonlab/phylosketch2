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
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import jloda.fx.util.AService;
import phylosketch.ocr.OcrWord;
import phylosketch.view.RootPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

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

	private RootPosition.Side rootSide = RootPosition.Side.Left;
	private boolean wordsCaptured = false;
	private boolean[][] lines; // the ink of the input image that belongs to lines, once known
	private final HashMap<Integer, List<OcrWord>> firstReadings = new HashMap<>(); // what OCR read, by rotation of the image
	private Exception ocrError;
	private Image withoutLines;
	// a word that OCR read with at least this confidence, and with at least three letters or digits and no stroke, is text
	private static final double CONFIDENT = 60;

	private final ArrayList<OcrWord> allWords = new ArrayList<>();
	private final ArrayList<OcrWord> textWords = new ArrayList<>(); // those of allWords that are text, by content and size
	private final ArrayList<OcrWord> words = new ArrayList<>();

	private final ArrayList<Point> endPoints = new ArrayList<>();
	private final ArrayList<Segment> segments = new ArrayList<>();

	private final IntegerProperty phase = new SimpleIntegerProperty(this, "status", NONE);
	private final IntegerProperty goal = new SimpleIntegerProperty(this, "goal", NONE);

	/**
	 * constructor
	 */
	public CaptureService(Consumer<Throwable> exceptionConsumer) {
		setOnFailed(e -> {
			exceptionConsumer.accept(getException());
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
				var matrix = Binarization.apply(getInputImage());

				if (ImageUtils.tooMuchBlack(matrix, 0.30)) {
					throw new UnsuitableImageException("This image cannot be captured: too much of it is dark. Capture needs a drawing with dark lines on a light background.");
				}

				// Capture the words first so we can erase their ink from the image before tracing. Otherwise
				// label strokes get skeletonized and traced into spurious ("ghost") nodes and edges. This works
				// for every orientation, including the tall/narrow boxes of rotated top/bottom labels, where the
				// old segment-level word-box test could not reliably catch them.
				// Only what OCR reports as text of a plausible size is used, and only the ink that lies completely
				// inside a box is erased: OCR also reports boxes where there is no text, some of them over large
				// parts of the tree, and the box of a label next to a branch overlaps the branch.
				// Dashed and dotted lines are joined before that: OCR reports dashes as words too, and a dash that
				// is still on its own lies completely inside such a box. Once they are joined, what is a line and
				// what is text can be told apart by size, and a word that was read together with a piece of a line
				// is read again without it.
				var crossings = DashConnector.apply(matrix, confidentText());
				lines = LineInk.compute(matrix);
				captureWords();
				TextEraser.apply(matrix, textWords.stream().map(OcrWord::boundingBox).toList(), 2);
				TextEraser.eraseSmallPieces(matrix, LineInk.maxGlyph(matrix[0].length, matrix.length));

				var ink = new int[matrix.length][];
				for (var y = 0; y < matrix.length; y++)
					ink[y] = matrix[y].clone();

				Skeletonization.apply(matrix);
				SpurRemoval.apply(matrix, ink);

				DotConnector.apply(matrix);

				skeletonImage = ImageUtils.convertToImage(matrix, Color.HOTPINK);

				CapturePointsSegments.apply(getProgressListener(), matrix, (int) Math.round(parameters.getMinDistanceNodes()), endPoints, segments);
				Crossings.apply(segments, crossings);
				Crossings.apply(segments, getInputImage());
				Crossings.joinThrough(segments);
				theStatus = SEGMENTS;
				updatePhase(theStatus);
			}

			if (getGoal() >= WORDS && theStatus < WORDS) {
				captureWords();
				theStatus = WORDS;
				updatePhase(theStatus);
			}

			if (getGoal() >= DUSTED && theStatus < DUSTED) {
				getProgressListener().setTasks("Capture", "Dust removal");
				var dustedSegments = DustRemoval.apply(getProgressListener(), segments, parameters.getMaxDustDistance(), parameters.getMinDustExtent());
				System.err.println("Dusting by size: " + segments.size() + " -> " + dustedSegments.size());

				// Remove what is left of text: segments that hang together and all lie in the box of one word. A
				// segment in such a box that hangs together with segments outside it is a line that runs through
				// the box or ends in it, however short, and stays.
				segments.clear();
				var group = new int[dustedSegments.size()];
				for (var i = 0; i < group.length; i++)
					group[i] = i;
				for (var i = 0; i < group.length; i++) {
					for (var j = i + 1; j < group.length; j++) {
						var a = dustedSegments.get(i);
						var b = dustedSegments.get(j);
						if (a.first().distance(b.first()) <= 3 || a.first().distance(b.last()) <= 3 || a.last().distance(b.first()) <= 3 || a.last().distance(b.last()) <= 3) {
							var from = group[j];
							var to = group[i];
							for (var k = 0; k < group.length; k++) {
								if (group[k] == from)
									group[k] = to;
							}
						}
					}
				}
				for (var i = 0; i < group.length; i++) {
					var g = group[i];
					var covered = false;
					for (var word : textWords) {
						var all = true;
						for (var j = 0; j < group.length && all; j++) {
							if (group[j] == g && !word.boundingBox().contains(shrink(dustedSegments.get(j).computeBoundingBox(), 10)))
								all = false;
						}
						if (all) {
							covered = true;
							break;
						}
					}
					if (!covered)
						segments.add(dustedSegments.get(i));
				}
				System.err.println("Dusting by words: " + dustedSegments.size() + " -> " + segments.size());

				// what is left of a letter that touches a line: a knot no larger than a letter
				{
					var heights = textWords.stream().mapToDouble(word -> Math.min(word.boundingBox().getWidth(), word.boundingBox().getHeight())).sorted().toArray();
					var image = getInputImage();
					var letterSize = (heights.length > 0 ? 1.2 * heights[heights.length / 2] : Math.max(16, 0.02 * Math.max(image.getWidth(), image.getHeight())));
					var without = KnotRemoval.apply(segments, textWords.stream().map(OcrWord::boundingBox).toList(), letterSize);
					System.err.println("Knots: " + segments.size() + " -> " + without.size());
					segments.clear();
					segments.addAll(without);
				}
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

	/**
	 * run OCR and populate the word lists. Idempotent within a run (guarded by {@code wordsCaptured}) so it
	 * can be called from the SEGMENTS phase (to mask label boxes out of the image before tracing) and again
	 * from the WORDS phase without repeating the work.
	 * <p>
	 * Leaf labels on top/bottom-rooted trees are usually rotated 90 degrees, which horizontal OCR cannot read,
	 * and the whole word pipeline (height filter, line grouping) assumes horizontal text. So we run the pipeline
	 * on a rotated copy where the labels are horizontal, then map the boxes back. We try both rotations (the
	 * CW/CCW ambiguity) plus no rotation, and keep whichever reads the most text (highest summed confidence).
	 */
	private void captureWords() throws Exception {
		if (wordsCaptured)
			return;

		var origW = (int) getInputImage().getWidth();
		var origH = (int) getInputImage().getHeight();

		List<OcrWord> bestAll = List.of();
		List<OcrWord> bestText = List.of();
		List<OcrWord> bestWords = List.of();
		var bestScore = -1.0;
		for (var rot : rotations()) {
			var raw = firstReading(rot);
			if (raw == null)
				continue;
			var image = rotate(greyScaleImage, rot);
			if (lines != null) { // a word read together with a piece of a line is read again without the line
				if (withoutLines == null)
					withoutLines = LineInk.erase(greyScaleImage, lines);
				raw = LineInk.readAgain(image, rotate(withoutLines, rot), raw);
			}
			raw = BranchEnds.stripDashes(raw);
			// Keep low-confidence and short words through the merge: a faint genus abbreviation ("V.") or a
			// mid-confidence species word (e.g. "chungii@24") must not be dropped before it can join its
			// label, or the label collapses to fragments and the leading "V." is then lost to the trailing-".
			// rule. Pre-filter only by text height (removes non-text boxes); apply the confidence and length
			// thresholds to the MERGED label instead, using its averaged confidence.
			//
			// The height gate is RELATIVE to the median detected box height, not the absolute
			// [minTextHeight, maxTextHeight] parameters: on a large, small-text capture the absolute gate
			// discards most real words (their glyph boxes are well under minTextHeight=10px), which is what
			// limited big top/bottom trees to ~1/4 of their labels. A relative gate adapts to any text size.
			// The median is that of the words with a letter or digit: where the labels stand in dashed lines, most
			// of what OCR reports are dashes, a few pixels high, and their median would rule out all the labels.
			var rawHeights = raw.stream().filter(w -> w.text() != null && w.text().matches(".*[a-zA-Z0-9].*")).mapToDouble(w -> w.boundingBox().getHeight()).sorted().toArray();
			if (rawHeights.length == 0)
				rawHeights = raw.stream().mapToDouble(w -> w.boundingBox().getHeight()).sorted().toArray();
			var medianRawHeight = (rawHeights.length == 0 ? 1.0 : rawHeights[rawHeights.length / 2]);
			var shaped = CaptureWords.filter(raw, 0, 1, 0.4 * medianRawHeight, 2.5 * medianRawHeight);
			// below a tree with the root at the top, or above one with the root at the bottom, labels that are not
			// turned stand side by side in one line: there a gap of the height of the text separates two labels
			var gapFactor = (rot == 0 && rotations().length > 1 ? 1.0 : 2.0);
			var joined = CaptureWords.joinConsecutiveWords(shaped, parameters.isMustStartAlphaNumeric(), parameters.isMustEndAlphaNumeric(), parameters.isMustContainLetter(), gapFactor).stream()
					.filter(w -> w.confidence() >= parameters.getMinWordConfidence() && w.text().length() >= parameters.getMinWordLength())
					.toList();
			// Score each rotation by confidence WEIGHTED BY TEXT LENGTH. A wrong rotation turns the tree's
			// branch lines into many tiny junk "words" (single characters); at high resolution there can be
			// hundreds of them, and a plain confidence sum lets them out-vote the rotation that actually reads
			// the (fewer but far longer) real labels. Weighting by character count makes the real rotation win.
			var score = joined.stream().mapToDouble(w -> w.confidence() * Math.max(1, w.text().replaceAll("\\s+", "").length())).sum();
			if (score > bestScore) {
				bestScore = score;
				if (rot == 0) {
					bestAll = raw;
					bestText = shaped;
					bestWords = joined;
				} else {
					var cw = rot > 0;
					bestAll = raw.stream().map(w -> new OcrWord(w.text(), w.confidence(), ImageUtils.unrotateBox(w.boundingBox(), cw, origW, origH))).toList();
					bestText = shaped.stream().map(w -> new OcrWord(w.text(), w.confidence(), ImageUtils.unrotateBox(w.boundingBox(), cw, origW, origH))).toList();
					bestWords = joined.stream().map(w -> new OcrWord(w.text(), w.confidence(), ImageUtils.unrotateBox(w.boundingBox(), cw, origW, origH))).toList();
				}
			}
		}
		if (bestScore < 0 && ocrError != null)
			throw ocrError;
		withoutLines = null;

		allWords.clear();
		allWords.addAll(bestAll);
		textWords.clear();
		textWords.addAll(bestText);
		words.clear();
		words.addAll(bestWords);
		wordsCaptured = true;
	}

	/**
	 * the image is read as it is, and for a root at the top or bottom also turned by a quarter either way
	 */
	private int[] rotations() {
		return (rootSide == RootPosition.Side.Top || rootSide == RootPosition.Side.Bottom) ? new int[]{0, 1, -1} : new int[]{0};
	}

	private static Image rotate(Image image, int rot) {
		return (rot == 0 ? image : ImageUtils.rotate90(image, rot > 0));
	}

	/**
	 * what OCR reads in the image turned by the given number of quarters, in the coordinates of the turned image;
	 * read once, and null if that failed
	 */
	private List<OcrWord> firstReading(int rot) {
		if (!firstReadings.containsKey(rot)) {
			if (greyScaleImage == null)
				greyScaleImage = Binarization.flatten(getInputImage());
			try {
				var image = rotate(greyScaleImage, rot);
				firstReadings.put(rot, BranchEnds.removeFromWords(image, OCR.getWords(image)));
			} catch (Exception ex) {
				ocrError = ex;
				firstReadings.put(rot, null);
			}
		}
		return firstReadings.get(rot);
	}

	/**
	 * the boxes, in the coordinates of the input image, of the words that are certainly text
	 */
	private List<Rectangle2D> confidentText() {
		var boxes = new ArrayList<Rectangle2D>();
		var origW = (int) getInputImage().getWidth();
		var origH = (int) getInputImage().getHeight();
		for (var rot : rotations()) {
			var words = firstReading(rot);
			if (words == null)
				continue;
			for (var word : words) {
				var text = (word.text() == null ? "" : word.text().strip());
				if (word.confidence() >= CONFIDENT && text.chars().filter(Character::isLetterOrDigit).count() >= 3 && text.chars().noneMatch(ch -> LineInk.LINE_CHARACTERS.indexOf(ch) >= 0))
					boxes.add(rot == 0 ? word.boundingBox() : ImageUtils.unrotateBox(word.boundingBox(), rot > 0, origW, origH));
			}
		}
		return boxes;
	}

	public static Rectangle2D shrink(Rectangle2D rect, int inset) {
		if (rect == null) {
			throw new IllegalArgumentException("Rectangle cannot be null");
		}
		if (inset < 0) {
			throw new IllegalArgumentException("Inset must be non-negative");
		}

		// Ensure x and y do not extend beyond their limits
		var newX = Math.min(rect.getMinX() + inset, rect.getMinX() + rect.getWidth());
		var newY = Math.min(rect.getMinY() + inset, rect.getMinY() + rect.getHeight());

		// Ensure width and height do not go negative
		var newWidth = Math.max(rect.getWidth() - 2 * inset, 1);
		var newHeight = Math.max(rect.getHeight() - 2 * inset, 1);

		return new Rectangle2D(newX, newY, newWidth, newHeight);
	}

	public void clear() {
		inputImage = null;
		clearData();
	}

	public void clearData() {
		updatePhase(getInputImage() != null ? IMAGE : NONE);
		greyScaleImage = null;
		lines = null;
		firstReadings.clear();
		ocrError = null;
		words.clear();
		allWords.clear();
		textWords.clear();
		wordsCaptured = false;
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
	 * set the side on which the root lies; used to decide whether leaf labels may be rotated 90 degrees
	 *
	 * @param rootSide the root side
	 */
	public void setRootSide(RootPosition.Side rootSide) {
		this.rootSide = rootSide;
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
	public ArrayList<OcrWord> getWords() {
		return words;
	}

	public void removeWord(OcrWord word) {
		if (!isRunning()) {
			this.words.remove(word);
		}
	}

	/**
	 * get what OCR found that is text by content and size, whether or not it was accepted as a label. This is
	 * what is erased from the picture before the lines are traced
	 *
	 * @return words list
	 */
	public ArrayList<OcrWord> getTextWords() {
		return textWords;
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

	/**
	 * thrown when the input image is not one that can be captured, such as a photo that is mostly dark. This is not
	 * a program error: the message is meant to be shown to the user as is
	 */
	public static class UnsuitableImageException extends Exception {
		public UnsuitableImageException(String message) {
			super(message);
		}
	}
}

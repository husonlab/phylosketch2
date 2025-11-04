/*
 * PhyloImageAnalyzer.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * analyze the provided image to provide suitability for phylogenetic capture
 * Daniel Huson 11.2025, using ChatGPT
 */
public final class PhyloImageAnalyzer {

	// Configuration thresholds (tweak as needed)
	private static final int MIN_WIDTH = 600;
	private static final int MIN_HEIGHT = 400;
	private static final int RECOMMENDED_MIN_WIDTH = 800;
	private static final int RECOMMENDED_MIN_HEIGHT = 600;
	private static final int TOO_LARGE_DIMENSION = 5000;

	// Brightness thresholds
	private static final double VERY_LIGHT = 0.88; // "white-ish" background
	private static final double VERY_DARK = 0.35;  // "black-ish" foreground
	private static final double MID_TONE_LOW = VERY_DARK;
	private static final double MID_TONE_HIGH = VERY_LIGHT;

	private PhyloImageAnalyzer() {
		// utility class
	}

	/**
	 * Analyze the given image and report potential problems via the warn consumer.
	 * <p>
	 * The analysis is heuristic only; it does not attempt to fully understand the tree,
	 * but checks whether the image looks like a high-contrast dark-on-light drawing
	 * with reasonable line thickness and low noise.
	 *
	 * @param image the JavaFX Image to analyze (must be loaded)
	 * @param warn  a consumer to receive warning or advice messages
	 */
	public static void analyze(Image image, Consumer<String> warn) {
		if (image == null) {
			warn.accept("No image provided. Please load an image containing your tree or network.");
			return;
		}

		PixelReader reader = image.getPixelReader();
		if (reader == null) {
			warn.accept("Cannot read pixels from the image. Make sure the image is fully loaded.");
			return;
		}

		int width = (int) Math.round(image.getWidth());
		int height = (int) Math.round(image.getHeight());

		// 1) Basic size checks
		checkResolution(width, height, warn);

		// Choose a sampling step so that we never sample more than about 800 points
		// along the larger dimension.
		int maxDim = Math.max(width, height);
		int step = Math.max(1, maxDim / 800);

		// Overall statistics
		double totalSamples = 0;
		int lightCount = 0;
		int darkCount = 0;
		int midCount = 0;
		double sumBrightness = 0.0;

		// For color statistics among dark pixels
		int darkColoredCount = 0;

		// For rough line thickness analysis (horizontal runs of dark pixels)
		List<Integer> darkRunLengths = new ArrayList<>();
		int tinyRunCount = 0;

		for (int y = 0; y < height; y += step) {
			int currentRun = 0;

			for (int x = 0; x < width; x += step) {
				Color c = reader.getColor(x, y);
				double b = c.getBrightness();
				double s = c.getSaturation();

				sumBrightness += b;
				totalSamples++;

				boolean isDark = b < VERY_DARK;
				boolean isLight = b > VERY_LIGHT;

				if (isLight) {
					lightCount++;
					if (currentRun > 0) {
						darkRunLengths.add(currentRun);
						if (currentRun <= 2 * step) {
							tinyRunCount++;
						}
						currentRun = 0;
					}
				} else if (isDark) {
					darkCount++;
					currentRun += step; // approximate run length in pixels
					if (s > 0.3) {
						darkColoredCount++;
					}
				} else {
					midCount++;
					if (currentRun > 0) {
						darkRunLengths.add(currentRun);
						if (currentRun <= 2 * step) {
							tinyRunCount++;
						}
						currentRun = 0;
					}
				}
			}

			// Close any run at the end of the row
			if (currentRun > 0) {
				darkRunLengths.add(currentRun);
				if (currentRun <= 2 * step) {
					tinyRunCount++;
				}
			}
		}

		if (totalSamples == 0) {
			warn.accept("Image appears to be empty. Load a file that contains a visible phylogeny.");
			return;
		}

		double lightFraction = lightCount / totalSamples;
		double darkFraction = darkCount / totalSamples;
		double midFraction = midCount / totalSamples;
		double avgBrightness = sumBrightness / totalSamples;

		// 2) Background vs foreground checks
		checkBackgroundAndContrast(lightFraction, darkFraction, midFraction, avgBrightness, warn);

		// 3) Border checks: is the tree touching the margins?
		checkBorders(reader, width, height, step, warn);

		// 4) Noise & line thickness checks
		checkLineThicknessAndNoise(darkRunLengths, tinyRunCount, darkFraction, warn);

		// 5) Color usage in dark regions
		checkColorUsageInDarkRegions(darkCount, darkColoredCount, warn);
	}

	private static void checkResolution(int width, int height, Consumer<String> warn) {
		if (width < MIN_WIDTH || height < MIN_HEIGHT) {
			warn.accept(String.format(
					"Image size is %dx%d pixels. This is quite small; the tree and labels may be hard to analyze. " +
					"(Recommend at least %dx%d pixels).",
					width, height, RECOMMENDED_MIN_WIDTH, RECOMMENDED_MIN_HEIGHT
			));
		} else if (width < RECOMMENDED_MIN_WIDTH || height < RECOMMENDED_MIN_HEIGHT) {
			warn.accept(String.format(
					"Image size is %dx%d pixels. This is usable but somewhat small. " +
					"(Recommend at least %dx%d pixels).",
					width, height, RECOMMENDED_MIN_WIDTH, RECOMMENDED_MIN_HEIGHT
			));
		}

		if (width > TOO_LARGE_DIMENSION || height > TOO_LARGE_DIMENSION) {
			warn.accept(String.format(
					"Image size is %dx%d pixels. This is very large and may cause problems. " +
					"Downscale the image around 1200–2400 pixels on the longer side and reload.",
					width, height
			));
		}
	}

	private static void checkBackgroundAndContrast(double lightFraction,
												   double darkFraction,
												   double midFraction,
												   double avgBrightness,
												   Consumer<String> warn) {
		// Expect most pixels to be very light (white background)
		if (lightFraction < 0.5) {
			warn.accept(String.format(
					"Only about %.0f%% of sampled pixels are very light. The background may not be white. " +
					"Please increase brightness/contrast and reload.",
					lightFraction * 100.0
			));
		}

		// Expect a small but visible fraction of dark pixels (edges & labels)
		if (darkFraction < 0.005) { // < 0.5%
			warn.accept(String.format(
					"Only about %.2f%% of sampled pixels are very dark. Phylogeny may be too faint or too thin. " +
					"Ensure image has solid black or dark gray lines and consider increasing the image resolution.",
					darkFraction * 100.0
			));
		} else if (darkFraction > 0.4) { // > 40%
			warn.accept(String.format(
					"About %.0f%% of sampled pixels are very dark. It looks like a dark background with light lines, " +
					"or a very dense graphic. For best results, use a white background and dark edges/labels.",
					darkFraction * 100.0
			));
		}

		// Many mid-tone pixels might indicate low contrast or heavy anti-aliasing
		if (midFraction > 0.5 && avgBrightness < 0.8 && avgBrightness > 0.2) {
			warn.accept(String.format(
					"A large fraction (≈%.0f%%) of pixels are mid-gray, suggesting low contrast. " +
					"Increase contrast or export the tree as a high-contrast graphic (e.g. PNG with black lines on white background).",
					midFraction * 100.0
			));
		}
	}

	private static void checkBorders(PixelReader reader,
									 int width,
									 int height,
									 int step,
									 Consumer<String> warn) {
		int borderSamples = 0;
		int borderDark = 0;

		// Top and bottom borders
		for (int x = 0; x < width; x += step) {
			Color top = reader.getColor(x, 0);
			Color bottom = reader.getColor(x, height - 1);
			if (top.getBrightness() < VERY_DARK) borderDark++;
			if (bottom.getBrightness() < VERY_DARK) borderDark++;
			borderSamples += 2;
		}

		// Left and right borders
		for (int y = 0; y < height; y += step) {
			Color left = reader.getColor(0, y);
			Color right = reader.getColor(width - 1, y);
			if (left.getBrightness() < VERY_DARK) borderDark++;
			if (right.getBrightness() < VERY_DARK) borderDark++;
			borderSamples += 2;
		}

		if (borderSamples == 0) {
			return;
		}

		double borderDarkFraction = borderDark / (double) borderSamples;
		if (borderDarkFraction > 0.25) {
			warn.accept(String.format(
					"About %.0f%% of the border pixels are dark. Parts of the tree likely touch or cross the image edges. " +
					"Please leave a white margin around the tree so that it is fully contained in the image.",
					borderDarkFraction * 100.0
			));
		}
	}

	private static void checkLineThicknessAndNoise(List<Integer> darkRunLengths,
												   int tinyRunCount,
												   double darkFraction,
												   Consumer<String> warn) {
		if (darkRunLengths.isEmpty()) {
			// If we also have a low darkFraction, this probably means nothing recognizable
			if (darkFraction < 0.01) {
				warn.accept("No clear dark lines detected. The tree might be missing, extremely faint, or in a color that is not very dark. " +
							"Ensure the image has solid black edges on a white background.");
			}
			return;
		}

		double sum = 0.0;
		for (int len : darkRunLengths) {
			sum += len;
		}
		double avgRunLength = sum / darkRunLengths.size();

		// Tiny run fraction → noise/compression
		double tinyFraction = darkRunLengths.isEmpty() ? 0.0 :
				(double) tinyRunCount / darkRunLengths.size();

		if (avgRunLength < 1.5) {
			warn.accept(String.format(
					"Estimated line thickness is about %.1f pixels. Lines may be very thin and close to 1 pixel wide. " +
					"Consider increasing the stroke width or exporting at a higher resolution.",
					avgRunLength
			));
		} else if (avgRunLength > 10.0) {
			warn.accept(String.format(
					"Estimated line thickness is about %.1f pixels. Lines may be too thick, so branch junctions are blurred together. " +
					"Try reducing the stroke width or downscaling the image slightly.",
					avgRunLength
			));
		}

		if (tinyFraction > 0.7 && darkFraction < 0.2) {
			warn.accept(String.format(
					"Many small, isolated dark segments were detected (≈%.0f%% of dark runs). " +
					"If this is due to noise or compression artifacts, please clean the image before use.",
					tinyFraction * 100.0
			));
		}
	}

	private static void checkColorUsageInDarkRegions(int darkCount,
													 int darkColoredCount,
													 Consumer<String> warn) {
		if (darkCount <= 0) {
			return;
		}
		double coloredFraction = darkColoredCount / (double) darkCount;
		if (coloredFraction > 0.3) {
			warn.accept(String.format(
					"About %.0f%% of dark pixels appear to be strongly colored rather than black/gray. " +
					"The recognition works best if tree edges and labels are black or dark gray on a white background.",
					coloredFraction * 100.0
			));
		}
	}
}
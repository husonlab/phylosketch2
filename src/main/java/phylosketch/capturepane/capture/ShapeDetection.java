/*
 * ShapeDetection.java Copyright (C) 2025 Daniel H. Huson
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

import java.util.ArrayList;
import java.util.List;

public class ShapeDetection {

	public static List<Region> analyzeRegions(int[][] labels) {
		List<Region> regions = new ArrayList<>();
		int height = labels.length;
		int width = labels[0].length;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int label = labels[y][x];
				if (label > 0) {
					// Find or create the region for this label
					Region region = regions.stream().filter(r -> r.label == label).findFirst().orElse(null);
					if (region == null) {
						region = new Region(label);
						regions.add(region);
					}
					// Update the region properties
					region.update(x, y);
				}
			}
		}

		// Filter regions based on aspect ratio and black pixel proportion
		regions.removeIf(r -> {
			double aspectRatio = r.getAspectRatio();
			double blackProportion = r.getBlackPixelProportion();
			return aspectRatio > 5.0 || aspectRatio < 0.2 || blackProportion < 0.5; // Adjust thresholds as needed
		});

		return regions;
	}

	// Perform Connected Component Analysis (Label connected regions)
	public static int[][] connectedComponents(int[][] binary) {
		int height = binary.length;
		int width = binary[0].length;
		int[][] labels = new int[height][width];
		int label = 1;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (binary[y][x] == 1 && labels[y][x] == 0) {
					floodFill(binary, labels, x, y, label++);
				}
			}
		}
		return labels;
	}

	// Flood fill algorithm for labeling connected regions
	private static void floodFill(int[][] binary, int[][] labels, int x, int y, int label) {
		int height = binary.length;
		int width = binary[0].length;

		if (x < 0 || y < 0 || x >= width || y >= height) return; // Out of bounds
		if (binary[y][x] == 0 || labels[y][x] != 0) return;      // Already labeled or background

		labels[y][x] = label;

		// Recursively fill neighboring pixels
		floodFill(binary, labels, x + 1, y, label);
		floodFill(binary, labels, x - 1, y, label);
		floodFill(binary, labels, x, y + 1, label);
		floodFill(binary, labels, x, y - 1, label);
	}

	// Helper class to represent a connected region
	static public class Region {
		final int label;
		int minX, minY, maxX, maxY;
		int blackPixelCount;

		Region(int label) {
			this.label = label;
			this.minX = Integer.MAX_VALUE;
			this.minY = Integer.MAX_VALUE;
			this.maxX = Integer.MIN_VALUE;
			this.maxY = Integer.MIN_VALUE;
			this.blackPixelCount = 0;
		}

		void update(int x, int y) {
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			blackPixelCount++; // Increment black pixel count for each pixel in the region
		}

		double getAspectRatio() {
			int width = maxX - minX + 1;
			int height = maxY - minY + 1;
			return (double) width / height;
		}

		double getBlackPixelProportion() {
			int width = maxX - minX + 1;
			int height = maxY - minY + 1;
			int boundingBoxArea = width * height;
			return (double) blackPixelCount / boundingBoxArea;
		}
	}
}

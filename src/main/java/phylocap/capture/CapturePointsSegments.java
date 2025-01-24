/*
 * CapturePointsSegments.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.scene.image.Image;
import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;
import net.sourceforge.tess4j.Word;

import java.util.ArrayList;
import java.util.List;

public class CapturePointsSegments {
	public static Image apply(ProgressListener progress, Image image, List<Word> words, int minDistancePoints, List<Point> endPoints, List<Segment> segments) throws CanceledException {
		endPoints.clear();
		segments.clear();

		var theMaskedImage = ImageUtils.replaceRectanglesWithWhite(image, words.stream().map(Word::getBoundingBox).toList(), 2);
		var matrix = ImageUtils.convertToBinaryArray(theMaskedImage, 0.95);
		Skeletonization.apply(matrix);
		var maskedImage = ImageUtils.convertToImage(matrix);

		var points = ProcessingUtils.detectEndPoints(matrix);
		points.addAll(ProcessingUtils.detectBranchPoints(matrix));

		if (minDistancePoints > 0)
			points = ProcessingUtils.removeClosePoints(points, minDistancePoints);

		var sorted = new ArrayList<>(ProcessingUtils.findPaths(progress, matrix, points));
		sorted.sort((a, b) -> -Double.compare(a.first().distance(a.last()), b.first().distance(b.last())));
		for (var segment : sorted) {
			if (segments.stream().noneMatch(that -> that.contains(segment, 3)))
				segments.add(segment);
		}

		endPoints.addAll(points);
		return maskedImage;

	}
}

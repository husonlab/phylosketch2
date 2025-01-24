/*
 * DustRemoval.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.util.BitSetUtils;
import jloda.util.CanceledException;
import jloda.util.CollectionUtils;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * removes all the dust from the list of segments
 * Daniel Huson, 1.2025
 */
public class DustRemoval {

	public static ArrayList<Segment> apply(ProgressListener progress, ArrayList<Segment> segments, double maxDistance, double minExtent) throws CanceledException {
		// determine all clusters of segments
		progress.setMaximum(segments.size());
		progress.setProgress(0);
		var clusters = new LinkedList<ArrayList<Segment>>();
		for (var segment : segments) {
			var closeClusters = new BitSet();
			for (int i = 0; i < clusters.size(); i++) {
				var cluster = clusters.get(i);
				if (close(segment, cluster, maxDistance)) {
					closeClusters.set(i);
				}
			}
			if (closeClusters.isEmpty()) {
				clusters.add(new ArrayList<>(List.of(segment)));
			} else {
				var newList = new ArrayList<Segment>();
				newList.add(segment);
				for (var i : BitSetUtils.members(closeClusters)) {
					newList.addAll(clusters.get(i));
				}
				clusters.add(newList);
				for (int i : CollectionUtils.reverse(BitSetUtils.asList(closeClusters))) {
					clusters.remove(i);
				}
			}
			progress.incrementProgress();
		}

		progress.setMaximum(clusters.size());
		progress.setProgress(0);
		var clustersSizes = new ArrayList<Pair<ArrayList<Segment>, Integer>>();
		for (var cluster : clusters) {
			clustersSizes.add(new Pair<>(cluster, computeMinExtent(cluster)));
			progress.incrementProgress();
		}
		clustersSizes.sort((a, b) -> -Integer.compare(a.getSecond(), b.getSecond()));
		progress.setProgress(-1);

		var result = new ArrayList<Segment>();
		for (var pair : clustersSizes) {
			if (!result.isEmpty() && pair.getSecond() < minExtent)
				break;
			result.addAll(pair.getFirst());
		}
		System.err.println("out: " + result.size());

		return result;
	}

	public static boolean close(Segment segment, List<Segment> list, double maxDistance) {
		for (var other : list) {
			if (segment.proximal(other, maxDistance))
				return true;
		}
		return false;
	}

	public static int computeMinExtent(ArrayList<Segment> list) {
		var minX = Integer.MAX_VALUE;
		var maxX = Integer.MIN_VALUE;
		var minY = Integer.MAX_VALUE;
		var maxY = Integer.MIN_VALUE;

		for (var segment : list) {
			for (var p : segment.points()) {
				minX = Math.min(minX, p.x());
				maxX = Math.max(maxX, p.x());
				minY = Math.min(minY, p.y());
				maxY = Math.max(maxY, p.y());
			}
		}
		return Math.min(maxX - minX + 1, maxY - minY + 1);
	}
}

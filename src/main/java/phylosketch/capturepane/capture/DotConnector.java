/*
 * DotConnector.java Copyright (C) 2025 Daniel H. Huson
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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class DotConnector {

	private final int[][] image;
	private final int width;
	private final int height;
	private final boolean[][] visited;

	private final int minChainLength = 8;
	private final int distanceThreshold = 10;

	public static void apply(int[][] image) {
		(new DotConnector(image)).connectDots();
	}

	public DotConnector(int[][] image) {
		this.image = image;
		this.height = image.length;
		this.width = image[0].length;
		this.visited = new boolean[height][width];
	}

	public void connectDots() {
		List<Point> dots = findDots();

		// Try to grow chains starting from each unvisited dot
		for (Point start : dots) {
			if (visited[start.y][start.x]) continue;
			List<Point> chain = growChain(start, dots);

			if (chain.size() >= minChainLength) {
				drawLineThrough(chain);
			}
		}
	}

	private List<Point> findDots() {
		List<Point> dots = new ArrayList<>();
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (image[y][x] == 1 && isIsolated(x, y)) {
					dots.add(new Point(x, y));
				}
			}
		}
		return dots;
	}

	private boolean isIsolated(int x, int y) {
		// Check if the black pixel is mostly surrounded by white (i.e. a dot)
		int blackCount = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0) continue;
				if (image[y + dy][x + dx] == 1) blackCount++;
			}
		}
		return blackCount <= 2;
	}

	private List<Point> growChain(Point start, List<Point> allDots) {
		List<Point> chain = new ArrayList<>();
		chain.add(start);
		visited[start.y][start.x] = true;

		Point last = start;

		while (true) {
			Point next = null;
			double bestScore = Double.MAX_VALUE;

			for (Point candidate : allDots) {
				if (visited[candidate.y][candidate.x]) continue;

				double d = distance(last, candidate);
				if (d > distanceThreshold) continue;

				if (!angleMatch(chain, candidate)) continue;

				if (d < bestScore) {
					next = candidate;
					bestScore = d;
				}
			}

			if (next == null) break;

			chain.add(next);
			visited[next.y][next.x] = true;
			last = next;
		}

		return chain;
	}

	private boolean angleMatch(List<Point> chain, Point newPoint) {
		if (chain.size() < 2) return true;
		Point a = chain.get(chain.size() - 2);
		Point b = chain.get(chain.size() - 1);
		double angle1 = Math.atan2(b.y - a.y, b.x - a.x);
		double angle2 = Math.atan2(newPoint.y - b.y, newPoint.x - b.x);
		double diff = Math.abs(angle1 - angle2);
		return diff < Math.PI / 8; // ~22.5 degrees
	}

	private void drawLineThrough(List<Point> chain) {
		for (int i = 0; i < chain.size() - 1; i++) {
			drawLine(chain.get(i), chain.get(i + 1));
		}
	}

	private void drawLine(Point a, Point b) {
		int dx = Math.abs(b.x - a.x), dy = Math.abs(b.y - a.y);
		int sx = Integer.compare(b.x, a.x);
		int sy = Integer.compare(b.y, a.y);
		int err = dx - dy;

		int x = a.x, y = a.y;
		while (true) {
			image[y][x] = 1;
			if (x == b.x && y == b.y) break;
			int e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private double distance(Point a, Point b) {
		int dx = a.x - b.x, dy = a.y - b.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public int[][] getImage() {
		return image;
	}
}
/*
 * PhyloSketch1Import.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.io;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import jloda.fx.util.FontUtils;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.IOExceptionWithLineNumber;
import jloda.util.parse.NexusStreamParser;
import phylosketch.paths.PathUtils;
import phylosketch.utils.ColorUtils;
import phylosketch.utils.CubicCurve;
import phylosketch.view.DrawView;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

/**
 * network block nexus input
 * Daniel Huson, 2.2018
 */
public class PhyloSketch1Import {
	/**
	 * parse a network block
	 *
	 * @return taxa labels found
	 */
	public static void apply(String fileName, DrawView view) throws IOException {
		view.clear();

		try (var np = new NexusStreamParser(new FileReader(fileName))) {
			np.matchIgnoreCase("#nexus");
			if (np.peekMatchAnyTokenIgnoreCase("begin taxa")) {
				np.matchBeginBlock("taxa");
				np.skipBlock();
			}

			np.matchBeginBlock("network");

			np.matchIgnoreCase("dimensions nNodes=");
			final int nNodes = np.getInt(0, Integer.MAX_VALUE);
			np.matchIgnoreCase("nEdges=");
			final int nEdges = np.getInt(0, Integer.MAX_VALUE);
			np.matchIgnoreCase(";");

			np.matchIgnoreCase("TYPE=other;");

			var id2node = new TreeMap<Integer, Node>();

			np.matchIgnoreCase("NODES");

			{
				boolean first = true;
				for (int i = 0; i < nNodes; i++) {
					if (first)
						first = false;
					else
						np.matchIgnoreCase(",");

					np.matchIgnoreCase("id=");
					final int id = np.getInt();
					if (id2node.containsKey(id))
						throw new IOExceptionWithLineNumber("Multiple occurrence of node id: " + id, np.lineno());

					var v = view.createNode();
					id2node.put(id, v);

					while (!np.peekMatchAnyTokenIgnoreCase(", ;")) {
						var key = np.getWordRespectCase().toLowerCase();
						np.matchIgnoreCase("=");
						var value = np.getWordRespectCase();
						switch (key) {
							case "label" -> view.setLabel(v, value);
							case "clr" -> {
								ColorUtils.setFill(DrawView.getShape(v), value, "graph-node");
								ColorUtils.setStroke(DrawView.getShape(v), Color.BLACK, "graph-node");
							}
							case "x" ->
									view.setLocation(v, new Point2D(Double.parseDouble(value), view.getLocation(v).getY()));
							case "y" ->
									view.setLocation(v, new Point2D(view.getLocation(v).getX(), Double.parseDouble(value)));
							case "w", "h" -> {
								if (DrawView.getShape(v) != null)
									DrawView.getShape(v).setSize(Double.parseDouble(value));
							}
							case "font" -> {
								var font = FontUtils.valueOf(value);
								var label = DrawView.getLabel(v);
								label.setFontSize(font.getSize());
								label.setFontFamily(font.getFamily());
								label.setBold(font.getStyle().contains("bold"));
								label.setItalic(font.getStyle().contains("italic"));
							}
							case "lr" -> {
								var angle = Double.parseDouble(value);
								System.err.println("label rotate: to implemented");
							}
						}
					}
				}
			}
			np.matchIgnoreCase(";");

			np.matchIgnoreCase("EDGES");
			{
				var id2edge = new TreeMap<Integer, Edge>();

				var first = true;
				for (var i = 0; i < nEdges; i++) {
					if (first)
						first = false;
					else
						np.matchIgnoreCase(",");

					np.matchIgnoreCase("id=");
					var id = np.getInt();
					if (id2edge.containsKey(id))
						throw new IOExceptionWithLineNumber("Multiple occurrence of edge id: " + id, np.lineno());

					np.matchIgnoreCase("sid=");
					var sid = np.getInt();
					if (!id2node.containsKey(sid))
						throw new IOExceptionWithLineNumber("Unknown node id: " + sid, np.lineno());

					np.matchIgnoreCase("tid=");
					var tid = np.getInt();
					if (!id2node.containsKey(tid))
						throw new IOExceptionWithLineNumber("Unknown node id: " + tid, np.lineno());

					var source = id2node.get(sid);
					var target = id2node.get(tid);

					var path = new Path();
					var e = view.createEdge(source, target, path);
					id2edge.put(id, e);

					var c1x = Double.MAX_VALUE;
					var c1y = Double.MAX_VALUE;
					var c2x = Double.MAX_VALUE;
					var c2y = Double.MAX_VALUE;
					var arrow = true;

					while (!np.peekMatchAnyTokenIgnoreCase(", ;")) {
						var key = np.getWordRespectCase();
						np.matchIgnoreCase("=");
						var value = np.getWordRespectCase();
						switch (key) {
							case "label" -> view.setLabel(e, value);
							case "sw" -> path.setStrokeWidth(Double.parseDouble(value));
							case "c1x" -> c1x = Double.parseDouble(value);
							case "c1y" -> c1y = Double.parseDouble(value);
							case "c2x" -> c2x = Double.parseDouble(value);
							case "c2y" -> c2y = Double.parseDouble(value);
							case "clr" -> ColorUtils.setStroke(path, value, "graph-edge");
							case "font" -> {
								var font = FontUtils.valueOf(value);
								var label = DrawView.getLabel(e);
								label.setFontSize(font.getSize());
								label.setFontFamily(font.getFamily());
								label.setBold(font.getStyle().contains("bold"));
								label.setItalic(font.getStyle().contains("italic"));
							}
							case "arw" -> arrow = Boolean.parseBoolean(value);
						}
					}
					if (c1x != Double.MAX_VALUE && c2x != Double.MAX_VALUE && c1y != Double.MAX_VALUE && c2y != Double.MAX_VALUE) {
						path.getElements().setAll(PathUtils.createPath(CubicCurve.apply(view.getLocation(source), new Point2D(c1x, c1y), new Point2D(c2x, c2y), view.getLocation(target), 4), true).getElements());
					} else {
						path.getElements().setAll(PathUtils.createPath(List.of(view.getLocation(source), view.getLocation(target)), true).getElements());
					}
					view.setShowArrow(e, arrow);
				}
			}
			np.matchIgnoreCase(";");
			np.matchEndBlock();
		}
	}

	public static boolean looksLikePhyloSketch1(String firstLine) {
		return firstLine != null && firstLine.toLowerCase().startsWith("#nexus") && firstLine.toLowerCase().contains("splitstree5 compatible");
	}
}

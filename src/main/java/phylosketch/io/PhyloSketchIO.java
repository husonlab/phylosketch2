/*
 * PhyloSketchIO.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.io;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.ColorUtilsFX;
import jloda.fx.window.MainWindowManager;
import jloda.graph.Edge;
import jloda.graph.io.GraphGML;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import phylosketch.paths.PathUtils;
import phylosketch.view.DrawView;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * input and output of created network
 * Daniel Huson, 9.2024
 */
public class PhyloSketchIO {
	public static void save(File file, DrawView drawView) throws IOException {
		try(var w= FileUtils.getOutputWriterPossiblyZIPorGZIP(file.getPath())) {
			save(w, drawView);
		}
	}

	/**
	 * save the phylo sketch in GML format
	 * @param w writer
	 * @param view the draw window
	 * @throws IOException  something went wrong
	 */
	public static void save(Writer w, DrawView view) throws IOException {
		var graph = view.getGraph();
		var nodeKeyNames = List.of("taxon", "shape", "size", "fill", "x", "y", "label", "label_dx", "label_dy");
		var edgeKeyNames = List.of("weight", "confidence", "probability", "path", "stroke", "stroke_dash_array", "stroke_width", "arrow", "label");
		var comment="Created by PhyloSketch App on %s".formatted(Basic.getDateString("yyyy-MM-dd HH:mm:ss"));
		GraphGML.writeGML(graph, comment, graph.getName(), true, 1, w,
				nodeKeyNames, (key, v) -> {
					var shape = (Shape) v.getData();
					var label = (RichTextLabel) v.getInfo();
					var value = String.valueOf(switch (key) {
						case "taxon" -> graph.hasTaxa(v) ? graph.getTaxon(v) : "";
						case "shape" -> shape.getClass().getSimpleName().toLowerCase();
						case "size" -> {
							var buf=new StringBuilder();
							if(shape instanceof Rectangle rectangle) {
								buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f",rectangle.getWidth()));
								buf.append(",");
								buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f",rectangle.getHeight()));
							} else if(shape instanceof Circle circle) {
								buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f",circle.getRadius()));
							} else {
								buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getBoundsInLocal().getWidth()));
								buf.append(",");
								buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getBoundsInLocal().getHeight()));
							}
							yield buf.toString();
						}
						case "fill" ->	(shape.getFill()!=null
										   && !(!MainWindowManager.isUseDarkTheme() && shape.getFill() == Color.BLACK)
										   && !(MainWindowManager.isUseDarkTheme() && shape.getFill()==Color.WHITE))? shape.getFill():"";
						case "x" -> StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getTranslateX());
						case "y" ->  StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getTranslateY());
						case "label" -> DrawView.getLabel(v).getText().trim();
						case "label_dx" -> label != null ?  StringUtils.removeTrailingZerosAfterDot("%.1f",label.getLayoutX()) : "";
						case "label_dy" -> label != null ? StringUtils.removeTrailingZerosAfterDot("%.1f",label.getLayoutY()) : "";
						default -> "";
					});
					return (value.isBlank() ? null : value);
				}, edgeKeyNames, (key, e) -> {
					var path = (Path) e.getData();

					var value = (path == null ? "" : String.valueOf(switch (key) {
						case "path" -> pathToString(path);
						case "stroke" ->
								(path.getStroke()!=null
								 && !(!MainWindowManager.isUseDarkTheme() && path.getStroke() == Color.BLACK)
								 && !(MainWindowManager.isUseDarkTheme() && path.getStroke()==Color.WHITE))? path.getStroke():"";
						case "stroke_dash_array" ->
								path.getStrokeDashArray().isEmpty() ? "" : StringUtils.toString(path.getStrokeDashArray(), ",");
						case "stroke_width" -> path.getStrokeWidth()!=1?path.getStrokeWidth():"";
						case "weight" ->
								graph.hasEdgeWeights() && e.getTarget().getInDegree() <= 1 ? StringUtils.removeTrailingZerosAfterDot(graph.getWeight(e)) : "";
						case "confidence" ->
								graph.hasEdgeConfidences() ? StringUtils.removeTrailingZerosAfterDot(graph.getConfidence(e)) : "";
						case "probability" ->
								graph.hasEdgeProbabilities() && e.getTarget().getInDegree() > 1 ? StringUtils.removeTrailingZerosAfterDot(graph.getProbability(e)) : "";
						case "arrow" -> view.getEdgeArrowMap().containsKey(e) ? "1" : "";
						case "label" -> DrawView.getLabel(e).getText().trim();
						default -> "";
					}));
					return (value.isBlank() ? null : value);
				});
	}

	/**
	 * open a file and load it into the draw window
	 * @param file file
	 * @param drawView draw window
	 * @throws IOException
	 */
	public static void open(String file, DrawView drawView) throws IOException {
		 try(var r=new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(file))) {
			 load(r, drawView);
			 drawView.getUndoManager().clear();
		 }
	}

	/**
	 * load a a phylo-sketch, previously saved in GML format
	 * @param r reader
	 * @param view the draw window
	 * @throws IOException something went wrong
	 */
	public static void load(Reader r, DrawView view) throws IOException {
		view.clear();
		var graph = view.getGraph();
		var arrowEdges = new HashSet<Edge>();

		var gmlInfo=GraphGML.readGML(r, graph, (key, v, value) -> {
			switch (key) {
				case "tax_id" -> {
					graph.addTaxon(v, Integer.parseInt(value));
				}
				case "shape" -> {
					var shape = value.equals("square") ? new Rectangle(3, 3) : new Circle(3);
					view.setShape(v, shape);
				}
				case "size" -> {
					if (v.getData() instanceof Shape shape) {
						var tokens = StringUtils.split(value, ',');
						if (shape instanceof Rectangle rectangle) {
							if (tokens.length == 1 && NumberUtils.isDouble(tokens[0])) {
								var w = NumberUtils.parseDouble(tokens[0]);
								rectangle.setWidth(w);
								rectangle.setHeight(w);
							} else if (tokens.length >= 2 && NumberUtils.isDouble(tokens[0]) && NumberUtils.isDouble(tokens[1])) {
								var w = NumberUtils.parseDouble(tokens[0]);
								var h = NumberUtils.parseDouble(tokens[1]);
								rectangle.setWidth(w);
								rectangle.setHeight(h);
							}
						} else if (shape instanceof Circle circle) {
							if (tokens.length >= 1 && NumberUtils.isDouble(tokens[0])) {
								var w = NumberUtils.parseDouble(tokens[0]);
								circle.setRadius(w);
							}
						}
					}
				}
				case "fill" -> {
					if (v.getData() instanceof Shape shape && ColorUtilsFX.isColor(value)) {
						shape.setFill(ColorUtilsFX.parseColor(value));
					}
				}
				case "x" -> {
					if (v.getData() instanceof Shape shape && NumberUtils.isDouble(value)) {
						shape.setTranslateX(NumberUtils.parseDouble(value));
					}
				}
				case "y" -> {
					if (v.getData() instanceof Shape shape && NumberUtils.isDouble(value)) {
						shape.setTranslateY(NumberUtils.parseDouble(value));
					}
				}
				case "label" -> {
					view.ensureLabelExists(v);
					view.setLabel(v, value);
				}
				case "label_dx" -> {
					if (NumberUtils.isDouble(value)) {
						view.ensureLabelExists(v);
						DrawView.getLabel(v).setLayoutX(NumberUtils.parseDouble(value));
					}
				}
				case "label_dy" -> {
					if (NumberUtils.isDouble(value)) {
						view.ensureLabelExists(v);
						DrawView.getLabel(v).setLayoutY(NumberUtils.parseDouble(value));
					}
				}
			}
		}, (key, e, value) -> {
			switch (key) {
				case "weight" -> {
					if (NumberUtils.isDouble(value))
						graph.setWeight(e, NumberUtils.parseDouble(value));
				}
				case "confidence" -> {
					if (NumberUtils.isDouble(value))
						graph.setConfidence(e, NumberUtils.parseDouble(value));
				}
				case "probability" -> {
					if (NumberUtils.isDouble(value))
						graph.setProbability(e, NumberUtils.parseDouble(value));
				}
				case "path" -> {
					var path = stringToPath(value);
					path.getStyleClass().add("graph-edge");
					view.addPath(e, path);
				}
				case "stroke" -> {
					if (e.getData() instanceof Path path && ColorUtilsFX.isColor(value)) {
						path.setStroke(ColorUtilsFX.parseColor(value));
					}
				}
				case "stroke_width" -> {
					if (e.getData() instanceof Path path && NumberUtils.isDouble(value)) {
						path.setStrokeWidth(NumberUtils.parseDouble(value));
					}
				}
				case "stroke_dash_array" -> {
					var items = StringUtils.split(value, ',');
					if (Arrays.stream(items).allMatch(NumberUtils::isDouble) && DrawView.getPath(e) != null) {
						DrawView.getPath(e).getStrokeDashArray().setAll(Arrays.stream(items).map(NumberUtils::parseDouble).toList());
					}
				}
				case "arrow" -> {
					if (value.equals("1"))
						arrowEdges.add(e);
				}
				case "label" -> {
					view.ensureLabelExists(e);
					view.setLabel(e, value);
				}
			}
		});

		// create shapes for any nodes for which shape not given
		for(var v:graph.nodes()) {
			if(!(v.getData() instanceof Shape)) {
				view.setShape(v, new Circle(3));
				view.ensureLabelExists(v);
			}
			view.ensureLabelExists(v);
		}
		// create paths for any edges for which path not given
		for(var e:graph.edges()) {
			if(!(e.getData() instanceof Path)) {
				var a=new Point2D(((Shape)e.getSource().getData()).getTranslateX(), ((Shape)e.getSource().getData()).getTranslateY());
				var b=new Point2D(((Shape)e.getTarget().getData()).getTranslateX(), ((Shape)e.getTarget().getData()).getTranslateY());
				var path = PathUtils.createPath(List.of(a, b), true);
				view.addPath(e, path);
			}
			view.ensureLabelExists(e);
		}

		graph.setName(gmlInfo.label());
		if (!arrowEdges.isEmpty()) {
			Platform.runLater(() -> {
				for (var f : arrowEdges) {
					view.setShowArrow(f, true);
				}
			});
		}
	}

	private static String pathToString(Path path) {
		var buf = new StringBuilder();
		if (path != null) {
			for (var item : path.getElements()) {
				var point = PathUtils.getCoordinates(item);
				if (!buf.isEmpty())
					buf.append(",");
				buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f", point.getX()));
				buf.append(",");
				buf.append(StringUtils.removeTrailingZerosAfterDot("%.1f", point.getY()));
			}
		}
		return buf.toString();
	}

	private static Path stringToPath(String text) {
		var tokens = text.split(",");

		var path = new Path();
		path.getStyleClass().add("graph-edge");
		for (var i = 0; i + 1 < tokens.length; i += 2) {
			var x = Double.parseDouble(tokens[i]);
			var y = Double.parseDouble(tokens[i + 1]);
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y));
			} else {
				path.getElements().add(new LineTo(x, y));
			}
		}
		return path;
	}
}


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
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.ColorUtilsFX;
import jloda.fx.window.MainWindowManager;
import jloda.graph.Edge;
import jloda.graph.io.GraphGML;
import jloda.phylo.LSAUtils;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import phylosketch.paths.EdgePath;
import phylosketch.utils.ColorUtils;
import phylosketch.view.DrawView;
import phylosketch.view.NodeShape;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * input and output of created network
 * Daniel Huson, 9.2024
 */
public class PhyloSketchIO {
	public static void save(File file, DrawView drawView, ImageView backgroundImageView) throws IOException {
		try(var w= FileUtils.getOutputWriterPossiblyZIPorGZIP(file.getPath())) {
			save(w, drawView, backgroundImageView);
		}
	}

	/**
	 * save the phylo sketch in GML format
	 * @param w writer
	 * @param view the draw window
	 * @throws IOException  something went wrong
	 */
	public static void save(Writer w, DrawView view, ImageView backgroundImageView) throws IOException {
		var graph = view.getGraph();
		var nodeKeyNames = List.of("taxon", "shape", "size", "stroke", "fill", "x", "y", "label", "label_dx", "label_dy", "label_angle");
		var edgeKeyNames = List.of("weight", "confidence", "probability", "path", "stroke", "stroke_dash_array", "stroke_width", "arrow", "label", "acceptor");
		var comment="Created by PhyloSketch App on %s".formatted(Basic.getDateString("yyyy-MM-dd HH:mm:ss"));
		GraphGML.writeGML(graph, comment, graph.getName(), true, 1, w,
				nodeKeyNames, (key, v) -> {
					var shape = DrawView.getShape(v);
					var label = (RichTextLabel) v.getInfo();
					var value = String.valueOf(switch (key) {
						case "taxon" -> graph.hasTaxa(v) ? graph.getTaxon(v) : "";
						case "shape" -> DrawView.getShape(v).getType().name();
						case "size" -> {
							var size = shape.getSize();
							yield StringUtils.removeTrailingZerosAfterDot("%.1f", size) + "," + StringUtils.removeTrailingZerosAfterDot("%.1f", size);
						}
						case "stroke" -> (shape.getFill() != null
										  && !(!MainWindowManager.isUseDarkTheme() && shape.getFill() == Color.WHITE)
										  && !(MainWindowManager.isUseDarkTheme() && shape.getFill() == Color.BLACK)) ? shape.getStroke() : "";
						case "fill" ->	(shape.getFill()!=null
										   && !(!MainWindowManager.isUseDarkTheme() && shape.getFill() == Color.BLACK)
										   && !(MainWindowManager.isUseDarkTheme() && shape.getFill()==Color.WHITE))? shape.getFill():"";
						case "x" -> StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getTranslateX());
						case "y" ->  StringUtils.removeTrailingZerosAfterDot("%.1f",shape.getTranslateY());
						case "label" -> (label.getRawText().isBlank() ? "" : DrawView.getLabel(v).getText().trim());
						case "label_dx" -> label != null ?  StringUtils.removeTrailingZerosAfterDot("%.1f",label.getLayoutX()) : "";
						case "label_dy" -> label != null ? StringUtils.removeTrailingZerosAfterDot("%.1f",label.getLayoutY()) : "";
						case "label_angle" ->
								label != null && label.getRotate() != 0 ? StringUtils.removeTrailingZerosAfterDot("%.1f", label.getRotate()) : "";
						default -> "";
					});
					return (value.isBlank() ? null : value);
				}, edgeKeyNames, (key, e) -> {
					var path = (Path) e.getData();

					var value = (path == null ? "" : String.valueOf(switch (key) {
						case "path" -> PathIO.toString(path);
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
						case "acceptor" -> graph.isTransferAcceptorEdge(e) ? "1" : "";
						default -> "";
					}));
					return (value.isBlank() ? null : value);
				});
		BackgroundImageIO.save(backgroundImageView, w);
	}

	/**
	 * open a file and load it into the draw window
	 * @param file file
	 * @param drawView draw window
	 * @throws IOException
	 */
	public static void open(String file, DrawView drawView, ImageView backgroundImageView) throws IOException {
		 try(var r=new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(file))) {
			 load(r, drawView, backgroundImageView);
			 drawView.getUndoManager().clear();
		 }
	}

	/**
	 * load a a phylo-sketch, previously saved in GML format
	 * @param r reader
	 * @param view the draw window
	 * @throws IOException something went wrong
	 */
	public static void load(Reader r, DrawView view, ImageView backgroundImageView) throws IOException {
		view.clear();
		var graph = view.getGraph();
		var arrowEdges = new HashSet<Edge>();
		var acceptorEdges = new HashSet<Edge>();

		var gmlInfo=GraphGML.readGML(r, graph, (key, v, value) -> {
			switch (key) {
				case "tax_id" -> {
					graph.addTaxon(v, Integer.parseInt(value));
				}
				case "shape" -> {
					var type = StringUtils.valueOfIgnoreCase(NodeShape.Type.class, value);
					var shape = new NodeShape(type != null ? type : NodeShape.Type.Circle);
					view.setShape(v, shape);
				}
				case "size" -> {
					if (v.getData() instanceof NodeShape shape) {
						var tokens = StringUtils.split(value, ',');
						if (tokens.length >= 1) {
							var width = Double.parseDouble(tokens[0]);
							//var height=(tokens.length>=2?Double.parseDouble(tokens[1]):width);
							shape.setSize(width);
						}
					}
				}
				case "stroke" -> {
					if (v.getData() instanceof Shape shape && ColorUtilsFX.isColor(value)) {
						ColorUtils.setStroke(shape, ColorUtilsFX.parseColor(value), "graph-node");
					}
				}
				case "fill" -> {
					if (v.getData() instanceof Shape shape && ColorUtilsFX.isColor(value)) {
						ColorUtils.setFill(shape, ColorUtilsFX.parseColor(value), "graph-node");
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
				case "label_angle" -> {
					if (NumberUtils.isDouble(value)) {
						view.ensureLabelExists(v);
						DrawView.getLabel(v).setRotate(NumberUtils.parseDouble(value));
						DrawView.getLabel(v).ensureUpright();
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
					var path = new EdgePath(PathIO.fromString(value));
					view.addPath(e, path);
				}
				case "stroke" -> {
					if (e.getData() instanceof Path path && ColorUtilsFX.isColor(value)) {
						ColorUtils.setStroke(path, ColorUtilsFX.parseColor(value), "graph-edge");
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
				case "acceptor" -> {
					if (value.equals("1"))
						acceptorEdges.add(e);
				}
			}
		});

		LSAUtils.setLSAChildrenAndTransfersMap(graph);
		for (var e : acceptorEdges)
			graph.setTransferAcceptor(e, true);


		// create shapes for any nodes for which shape not given
		for(var v:graph.nodes()) {
			if (!(v.getData() instanceof NodeShape)) {
				view.setShape(v, new NodeShape(NodeShape.Type.Circle));
				view.ensureLabelExists(v);
			}
			view.ensureLabelExists(v);
		}

		// create paths for any edges for which path not given
		for(var e:graph.edges()) {
			if (!(e.getData() instanceof EdgePath)) {
				var a=new Point2D(((Shape)e.getSource().getData()).getTranslateX(), ((Shape)e.getSource().getData()).getTranslateY());
				var b=new Point2D(((Shape)e.getTarget().getData()).getTranslateX(), ((Shape)e.getTarget().getData()).getTranslateY());
				var path = new EdgePath();
				path.setStraight(a, b);
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
		if (BackgroundImageIO.load(backgroundImageView, r))
			view.setMode(DrawView.Mode.Capture);
	}
}


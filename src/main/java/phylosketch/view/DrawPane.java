/*
 *  Copyright (C) 2018. Daniel H. Huson
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
 */

package phylosketch.view;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import jloda.fx.control.RichTextLabel;
import jloda.fx.graph.GraphFX;
import jloda.fx.selection.SelectionModel;
import jloda.fx.selection.SetSelectionModel;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.*;
import jloda.fx.window.MainWindowManager;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.graph.algorithms.IsDAG;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import phylosketch.commands.LayoutLabelsCommand;
import phylosketch.main.PhyloSketch;
import phylosketch.paths.PathUtils;
import phylosketch.window.MouseSelection;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * the draw view
 * todo: This needs refactoring to separated MVP
 * Daniel Huson, 9.2024
 */
public class DrawPane extends Pane {
	public enum Mode {View, Move, Edit}

	private final PhyloTree graph;
	private final GraphFX<PhyloTree> graphFX;

	private final SelectionModel<Node> nodeSelection;
	private final SelectionModel<Edge> edgeSelection;

	private final Group edgeIcebergsGroup = new Group();
	private final Group nodeIcebergsGroup = new Group();
	private final Group edgesGroup = new Group();
	private final Group arrowHeadsGroup = new Group();
	private final Group nodesGroup = new Group();
	private final Group edgeLabelsGroup = new Group();
	private final Group nodeLabelsGroup = new Group();
	private final Group outlinesGroup = new Group();
	private final Group otherGroup = new Group();
	private final Group world = new Group();

	private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(this, "mode", Mode.Edit);
	private final BooleanProperty movable = new SimpleBooleanProperty(this, "movable");

	private final ObservableMap<Edge, Shape> edgeArrowMap = FXCollections.observableHashMap();

	private final ObservableMap<Edge, Path> edgeOutlineMap = FXCollections.observableHashMap();
	private final BooleanProperty showOutlines = new SimpleBooleanProperty(this, "showOutlines", false);

	private final BooleanProperty showArrows = new SimpleBooleanProperty(this, "showArrows,true");

	private final DoubleProperty tolerance = new SimpleDoubleProperty(this, "tolerance", 5);

	private final BooleanProperty valid = new SimpleBooleanProperty(this, "isValidNetwork", false);

	private final BooleanProperty showWeight = new SimpleBooleanProperty(this, "showWeight", false);
	private final BooleanProperty showConfidence = new SimpleBooleanProperty(this, "showConfidence", false);
	private final BooleanProperty showProbability = new SimpleBooleanProperty(this, "showProbability", false);

	private final UndoManager undoManager = new UndoManager();

	public DrawPane() {
		mode.addListener((v, o, n) -> {
			movable.set(n == Mode.Edit || n == Mode.Move);
		});

		setPadding(new javafx.geometry.Insets(20));

		prefWidthProperty().addListener((v, o, n) -> System.err.println("width: " + n));

		Icebergs.setEnabled(false);
		var shapeIcebergMap = new HashMap<Shape, Shape>();

		nodesGroup.getChildren().addListener(createIcebergListener(shapeIcebergMap, nodeIcebergsGroup));
		edgesGroup.getChildren().addListener(createIcebergListener(shapeIcebergMap, edgeIcebergsGroup));
		edgesGroup.getChildren().addListener((ListChangeListener<? super javafx.scene.Node>) a -> {
			if (showOutlines.get()) {
				while (a.next()) {
					if (a.wasAdded()) {
						var edges = a.getAddedSubList().stream().map(p -> (Edge) p.getUserData()).filter(Objects::nonNull).toList();
						showOutlines(edges, true);

					}
					if (a.wasRemoved()) {
						var edges = a.getRemoved().stream().map(p -> (Edge) p.getUserData()).filter(Objects::nonNull).toList();
						showOutlines(edges, false);
					}
				}
			}
		});

		outlinesGroup.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, MainWindowManager.isUseDarkTheme() ? Color.WHITE : Color.BLACK, 0.5, 0.5, 0.0, 0.0));
		MainWindowManager.useDarkThemeProperty().addListener((v, o, n) -> {
			outlinesGroup.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, n ? Color.WHITE : Color.BLACK, 0.5, 0.5, 0.0, 0.0));
			for (var shape : BasicFX.getAllRecursively(outlinesGroup, Path.class)) {
				shape.setStroke(n ? Color.WHITE : Color.BLACK);
			}
		});


		graph = new PhyloTree();
		graphFX = new GraphFX<>(graph);
		nodeSelection = new SetSelectionModel<>();
		edgeSelection = new SetSelectionModel<>();

		var object = new Object();

		graphFX.lastUpdateProperty().addListener(e -> {
			RunAfterAWhile.applyInFXThread(object, () -> {
				for (var v : graph.nodes()) {
					if (v.getOwner() != null) {
						var shape = getShape(v);
						if (shape instanceof Circle circle) {
							if (v.getInDegree() == 0 || v.getOutDegree() == 0)
								circle.setRadius(4);
							else
								((Circle) shape).setRadius(2);

						}
					}
				}
			});
		});

		if (false) {
			graphFX.lastUpdateProperty().addListener(e -> {
				valid.set(graph.getNumberOfNodes() > 0 && graph.nodeStream().filter(v -> v.getInDegree() == 0).count() == 1
						  && IsDAG.apply(graph) && graph.nodeStream().filter(Node::isLeaf).allMatch(v -> graph.getLabel(v) != null));

				nodeSelection.getSelectedItems().removeAll(nodeSelection.getSelectedItems().stream().filter(a -> a.getOwner() == null).toList());
				edgeSelection.getSelectedItems().removeAll(edgeSelection.getSelectedItems().stream().filter(a -> a.getOwner() == null).toList());

				for (var v : graph.nodes()) {
					if (v.getData() instanceof Shape shape) {
						shape.getStyleClass().add("graph-node");
						shape.setStrokeWidth(v.getInDegree() == 0 ? 2 : 1);

						if (shape instanceof Circle circle)
							circle.setRadius(v.getInDegree() == 0 || v.getOutDegree() == 0 ? 3 : 1.5);
						MouseSelection.setupNodeSelection(v, shape, nodeSelection, edgeSelection);
					}
				}

				for (var edge : graph.edges()) {
					if (edge.getData() instanceof Path ePath)
						MouseSelection.setupEdgeSelection(edge, ePath, nodeSelection, edgeSelection);
				}
			});
		}

		nodeSelection.getSelectedItems().addListener((SetChangeListener<? super Node>) a -> {
			if (a.wasAdded()) {
				var v = a.getElementAdded();
				if (v.getOwner() != null) {
					if (v.getData() instanceof Shape shape) {
						shape.setEffect(SelectionEffect.create(Color.GOLD));
					}
					nodeLabelsGroup.getChildren().stream().filter(label -> label.getUserData() instanceof Integer id && id == v.getId()).forEach(label -> label.setEffect(SelectionEffect.create(Color.GOLD)));
				}
			} else if (a.wasRemoved()) {
				var v = a.getElementRemoved();
				if (v.getOwner() != null) {
					if (v.getData() instanceof Shape shape) {
						shape.setEffect(null);
					}
					nodeLabelsGroup.getChildren().stream().filter(label -> label.getUserData() instanceof Integer id && id == v.getId()).forEach(label -> label.setEffect(null));
				}
			}
		});

		edgeSelection.getSelectedItems().addListener((SetChangeListener<? super Edge>) e -> {
			if (e.wasAdded()) {
				var edge = e.getElementAdded();
				if (edge.getOwner() != null && edge.getData() instanceof Shape shape) {
					shape.setEffect(SelectionEffect.create(Color.GOLD));
				}
				edgeLabelsGroup.getChildren().stream().filter(label -> label.getUserData() instanceof Integer id && id == edge.getId()).forEach(label -> label.setEffect(SelectionEffect.create(Color.GOLD)));
			} else if (e.wasRemoved()) {
				var edge = e.getElementRemoved();
				if (edge.getOwner() != null && edge.getData() instanceof Shape shape) {
					shape.setEffect(null);
				}
				edgeLabelsGroup.getChildren().stream().filter(label -> label.getUserData() instanceof Integer id && id == edge.getId()).forEach(label -> label.setEffect(null));
			}
		});

		world.getChildren().addAll(edgeIcebergsGroup, nodeIcebergsGroup, edgesGroup, arrowHeadsGroup, nodesGroup, edgeLabelsGroup, nodeLabelsGroup, outlinesGroup, otherGroup);
		getChildren().add(world);

		getStyleClass().add("viewer-background");
		//setStyle("-fx-background-color: lightblue;");

		edgeArrowMap.addListener((MapChangeListener<Edge, Shape>) a -> {
			if (a.wasAdded()) {
				arrowHeadsGroup.getChildren().add(a.getValueAdded());
			} else if (a.wasRemoved()) {
				arrowHeadsGroup.getChildren().remove(a.getValueRemoved());
			}
		});

		edgeOutlineMap.addListener((MapChangeListener<Edge, Shape>) a -> {
			if (a.wasAdded()) {
				outlinesGroup.getChildren().add(a.getValueAdded());
			} else if (a.wasRemoved()) {
				outlinesGroup.getChildren().remove(a.getValueRemoved());
			}
		});

		showOutlines.addListener((v, o, n) -> showOutlines(graph.getEdgesAsList(), n));
	}

	public void clear() {
		graph.clear();
		for (var child : world.getChildren()) {
			if (child instanceof Group group)
				group.getChildren().clear();
		}
		undoManager.clear();
	}

	public String toBracketString() {
		var newickIO = new NewickIO();
		var outputFormat = new NewickIO.OutputFormat(graph.hasEdgeWeights(), false, graph.hasEdgeConfidences(), graph.hasEdgeProbabilities(), false);

		var w = new StringWriter();
		for (var tree : extractAllTrees(graph)) {
			if (nodeSelection.size() == 0 || tree.nodeStream().map(v -> (Shape) v.getData()).map(s -> (Node) s.getUserData()).anyMatch(nodeSelection::isSelected)) {
				var root = tree.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
				if (root.isPresent()) {
					tree.setRoot(root.get());
					tree.edgeStream().forEach(f -> {
						tree.setReticulate(f, f.getTarget().getInDegree() > 1);
					});
					try {
						newickIO.write(tree, w, outputFormat);
						w.write(";\n");
					} catch (IOException ignored) {
					}
				}
			}
		}
		return w.toString();
	}

	public List<PhyloTree> extractAllTrees(PhyloTree graph) {
		var list = new ArrayList<PhyloTree>();
		try (var componentMap = graph.newNodeIntArray();
			 NodeArray<Node> srcTarMap = graph.newNodeArray()) {
			graph.computeConnectedComponents(componentMap);
			for (var component : new TreeSet<>(componentMap.values())) {
				var tree = new PhyloTree();
				tree.copy(graph, srcTarMap, null);
				graph.nodeStream().filter(v -> !Objects.equals(componentMap.get(v), component)).map(srcTarMap::get).forEach(tree::deleteNode);
				list.add(tree);
				srcTarMap.clear();
			}
		}
		return list;
	}

	public double getTolerance() {
		return tolerance.get();
	}

	public DoubleProperty toleranceProperty() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance.set(tolerance);
	}

	public PhyloTree getGraph() {
		return graph;
	}

	public GraphFX<PhyloTree> getGraphFX() {
		return graphFX;
	}

	public Group getEdgesGroup() {
		return edgesGroup;
	}

	public Group getNodesGroup() {
		return nodesGroup;
	}

	public Group getNodeLabelsGroup() {
		return nodeLabelsGroup;
	}

	public Group getEdgeLabelsGroup() {
		return edgeLabelsGroup;
	}

	public Group getOtherGroup() {
		return otherGroup;
	}

	public boolean getValid() {
		return valid.get();
	}

	public BooleanProperty validProperty() {
		return valid;
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public SelectionModel<Node> getNodeSelection() {
		return nodeSelection;
	}

	public SelectionModel<Edge> getEdgeSelection() {
		return edgeSelection;
	}

	public Node createNode() {
		var v = graph.newNode();
		setShape(v, new Circle(3));
		return v;
	}

	public Node createNode(Point2D location) {
		var v = createNode();
		setLocation(v, location);
		return v;
	}

	public Node createNode(Point2D location, int recycleNodeId) {
		var v = createNode(new Circle(3), null, recycleNodeId);
		setLocation(v, location);
		return v;
	}

	public void setShape(Node v, Shape shape) {
		shape.getStyleClass().add("graph-node");
		v.setData(shape);
		shape.setUserData(v);
		if (!nodesGroup.getChildren().contains(shape))
			nodesGroup.getChildren().add(shape);
	}

	public Node createNode(Shape shape, RichTextLabel label, int recycledId) {
		var v = graph.newNode(null, recycledId);
		setShape(v, shape);
		if (label != null && !nodeLabelsGroup.getChildren().contains(label)) {
			v.setInfo(label);
			nodeLabelsGroup.getChildren().add(label);
		}
		return v;
	}

	public void setLocation(Node v, Point2D location) {
		var shape = getShape(v);
		shape.setTranslateX(location.getX());
		shape.setTranslateY(location.getY());
	}

	public void deleteNode(Node... nodes) {
		for (var v : nodes) {
			for (var e : IteratorUtils.asList(v.adjacentEdges())) {
				deleteEdge(e);
			}
			nodeSelection.getSelectedItems().remove(v);
			var shape = (Shape) v.getData();
			nodesGroup.getChildren().remove(shape);
			if (v.getInfo() instanceof RichTextLabel label)
				nodeLabelsGroup.getChildren().remove(label);
			graph.deleteNode(v);
		}
	}


	public Edge createEdge(Node v, Node w, Path path) {
		var e = graph.newEdge(v, w);
		addPath(e, path);
		return e;
	}

	public void addPath(Edge e, Path path) {
		e.setData(path);
		path.setUserData(e);
		if (!edgesGroup.getChildren().contains(path))
			edgesGroup.getChildren().add(path);
	}

	public Edge createEdge(Node v, Node w, Path path, int recycledId) {
		var e = graph.newEdge(v, w, null, recycledId);
		addPath(e, path);
		return e;
	}

	public void deleteEdge(Edge... edges) {
		for (var e : edges) {
			if (e.getOwner() != null) {
				if (e.getInfo() instanceof RichTextLabel label)
					edgeLabelsGroup.getChildren().remove(label);
				edgeArrowMap.remove(e);
				edgeSelection.getSelectedItems().remove(e);
				if (e.getData() instanceof Path path)
					edgesGroup.getChildren().remove(path);
				graph.deleteEdge(e);
			}
		}
	}

	public RichTextLabel createLabel(Node v, String text) {
		var shape = (Shape) v.getData();
		graph.setLabel(v, RichTextLabel.getRawText(text));
		var label = new RichTextLabel(text);
		v.setInfo(label);
		label.setUserData(v.getId());
		label.translateXProperty().bind(shape.translateXProperty());
		label.translateYProperty().bind(shape.translateYProperty());
		nodeLabelsGroup.getChildren().add(label);
		label.applyCss();

		label.setOnMouseClicked(e -> {
			if (!e.isShiftDown() && PhyloSketch.isDesktop()) {
				getNodeSelection().clearSelection();
				getEdgeSelection().clearSelection();
			}
			getNodeSelection().toggleSelection(v);
		});
		var labelLayout = LayoutLabelsCommand.computeLabelLayout(RootLocation.compute(ConnectedComponents.component(v)), label);
		label.setLayoutX(labelLayout.getX());
		label.setLayoutY(labelLayout.getY());
		LabelUtils.makeDraggable(label, movable, this);
		return label;
	}

	public RichTextLabel createLabel(Edge e, String text) {
		var path = (Path) e.getData();
		graph.setLabel(e, RichTextLabel.getRawText(text));
		var label = new RichTextLabel(text);
		e.setInfo(label);
		label.setUserData(e.getId());
		InvalidationListener listener = a -> {
			var middle = PathUtils.getMiddle(path);
			label.setTranslateX(middle.getX());
			label.setTranslateY(middle.getY());
		};
		path.getElements().addListener(listener);
		listener.invalidated(null);
		edgeLabelsGroup.getChildren().add(label);
		label.applyCss();

		label.setOnMouseClicked(a -> {
			if (!a.isShiftDown() && PhyloSketch.isDesktop()) {
				getNodeSelection().clearSelection();
				getEdgeSelection().clearSelection();
			}
			getEdgeSelection().toggleSelection(e);
		});
		LabelUtils.makeDraggable(label, movable, this);
		return label;
	}

	private ListChangeListener<javafx.scene.Node> createIcebergListener(Map<Shape, Shape> shapeIcebergMap, Group icebergsGroup) {
		return a -> {
			while (a.next()) {
				for (var item : a.getAddedSubList()) {
					if (item instanceof Shape shape) {
						var iceberg = Icebergs.create(shape, true);
						icebergsGroup.getChildren().add(iceberg);
						shapeIcebergMap.put(shape, iceberg);
					}
				}
				for (var item : a.getRemoved()) {
					if (item instanceof Shape shape) {
						var iceberg = shapeIcebergMap.get(shape);
						if (iceberg != null) {
							icebergsGroup.getChildren().remove(iceberg);
						}
					}
				}
			}
		};
	}

	public Collection<Node> getSelectedOrAllNodes() {
		if (nodeSelection.size() > 0)
			return nodeSelection.getSelectedItems();
		else
			return graph.getNodesAsList();
	}

	public Collection<Edge> getSelectedOrAllEdges() {
		if (edgeSelection.size() > 0)
			return edgeSelection.getSelectedItems();
		else
			return graph.getEdgesAsList();
	}

	public RichTextLabel getLabel(Node v) {
		if (v.getInfo() instanceof RichTextLabel richTextLabel)
			return richTextLabel;
		else
			return createLabel(v, "");
	}

	public static double getX(Node v) {
		if (v.getData() instanceof Shape shape) {
			return shape.getTranslateX();
		} else return 0;
	}

	public static double getY(Node v) {
		if (v.getData() instanceof Shape shape) {
			return shape.getTranslateY();
		} else return 0;
	}

	public RichTextLabel getLabel(Edge e) {
		if (e.getInfo() instanceof RichTextLabel richTextLabel)
			return richTextLabel;
		else
			return createLabel(e, "");
	}

	public Point2D getPoint(Node v) {
		if (v.getData() instanceof Shape shape) {
			return new Point2D(shape.getTranslateX(), shape.getTranslateY());
		} else return null;
	}

	public Shape getShape(Node v) {
		return (Shape) v.getData();
	}

	public Path getPath(Edge e) {
		return (Path) e.getData();
	}

	public List<Point2D> getPoints(Edge e) {
		return PathUtils.getPoints(getPath(e));
	}

	public void setLabel(Node v, String text) {
		if (v != null) {
			getLabel(v).setText(text);
			graph.setLabel(v, RichTextLabel.getRawText(text));
		}
	}

	public void setLabel(Edge e, String text) {
		if (e != null) {
			getLabel(e).setText(text);
			graph.setLabel(e, RichTextLabel.getRawText(text));
		}
	}

	public Mode getMode() {
		return mode.get();
	}

	public ObjectProperty<Mode> modeProperty() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode.set(mode);
	}

	public boolean isShowWeight() {
		return showWeight.get();
	}

	public BooleanProperty showWeightProperty() {
		return showWeight;
	}

	public boolean isShowConfidence() {
		return showConfidence.get();
	}

	public BooleanProperty showConfidenceProperty() {
		return showConfidence;
	}

	public boolean isShowProbability() {
		return showProbability.get();
	}

	public BooleanProperty showProbabilityProperty() {
		return showProbability;
	}

	public ObservableMap<Edge, Shape> getEdgeArrowMap() {
		return edgeArrowMap;
	}

	private void showOutlines(Collection<Edge> edges, boolean show) {
		if (!show) {
			edgeOutlineMap.clear();
		} else {
			for (var e : edges) {
				if (!edgeOutlineMap.containsKey(e)) {
					if (e.getData() instanceof Path path) {
						var outline = PathUtils.createPath(PathUtils.extractPoints(path), false);
						outline.getStyleClass().remove("graph-edge");
						if (e.getSource().getInDegree() == 0 || e.getTarget().getOutDegree() == 0)
							outline.setStrokeLineCap(StrokeLineCap.SQUARE);
						else
							outline.setStrokeLineCap(StrokeLineCap.ROUND);

						outline.setStrokeWidth(30);

						outline.setStroke(MainWindowManager.isUseDarkTheme() ? Color.BLACK : Color.WHITE);
						outline.setFill(Color.TRANSPARENT);
						edgeOutlineMap.put(e, outline);
						InvalidationListener listener = a -> {
							outline.getElements().setAll(PathUtils.copy(path.getElements()));
						};
						outline.setUserData(listener); // keep a reference
						path.getElements().addListener(new WeakInvalidationListener(listener));
						((Shape) e.getSource().getData()).translateXProperty().addListener(new WeakInvalidationListener(listener));
						((Shape) e.getTarget().getData()).translateXProperty().addListener(new WeakInvalidationListener(listener));
					}
				}
			}
		}
	}

	public boolean isShowOutlines() {
		return showOutlines.get();
	}

	public BooleanProperty showOutlinesProperty() {
		return showOutlines;
	}

	public boolean isShowArrows() {
		return showArrows.get();
	}

	public BooleanProperty showArrowsProperty() {
		return showArrows;
	}

	public void setShowArrow(Edge e, boolean show) {
		if (!show) {
			edgeArrowMap.remove(e);
		} else {
			if (!edgeArrowMap.containsKey(e)) {
				if (e.getData() instanceof Path path) {
					var arrowHead = new Polygon(7.0, 0.0, -7.0, 4.0, -7.0, -4.0);
					arrowHead.getStyleClass().add("graph-node");
					edgeArrowMap.put(e, arrowHead);

					InvalidationListener listener = a -> {
						var points = PathUtils.extractPoints(path);
						if (points.size() >= 2) {
							var lastId = points.size() - 1;
							var last = points.get(lastId);
							var firstId = lastId;
							while (firstId > 0 && last.distance(points.get(firstId)) < 8) {
								firstId--;
							}
							var direction = last.subtract(points.get(firstId));
							direction = direction.multiply(1.0 / direction.magnitude());
							arrowHead.setRotate(GeometryUtilsFX.computeAngle(direction));
							arrowHead.setTranslateX(last.getX() - 12 * direction.getX());
							arrowHead.setTranslateY(last.getY() - 12 * direction.getY());
						}
					};
					listener.invalidated(null);
					arrowHead.setUserData(listener);
					path.getElements().addListener(new WeakInvalidationListener(listener));
					getShape(e.getTarget()).translateXProperty().addListener(new WeakInvalidationListener(listener));
					getShape(e.getTarget()).translateYProperty().addListener(new WeakInvalidationListener(listener));
				}
			}
		}
	}
}
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

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import jloda.fx.control.RichTextLabel;
import jloda.fx.graph.GraphFX;
import jloda.fx.selection.SelectionModel;
import jloda.fx.selection.SetSelectionModel;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.Icebergs;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.SelectionEffect;
import jloda.fx.window.MainWindowManager;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.graph.algorithms.IsDAG;
import jloda.phylo.PhyloTree;
import jloda.util.IteratorUtils;
import phylosketch.main.PhyloSketch;
import phylosketch.window.MouseSelection;

import java.util.*;

/**
 * the draw pane
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
	private final Group nodesGroup = new Group();
	private final Group nodeLabelsGroup = new Group();
	private final Group otherGroup = new Group();
	private final Group world = new Group();

	private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(this, "mode", Mode.View);
	private final BooleanProperty movable = new SimpleBooleanProperty(this, "movable");

	private final BooleanProperty arrowHeads=new SimpleBooleanProperty(this,"arrowHeads",false);

	private final BooleanProperty outlineEdges =new SimpleBooleanProperty(this,"fatEdges",false);

	private final DoubleProperty tolerance = new SimpleDoubleProperty(this, "tolerance", 5);

	private final BooleanProperty valid = new SimpleBooleanProperty(this, "isValidNetwork", false);

	private final UndoManager undoManager = new UndoManager();

	public DrawPane() {
		mode.addListener((v, o, n) -> {
			movable.set(n == Mode.Edit || n == Mode.Move);
		});
		ProgramProperties.track(mode, Mode::valueOf, Mode.Edit);

		setPadding(new javafx.geometry.Insets(20));

		prefWidthProperty().addListener((v, o, n) -> System.err.println("width: " + n));

		Icebergs.setEnabled(false);
		var shapeIcebergMap = new HashMap<Shape, Shape>();

		nodesGroup.getChildren().addListener(createIcebergListener(shapeIcebergMap, nodeIcebergsGroup));
		edgesGroup.getChildren().addListener(createIcebergListener(shapeIcebergMap, edgeIcebergsGroup));

		graph = new PhyloTree();
		graphFX = new GraphFX<>(graph);
		nodeSelection = new SetSelectionModel<>();
		edgeSelection = new SetSelectionModel<>();

		var nodeIndegree = new HashMap<Node, Integer>();
		var nodeOutdegree = new HashMap<Node, Integer>();
		graphFX.lastUpdateProperty().addListener(e -> {
			for (var v : graph.nodes()) {
				if (v.getInDegree() != nodeIndegree.getOrDefault(v, -1) || v.getOutDegree() != nodeOutdegree.getOrDefault(v, -1)) {
					if (v.getData() instanceof Circle circle) {
						circle.setRadius(v.getInDegree() == 0 || v.getOutDegree() == 0 ? 4 : 2);
					}
					nodeIndegree.put(v, v.getInDegree());
					nodeOutdegree.put(v, v.getOutDegree());
				}
			}
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
				if (edge.getOwner() != null && edge.getData() instanceof Shape shape)
					shape.setEffect(SelectionEffect.create(Color.GOLD));
			} else if (e.wasRemoved()) {
				var edge = e.getElementRemoved();
				if (edge.getOwner() != null && edge.getData() instanceof Shape shape)
					shape.setEffect(null);
			}
		});

		world.getChildren().addAll(edgeIcebergsGroup, nodeIcebergsGroup, edgesGroup, nodesGroup, nodeLabelsGroup, otherGroup);
		getChildren().add(world);

		EdgeCreationInteraction.setup(this);
		PaneInteraction.setup(this);
		NodeInteraction.setup(this);
		EdgeInteraction.setup(this);

		getStyleClass().add("viewer-background");
		//setStyle("-fx-background-color: lightblue;");

		if (true)

		outlineEdges.addListener((v, o, n)->{
			if(!n){
				nodesGroup.setVisible(true);
				for (var item : edgesGroup.getChildren()) {
					if (item instanceof Path path) {
						path.setStroke(null);
						path.setStrokeWidth(1);
						path.setStrokeLineCap(StrokeLineCap.ROUND);
						path.getStyleClass().add("graph-edge");
					}
				}
				edgesGroup.setEffect(null);
			}
			else {
				nodesGroup.setVisible(false);
				for (var item : edgesGroup.getChildren()) {
					if (item instanceof Path path) {
						if (item.getUserData() instanceof Edge e) {
							if (e.getSource().getDegree() == 1 || e.getTarget().getDegree() == 1)
								path.setStrokeLineCap(StrokeLineCap.SQUARE);
							else
								path.setStrokeLineCap(StrokeLineCap.ROUND);
						}
						path.setStrokeWidth(30);
						path.setStroke(MainWindowManager.isUseDarkTheme()?Color.BLACK:Color.WHITE);
					}
				}
				edgesGroup.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, MainWindowManager.isUseDarkTheme()?Color.WHITE:Color.BLACK, 0.5, 0.5, 0.0, 0.0));
			}
		});
	}


	public void clear() {
		graph.clear();
		for (var child : world.getChildren()) {
			if (child instanceof Group group)
				group.getChildren().clear();
		}
		undoManager.clear();
	}

	public String toBracketString(boolean showWeights) {
		var buf = new StringBuilder();
		for (var tree : extractAllTrees(graph)) {
			if (nodeSelection.size() == 0 || tree.nodeStream().map(v -> (Shape) v.getData()).map(s -> (Node) s.getUserData()).anyMatch(nodeSelection::isSelected)) {
				var root = tree.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
				if (root.isPresent()) {
					tree.setRoot(root.get());
					tree.edgeStream().forEach(f -> {
						var weight = 0.0;
						var reticulate = (f.getTarget().getInDegree() > 1);
						if (!reticulate) {
							if (showWeights) {
								var aPt = new Point2D(DrawPane.getX(f.getSource()), DrawPane.getY(f.getSource()));
								var bPt = new Point2D(DrawPane.getX(f.getTarget()), DrawPane.getY(f.getTarget()));
								weight = ((int) aPt.distance(bPt)) / 100.0;
							} else weight = 1.0;
						}
						tree.setWeight(f, weight);
						tree.setReticulate(f, reticulate);
					});
					buf.append(tree.toBracketString(showWeights)).append(";\n");
				}
			}
		}
		return buf.toString();
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

	public Node createNode(Shape shape) {
		var v = graph.newNode();
		addShape(v, shape);
		return v;
	}

	public void addShape(Node v, Shape shape) {
		shape.getStyleClass().add("graph-node");
		v.setData(shape);
		shape.setUserData(v);
		if (!nodesGroup.getChildren().contains(shape))
			nodesGroup.getChildren().add(shape);
	}

	public Node createNode(Shape shape, RichTextLabel label, int recycledId) {
		var v = graph.newNode(null, recycledId);
		addShape(v, shape);
		if (label != null && !nodeLabelsGroup.getChildren().contains(label)) {
			v.setInfo(label);
			nodeLabelsGroup.getChildren().add(label);
		}
		return v;
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
				edgeSelection.getSelectedItems().remove(e);
				if (e.getData() instanceof Path p)
					edgesGroup.getChildren().remove(p);
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
		switch (RootLocation.compute(ConnectedComponents.component(v))) {
			case Top -> {
				label.setLayoutX(-10);
				label.setLayoutY(5);
			}
			case Bottom -> {
				label.setLayoutX(-10);
				label.setLayoutY(+5);
			}
			case Right -> {
				label.setLayoutX(label.getWidth()+10);
				label.setLayoutY(-5);
			}
			default /*case Left */ -> {
				label.setLayoutX(10);
				label.setLayoutY(-5);
			}
		}
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

	public Point2D getPoint(Node v) {
		if (v.getData() instanceof Shape shape) {
			return new Point2D(shape.getTranslateX(), shape.getTranslateY());
		} else return null;
	}

	public void setLabel(int nodeId, String text) {
		var v = graph.findNodeById(nodeId);
		if (v != null) {
			getLabel(v).setText(text);
			graph.setLabel(v, RichTextLabel.getRawText(text));
		}
	}

	public boolean getOutlineEdges() {
		return outlineEdges.get();
	}

	public BooleanProperty outlineEdgesProperty() {
		return outlineEdges;
	}

	public void setOutlineEdges(boolean outlineEdges) {
		this.outlineEdges.set(outlineEdges);
	}

	public boolean isArrowHeads() {
		return arrowHeads.get();
	}

	public BooleanProperty arrowHeadsProperty() {
		return arrowHeads;
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
}
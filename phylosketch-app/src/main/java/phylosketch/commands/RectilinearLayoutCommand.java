/*
 * RectilinearLayoutCommand.java Copyright (C) 2026 Daniel H. Huson
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

package phylosketch.commands;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Point2D;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.AService;
import jloda.fx.util.GeometryUtilsFX;
import jloda.fx.window.NotificationManager;
import jloda.graph.Node;
import phylosketch.paths.EdgePath;
import phylosketch.utils.RectilinearLayout;
import phylosketch.view.DrawView;
import phylosketch.view.NodeGrid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * redraws the phylogeny on the grid so that edges run horizontally or vertically where possible and diagonally
 * otherwise, with few crossings and room for labels, see {@link RectilinearLayout}, and switches the grid on, so that
 * the drawing can then be adjusted by hand in grid steps
 * <p>
 * The search places the nodes for straight edges, so all edges are drawn straight. As an edge may now leave a node in
 * any of eight directions, the labels are placed horizontally, away from the edges of their nodes, as SplitsTree does
 * in a network, rather than on the side that the root position of a tree implies. The drawing stays where it was: the
 * result is shifted by whole grid steps so that its top-left corner is that of the snapped drawing. The search runs in
 * the background, see {@link #apply}; its result is applied as one edit that can be undone, unless the drawing changed
 * in the meantime.
 * <p>
 * Daniel Huson, 9.2026
 */
public class RectilinearLayoutCommand extends UndoableRedoableCommand {
	/**
	 * time allowed for the search; phylogenies of up to about a hundred nodes need less and then always come out the
	 * same, see {@link RectilinearLayout}
	 */
	public static final long BUDGET_MILLIS = 2000;
	/**
	 * distance between a node and its label, enough to clear a node of the default size
	 */
	private static final double LABEL_GAP = 8;

	private final Runnable undo;
	private final Runnable redo;

	/**
	 * runs the search in the background and applies its result as one edit, unless the drawing changed in the
	 * meantime
	 *
	 * @param view      the view
	 * @param grid      the grid, whose spacing is kept if it is on, and otherwise chosen for the drawing
	 * @param isRunning set while the search runs
	 */
	public static void apply(DrawView view, NodeGrid grid, BooleanProperty isRunning) {
		var graph = view.getGraph();
		if (graph.getNumberOfNodes() == 0 || isRunning.get())
			return;
		var oldPoints = new LinkedHashMap<Node, Point2D>(); // in node order, which breaks ties
		for (var v : graph.nodes())
			oldPoints.put(v, view.getLocation(v));
		var edges = graph.getEdgesAsList();

		var spacing = (grid.isSnap() && grid.getSpacing() > 0 ? grid.getSpacing() : NodeGrid.computeSpacing(graph, oldPoints::get));
		var snapped = NodeGrid.snapAll(oldPoints, spacing);
		var start = new HashMap<Node, RectilinearLayout.GridPoint>();
		for (var v : oldPoints.keySet()) {
			var p = snapped.get(v);
			start.put(v, new RectilinearLayout.GridPoint((int) Math.round(p.getX() / spacing), (int) Math.round(p.getY() / spacing)));
		}
		// the spacing is half the typical length of an edge, so each edge should be two grid steps long
		var search = RectilinearLayout.prepare(graph, start, e -> 2.0); // reads the graph here, on the FX thread

		isRunning.set(true);
		AService.run(() -> search.run(BUDGET_MILLIS), result -> {
			isRunning.set(false);
			if (graph.getNumberOfNodes() != oldPoints.size() || graph.getNumberOfEdges() != edges.size() || edges.stream().anyMatch(e -> e.getOwner() == null))
				return; // edited while the search ran
			for (var v : oldPoints.keySet()) {
				if (v.getOwner() == null || !view.getLocation(v).equals(oldPoints.get(v)))
					return; // edited while the search ran
			}
			// the search may drift the drawing; it is put back by whole grid steps, so that its top-left corner is where
			// that of the snapped drawing was, as LayoutPhylogenyCommand keeps a layout where the drawing was
			var dx = start.values().stream().mapToInt(RectilinearLayout.GridPoint::x).min().orElse(0) - result.values().stream().mapToInt(RectilinearLayout.GridPoint::x).min().orElse(0);
			var dy = start.values().stream().mapToInt(RectilinearLayout.GridPoint::y).min().orElse(0) - result.values().stream().mapToInt(RectilinearLayout.GridPoint::y).min().orElse(0);
			var newPoints = new HashMap<Node, Point2D>();
			for (var v : oldPoints.keySet())
				newPoints.put(v, new Point2D(spacing * (result.get(v).x() + dx), spacing * (result.get(v).y() + dy)));
			view.getUndoManager().doAndAdd(new RectilinearLayoutCommand(view, grid, spacing, newPoints));
		}, ex -> {
			isRunning.set(false);
			NotificationManager.showError("Rectilinear layout failed: " + ex.getMessage());
		});
	}

	/**
	 * the edit that applies a result of the search
	 *
	 * @param view      the view
	 * @param grid      the grid
	 * @param spacing   the grid spacing used by the search
	 * @param newPoints the new position of each node
	 */
	private RectilinearLayoutCommand(DrawView view, NodeGrid grid, double spacing, Map<Node, Point2D> newPoints) {
		super("rectilinear layout");

		var oldNodeMap = new HashMap<Integer, Point2D>();
		var newNodeMap = new HashMap<Integer, Point2D>();
		for (var v : newPoints.keySet()) {
			oldNodeMap.put(v.getId(), view.getLocation(v));
			newNodeMap.put(v.getId(), newPoints.get(v));
		}

		var oldEdgeMap = new HashMap<Integer, EdgePath>();
		var newEdgeMap = new HashMap<Integer, EdgePath>();
		for (var e : view.getGraph().edges()) {
			var path = DrawView.getPath(e);
			if (path != null && !path.getElements().isEmpty()) {
				// from source to target, as DrawNetwork draws edges, and as moving a node and drawing an arrow expect
				var newPath = path.copy();
				newPath.setStraight(newPoints.get(e.getSource()), newPoints.get(e.getTarget()));
				oldEdgeMap.put(e.getId(), path.copy());
				newEdgeMap.put(e.getId(), newPath);
			}
		}

		// the labels are placed away from the edges, horizontally
		var oldLabelMap = new HashMap<Integer, LabelPlacement>();
		var newLabelMap = new HashMap<Integer, LabelPlacement>();
		for (var v : newPoints.keySet()) {
			var label = DrawView.getLabel(v);
			if (label != null && !label.getRawText().isBlank()) {
				oldLabelMap.put(v.getId(), new LabelPlacement(new Point2D(label.getLayoutX(), label.getLayoutY()), label.getRotate()));
				newLabelMap.put(v.getId(), new LabelPlacement(computeLabelLayout(v, label, newPoints), 0.0));
			}
		}

		var oldSnap = grid.isSnap();
		var oldSpacing = grid.getSpacing();
		var oldHorizontalLabels = view.isHorizontalLabels();

		undo = () -> {
			grid.setSpacing(oldSpacing);
			grid.setSnap(oldSnap);
			view.setHorizontalLabels(oldHorizontalLabels);
			setPositions(view, oldNodeMap, oldEdgeMap, oldLabelMap);
		};
		redo = () -> {
			grid.setSpacing(spacing);
			grid.setSnap(true);
			view.setHorizontalLabels(true);
			setPositions(view, newNodeMap, newEdgeMap, newLabelMap);
		};
	}

	/**
	 * the layout of a label relative to its node, and its rotation
	 */
	private record LabelPlacement(Point2D layout, double rotate) {
	}

	private static void setPositions(DrawView view, Map<Integer, Point2D> nodeMap, Map<Integer, EdgePath> edgeMap, Map<Integer, LabelPlacement> labelMap) {
		for (var entry : nodeMap.entrySet()) {
			view.setLocation(view.getGraph().findNodeById(entry.getKey()), entry.getValue());
		}
		for (var entry : edgeMap.entrySet()) {
			var path = DrawView.getPath(view.getGraph().findEdgeById(entry.getKey()));
			path.set(entry.getValue().getElements(), entry.getValue().getType());
		}
		for (var entry : labelMap.entrySet()) {
			var label = DrawView.getLabel(view.getGraph().findNodeById(entry.getKey()));
			label.setLayoutX(entry.getValue().layout().getX());
			label.setLayoutY(entry.getValue().layout().getY());
			label.setRotate(entry.getValue().rotate());
			label.ensureUpright();
		}
	}

	/**
	 * the layout of a horizontal label relative to its node: a small gap away from the node in the direction of
	 * {@link #computeLabelAngle}, centered on that direction when above or below the node, and starting or ending
	 * there when to the right or left, as the first choice of splitstree6.layout.tree.RadialLabelLayout
	 */
	private static Point2D computeLabelLayout(Node v, RichTextLabel label, Map<Node, Point2D> points) {
		label.applyCss();
		var angle = GeometryUtilsFX.modulo360(computeLabelAngle(v, points));
		var dx = LABEL_GAP * Math.cos(GeometryUtilsFX.deg2rad(angle));
		var dy = LABEL_GAP * Math.sin(GeometryUtilsFX.deg2rad(angle));
		if (angle >= 45 && angle <= 135) // below
			return new Point2D(dx - 0.5 * label.getWidth(), dy);
		else if (angle > 135 && angle <= 225) // left
			return new Point2D(dx - label.getWidth(), dy - 0.5 * label.getHeight());
		else if (angle > 225 && angle <= 315) // above
			return new Point2D(dx - 0.5 * label.getWidth(), dy - label.getHeight());
		else // right
			return new Point2D(dx, dy - 0.5 * label.getHeight());
	}

	/**
	 * the direction in which a label points away from the edges of its node, in degrees, as in
	 * splitstree6.layout.network.NetworkLayout: a node with one edge continues it, and a node with more takes the
	 * middle of the widest gap between them
	 */
	private static double computeLabelAngle(Node v, Map<Node, Point2D> points) {
		var point = points.get(v);
		var angles = v.adjacentEdgesStream(false).map(e -> e.getOpposite(v)).filter(w -> !points.get(w).equals(point))
				.mapToDouble(w -> GeometryUtilsFX.modulo360(GeometryUtilsFX.computeAngle(points.get(w).subtract(point)))).sorted().toArray();
		if (angles.length == 0)
			return 0;
		else if (angles.length == 1)
			return GeometryUtilsFX.modulo360(angles[0] + 180);
		var bestI = angles.length - 1;
		var bestD = (360 - angles[angles.length - 1]) + angles[0];
		for (var i = 0; i < angles.length - 1; i++) {
			var d = angles[i + 1] - angles[i];
			if (d > bestD) {
				bestI = i;
				bestD = d;
			}
		}
		return GeometryUtilsFX.modulo360(angles[bestI] + 0.5 * bestD);
	}

	@Override
	public boolean isUndoable() {
		return undo != null;
	}

	@Override
	public boolean isRedoable() {
		return redo != null;
	}

	@Override
	public void undo() {
		undo.run();
	}

	@Override
	public void redo() {
		redo.run();
	}
}

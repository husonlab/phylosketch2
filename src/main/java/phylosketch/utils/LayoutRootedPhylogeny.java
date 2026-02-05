package phylosketch.utils;

import javafx.geometry.Point2D;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.phylogeny.layout.Averaging;
import jloda.phylogeny.layout.EdgeType;
import jloda.util.IteratorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * runs the layout code
 * Daniel Huson, 1.2025
 */
public class LayoutRootedPhylogeny {
	/**
	 * compute coordinates for nodes
	 *
	 * @param phylogeny               the rooted phylogenetic tree or network
	 * @param layout                  layout type
	 * @param scaling                 scaling type
	 * @param averaging               averaging type
	 * @param optimizeReticulateEdges optimize reticulate displacement?
	 * @param random                  random source
	 * @param nodeAngleMap            returns node to angles map
	 * @param nodePointMap            returns nodes to point map
	 */
	public static void apply(PhyloTree phylogeny, jloda.phylogeny.layout.LayoutRootedPhylogeny.Layout layout,
							 jloda.phylogeny.layout.LayoutRootedPhylogeny.Scaling scaling, Averaging averaging,
							 boolean optimizeReticulateEdges, Random random,
							 Map<Node, Double> nodeAngleMap, Map<Node, Point2D> nodePointMap) {
		Function<Node, List<Edge>> inEdges = v -> IteratorUtils.asList(v.inEdges());
		Function<Node, List<Edge>> outEdges = v -> IteratorUtils.asList(v.outEdges());
		Function<Edge, Node> source = Edge::getSource;
		Function<Edge, Node> target = Edge::getTarget;
		ToDoubleFunction<Edge> weight = phylogeny::getWeight;
		Function<Edge, EdgeType> edgeType = e -> {
			if (phylogeny.isTransferAcceptorEdge(e))
				return EdgeType.transferAcceptor;
			else if (phylogeny.isTransferEdge(e))
				return EdgeType.transfer;
			else if (phylogeny.isReticulateEdge(e))
				return EdgeType.combining;
			else return EdgeType.tree;
		};

		if (phylogeny.hasReticulateEdges() && phylogeny.getLSAChildrenMap().size() < phylogeny.getNumberOfNodes())
			LSAUtils.setLSAChildrenAndTransfersMap(phylogeny);

		var nodePointAdaptor = new HashMap<Node, jloda.phylogeny.layout.Point2D>();

		System.err.println(phylogeny.toBracketString(false) + ";");

		jloda.phylogeny.layout.LayoutRootedPhylogeny.apply(phylogeny.getRoot(), IteratorUtils.asList(phylogeny.nodes()), IteratorUtils.asList(phylogeny.edges()),
				inEdges, outEdges, source, target, weight, edgeType,
				layout, scaling, averaging, optimizeReticulateEdges, random, nodeAngleMap, nodePointAdaptor, phylogeny.getLSAChildrenMap());

		for (var v : nodePointAdaptor.keySet()) {
			var p = nodePointAdaptor.get(v);
			nodePointMap.put(v, new Point2D(p.x(), p.y()));
			System.err.println(v.getId() + ": " + nodePointMap.get(v));
		}
	}

	public static void scaleToBox(NodeArray<Point2D> nodePointMap, double xMin, double xMax, double yMin, double yMax) {
		var nodes = nodePointMap.keySet();
		var pxMin = nodes.stream().mapToDouble(v -> nodePointMap.get(v).getX()).min().orElse(0);
		var pxMax = nodes.stream().mapToDouble(v -> nodePointMap.get(v).getX()).max().orElse(0);
		var pyMin = nodes.stream().mapToDouble(v -> nodePointMap.get(v).getY()).min().orElse(0);
		var pyMax = nodes.stream().mapToDouble(v -> nodePointMap.get(v).getY()).max().orElse(0);

		for (var v : nodes) {
			var px = nodePointMap.get(v).getX();
			var py = nodePointMap.get(v).getY();

			var x = xMin + (px - pxMin) * (xMax - xMin) / (pxMax - pxMin);

			var y = yMin + (py - pyMin) * (yMax - yMin) / (pyMax - pyMin);

			nodePointMap.put(v, new Point2D(x, y));
		}
	}
}

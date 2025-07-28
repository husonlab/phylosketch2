package xtra.treesnet;

import jloda.graph.Edge;
import jloda.phylo.PhyloTree;

import java.util.*;

public class TreeEdgetoNetworkEdgeMap {

    private final PhyloTree tree1;
    private final PhyloTree tree2;
    private final PhyloTree network;

    private final Map<Edge, List<Edge>> tree1ToNetwork;
    private final Map<Edge, List<Edge>> tree2ToNetwork;

    public TreeEdgetoNetworkEdgeMap(PhyloTree tree1, PhyloTree tree2, PhyloTree network,
                                    Map<Edge, List<Edge>> tree1ToNetwork,
                                    Map<Edge, List<Edge>> tree2ToNetwork) {
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.network = network;
        this.tree1ToNetwork = tree1ToNetwork;
        this.tree2ToNetwork = tree2ToNetwork;
    }

    public List<Edge> getMappedEdgeFromTree1(Edge treeEdge) {
        return tree1ToNetwork.getOrDefault(treeEdge, Collections.emptyList());
    }

    public List<Edge> getMappedEdgeFromTree2(Edge treeEdge) {
        return tree2ToNetwork.getOrDefault(treeEdge, Collections.emptyList());
    }

    public Map<Edge, List<Edge>> getTree1Map() {
        return tree1ToNetwork;
    }

    public Map<Edge, List<Edge>> getTree2Map() {
        return tree2ToNetwork;
    }

    public PhyloTree getTree1() {
        return tree1;
    }

    public PhyloTree getTree2() {
        return tree2;
    }

    public PhyloTree getNetwork() {
        return network;
    }

    public void addMappingFromTree1(Edge treeEdge, Edge networkEdge) {
        tree1ToNetwork.computeIfAbsent(treeEdge, k -> new ArrayList<>()).add(networkEdge);
    }

    public void addMappingFromTree2(Edge treeEdge, Edge networkEdge) {
        tree2ToNetwork.computeIfAbsent(treeEdge, k -> new ArrayList<>()).add(networkEdge);
    }

}

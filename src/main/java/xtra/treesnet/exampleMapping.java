package xtra.treesnet;

import jloda.graph.Edge;
import java.util.ArrayList;
import java.util.List;

public class exampleMapping {

    public static void populateHardwiredMap(treeEdgetoNetworkEdgeMap hwMap) {
        // Example: manually map tree edges to network edges

        List<Edge> tree1Edges = new ArrayList<>(hwMap.getTree1().getEdgesAsList());
        List<Edge> tree2Edges = new ArrayList<>(hwMap.getTree2().getEdgesAsList());
        List<Edge> networkEdges = new ArrayList<>(hwMap.getNetwork().getEdgesAsList());


        //Tree1 mapping
        hwMap.addMappingFromTree1(tree1Edges.get(7), networkEdges.get(0));
        hwMap.addMappingFromTree1(tree1Edges.get(6), networkEdges.get(9));
        hwMap.addMappingFromTree1(tree1Edges.get(5), networkEdges.get(1));
        hwMap.addMappingFromTree1(tree1Edges.get(5), networkEdges.get(4));
        hwMap.addMappingFromTree1(tree1Edges.get(4), networkEdges.get(8));
        hwMap.addMappingFromTree1(tree1Edges.get(3), networkEdges.get(7));
        hwMap.addMappingFromTree1(tree1Edges.get(2), networkEdges.get(2));
        hwMap.addMappingFromTree1(tree1Edges.get(2), networkEdges.get(10));
        hwMap.addMappingFromTree1(tree1Edges.get(1), networkEdges.get(6));
        hwMap.addMappingFromTree1(tree1Edges.get(0), networkEdges.get(5));


        //Tree2 mapping
        hwMap.addMappingFromTree2(tree2Edges.get(7), networkEdges.get(0));
        hwMap.addMappingFromTree2(tree2Edges.get(6), networkEdges.get(9));
        hwMap.addMappingFromTree2(tree2Edges.get(5), networkEdges.get(4));
        hwMap.addMappingFromTree2(tree2Edges.get(4), networkEdges.get(1));
        hwMap.addMappingFromTree2(tree2Edges.get(3), networkEdges.get(2));
        hwMap.addMappingFromTree2(tree2Edges.get(3), networkEdges.get(3));
        hwMap.addMappingFromTree2(tree2Edges.get(2), networkEdges.get(8));
        hwMap.addMappingFromTree2(tree2Edges.get(1), networkEdges.get(6));
        hwMap.addMappingFromTree2(tree2Edges.get(1), networkEdges.get(7));
        hwMap.addMappingFromTree2(tree2Edges.get(0), networkEdges.get(5));


    }

}

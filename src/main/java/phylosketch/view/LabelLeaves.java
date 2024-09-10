/*
 * LabelLeaves.java Copyright (C) 2023 Daniel H. Huson
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

package phylosketch.view;

import javafx.stage.Stage;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.phylo.PhyloTree;
import jloda.util.CollectionUtils;
import jloda.util.Pair;


import java.util.*;
import java.util.stream.Collectors;

/**
 * label all leaves
 * Daniel Huson, 1.2020
 */
public class LabelLeaves {

    public static List<ChangeNodeLabelsCommand.Data> labelLeavesABC(DrawPane editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));

        return sortLeaves(editor).stream().filter(v -> editor.getLabel(v).getRawText().isEmpty()).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabelABC(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelInternalABC(DrawPane editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortInternal(editor).stream().filter(v -> editor.getLabel(v).getRawText().isEmpty()).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabelABC(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelLeaves123(DrawPane editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortLeaves(editor).stream().filter(v -> editor.getLabel(v).getRawText().isEmpty()).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabel123(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelInternal123(DrawPane editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortInternal(editor).stream().filter(v -> editor.getLabel(v).getRawText().isEmpty()).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabel123(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> clear(DrawPane editor) {
        var nodes=(editor.getNodeSelection().size()>0?
           editor.getNodeSelection().getSelectedItems():editor.getGraph().getNodesAsList());
        return nodes.stream().map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), "")).collect(Collectors.toList());
    }

    public static void labelLeaves(Stage owner, DrawPane drawPane) {
        final List<Node> leaves = sortLeaves(drawPane);

        if(drawPane.getNodeSelection().size()>0) {
			leaves.removeIf(v -> !drawPane.getNodeSelection().isSelected(v));
        }

        for (var v : leaves) {
            drawPane.getNodeSelection().clearSelection();
            drawPane.getNodeSelection().select(v);
            if (!NodeLabelDialog.apply(owner, drawPane, v))
                break;
        }
    }

    private static List<Node> sortLeaves(DrawPane drawPane) {
        var graph = drawPane.getGraph();

        var list=new ArrayList<Pair<Node, Double>>();
       for(var component: ConnectedComponents.components(graph)) {
           if (RootLocation.compute(component).isHorizontal())
               list.addAll(component.stream().filter(v -> v.getOutDegree() == 0).map(v -> new Pair<>(v, DrawPane.getY(v))).toList());
           else
               list.addAll(component.stream().filter(v -> v.getOutDegree() == 0).map(v -> new Pair<>(v, DrawPane.getX(v))).toList());
       }

        return list.stream().sorted(Comparator.comparingDouble(Pair::getSecond)).map(Pair::getFirst).collect(Collectors.toList());
    }

    private static List<Node> sortInternal(DrawPane drawPane) {
        var graph = drawPane.getGraph();

        var list=new ArrayList<Pair<Node, Double>>();
        for(var component: ConnectedComponents.components(graph)) {
            if (RootLocation.compute(component).isHorizontal())
                list.addAll(component.stream().filter(v -> v.getOutDegree() > 0).map(v -> new Pair<>(v, DrawPane.getY(v))).toList());
            else
                list.addAll(component.stream().filter(v -> v.getOutDegree() > 0).map(v -> new Pair<>(v, DrawPane.getX(v))).toList());
        }
        return list.stream().sorted(Comparator.comparingDouble(Pair::getSecond)).map(Pair::getFirst).collect(Collectors.toList());
    }

    public static String getNextLabelABC(Set<String> seen) {
        int id = 0;
        String label = "A";
        while (seen.contains(label)) {
            id++;
            int letter = ('A' + (id % 26));
            int number = id / 26;
            label = (char) letter + (number > 0 ? "_" + number : "");
        }
        seen.add(label);
        return label;
    }

    public static String getNextLabel123(Set<String> seen) {
        int id = 1;
        String label = "t" + id;
        while (seen.contains(label)) {
            id++;
            label = "t" + id;

        }
        seen.add(label);
        return label;
    }
}

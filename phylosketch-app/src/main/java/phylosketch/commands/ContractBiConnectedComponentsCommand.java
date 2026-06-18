package phylosketch.commands;

import jloda.fx.undo.CompositeCommand;
import jloda.graph.Edge;
import jloda.graph.algorithms.BiconnectedComponents;
import jloda.util.IteratorUtils;
import org.apache.commons.collections4.SetUtils;
import phylosketch.view.DrawView;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * contract all bi-connected components
 * Daniel Huson, 5.2026
 */
public class ContractBiConnectedComponentsCommand extends CompositeCommand {

	public ContractBiConnectedComponentsCommand(DrawView view) {
		super("contract blobs");

		var components = (new BiconnectedComponents()).findBiconnectedComponents(view.getGraph());
		var nodes = new HashSet<>(view.getSelectedOrAllNodes());
		components.retainAll(components.stream().filter(nodes::containsAll).toList());

		for (var component : components) {
			if (component.size() > 2) {
				var cutNodes = component.stream().filter(v -> !component.containsAll(IteratorUtils.asList(v.adjacentNodes()))).collect(Collectors.toSet());
				if (!cutNodes.isEmpty()) {
					var nonCutNodes = SetUtils.difference(component, cutNodes);
					var interCutEdges = new HashSet<Edge>();
					for (var v : cutNodes) {
						for (var e : v.adjacentEdges()) {
							if (cutNodes.contains(e.getOpposite(v)))
								interCutEdges.add(e);
						}
					}
					if (!nonCutNodes.isEmpty() || !interCutEdges.isEmpty()) {
						var deleteCommand = new DeleteCommand(view, nonCutNodes, interCutEdges);
						add(deleteCommand);
					}
					var mergeNodesCommand = new MergeNodesCommand(view, cutNodes);
					add(mergeNodesCommand);
				}
			}
		}
	}
}

/*
 * TreesNetPresenter.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.treesnet;


import javafx.application.Platform;

import java.io.IOException;
import java.util.HashMap;

/**
 * the TreesNet presenter
 * Daniel Huson, 4.2025
 */
public class TreesNetPresenter {
	private final TreesNetDocument document;
	private final TreesNetController controller;

	public TreesNetPresenter(TreesNetViewer viewer, TreesNetDocument document, TreesNetController controller) {
		this.document = document;
		this.controller = controller;

		document.embedToScaleProperty().bindBidirectional(controller.getToScaleToggle().selectedProperty());
		document.embedWidthProperty().bind(controller.getMainBorderPane().widthProperty());
		document.embedHeightProperty().bind(controller.getMainBorderPane().heightProperty());

		document.embedToScaleProperty().addListener(e -> embedNetwork());

		controller.getDoneButton().setOnAction(e -> Platform.exit());
	}

	public void load(String treesFile, String networkFile) throws IOException {
		document.loadTrees(treesFile);
		document.loadNetwork(networkFile);
	}

	public void embedNetwork() {
		controller.getCenterPane().getChildren().clear();
		if (document.getEmbedWidth() > 120 && document.getEmbedHeight() > 120) {
			controller.getCenterPane().getChildren().add(EmbedNetwork.apply(document.getNetwork(), controller.getToScaleToggle().isSelected(), 20, document.getEmbedWidth() - 120, 20, document.getEmbedHeight() - 120));
		}
	}

	public void mapExample() throws IOException {
		treeEdgetoNetworkEdgeMap hwMap = new treeEdgetoNetworkEdgeMap(document.getTrees().get(0), document.getTrees().get(1), document.getNetwork(), new HashMap<>(), new HashMap<>());
		exampleMapping.populateHardwiredMap(hwMap);

		System.err.println("Tree 1 mapping:");
		for(var i:hwMap.getTree1Map().keySet()){
			System.err.println(i + " --> " + hwMap.getTree1Map().get(i));
		}

		System.err.println("Tree 2 mapping:");
		for(var i:hwMap.getTree2Map().keySet()){
			System.err.println(i + " --> " + hwMap.getTree2Map().get(i));
		}
	}

}

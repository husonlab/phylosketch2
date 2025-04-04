/*
 * ExploreLayoutOptimization.java Copyright (C) 2025 Daniel H. Huson
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

package xtra.test;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.util.Icebergs;
import jloda.graph.Node;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CollectionUtils;
import phylosketch.embed.CircularPhylogenyLayout;
import phylosketch.embed.HeightAndAngles;
import phylosketch.embed.RectangularPhylogenyLayout;
import phylosketch.embed.optimize.OptimizeLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * explore the layout optimization algorithm
 */
public class ExploreLayoutOptimization extends Application {
	private static BooleanProperty circular = new SimpleBooleanProperty();

	@Override
	public void start(Stage stage) throws Exception {
		PhyloTree.SUPPORT_RICH_NEWICK = true;

		Icebergs.setEnabled(true);
		//var newick="(a,b,c);";
/*
		var newick = """
				((Trithuria_inconspicua_NC_020372,Trithuria_filamentosa_KF696682),(((((((Barclaya_kunstleri_KY392762,Barclaya_longifolia_KY284156))#H2,((((((Nymphaea_jamesoniana_NC_031826,(Nymphaea_ampla_KU189255)#H4),(Euryale_ferox_KY392765)#H5))#H3,(Nymphaea_mexicana_NC_024542,(Nymphaea_alba_KU234277,Nymphaea_alba_NC_006050))),#H4),(((Victoria_cruziana_KY001813,#H5),#H4),#H3))))#H1,(Cabomba_caroliniana_KT705317,Brasenia_schreberi_NC_031343)),(#H1,(((((Nuphar_shimadae_MH050797,(Nuphar_pumila_MH050796)#H7))#H6,(Nuphar_advena_NC_008788,Nuphar_longifolia_MH050795)),#H7),#H6))),#H2))[&&NHX:GN=N1];
				""";

		var newick = """
				((Trithuria_inconspicua_NC_020372,Trithuria_filamentosa_KF696682),((((((((((((Victoria_cruziana_KY001813,(Euryale_ferox_KY392765)#H2),((Nymphaea_alba_KU234277,Nymphaea_alba_NC_006050))#H3),(Nymphaea_jamesoniana_NC_031826,(Nymphaea_ampla_KU189255)#H4)),((Barclaya_kunstleri_KY392762,Barclaya_longifolia_KY284156))#H5,((Nymphaea_mexicana_NC_024542,#H3),#H2)),#H4),#H2),#H5))#H1,(Nuphar_advena_NC_008788,Nuphar_longifolia_MH050795)),(Nuphar_shimadae_MH050797,Nuphar_pumila_MH050796)),(Cabomba_caroliniana_KT705317,Brasenia_schreberi_NC_031343)),#H1))[&&NHX:GN=N1];
				""";
*/
		var newick = """
				(((((B)#H2,(C)#H3))#H1,(A,#H2)),((D,#H3),#H1));
				""";

		// var newick="((a,b));";

		var tree = new PhyloTree();
		tree.parseBracketNotation(newick, true);
		tree.setRoot(tree.nodeStream().filter(v -> v.getInDegree() == 0).toList().get(0));

		Map<Node, Point2D> points = tree.newNodeArray();

		var nodesGroup = new Group();
		var edgesGroup = new Group();
		var labelsGroup = new Group();
		var otherGroup = new Group();

		var orderingGraphGroup = new Group();

		var width = 600;
		var height = 600;

		var toScale = new SimpleBooleanProperty(this, "toScale", false);

		var random = new Random(666);

		LSAUtils.setLSAChildrenAndTransfersMap(tree);

		RectangularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, false, points);

		if (circular.get()) {
			CircularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, false, points);
		} else
			RectangularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, false, points);

		DrawNetwork.apply(circular.get(), tree, points, nodesGroup, edgesGroup, labelsGroup);

		tree.nodeStream().forEach(u -> setupMouseClicked(tree, u, points, nodesGroup, edgesGroup, labelsGroup, orderingGraphGroup));

		var worldGroup = new Group();
		worldGroup.getChildren().addAll(edgesGroup, nodesGroup, labelsGroup, otherGroup);
		var centerPane = new StackPane(worldGroup);
		StackPane.setMargin(worldGroup, new Insets(50, 50, 50, 50));
		var rootPane = new BorderPane(new ZoomableScrollPane(centerPane));
		var sidePane = new Pane(orderingGraphGroup);
		sidePane.setMinWidth(150);
		sidePane.setStyle("-fx-border-color: gray;");
		rootPane.setRight(sidePane);

		var textField = new TextField(newick);
		textField.setOnAction(e -> {
			var text = textField.getText();
			if (!text.isBlank()) {
				if (!text.endsWith(";"))
					text += ";";
				try {
					random.setSeed(666);
					tree.parseBracketNotation(text, true);
					orderingGraphGroup.getChildren().clear();
					otherGroup.getChildren().clear();

					LSAUtils.setLSAChildrenAndTransfersMap(tree);

					if (circular.get())
						CircularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, false, points);
					else
						RectangularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, false, points);

					DrawNetwork.apply(circular.get(), tree, points, nodesGroup, edgesGroup, labelsGroup);

					tree.nodeStream().forEach(u -> setupMouseClicked(tree, u, points, nodesGroup, edgesGroup, labelsGroup, orderingGraphGroup));
				} catch (IOException ex) {
					Basic.caught(ex);
				}
			}
		});
		HBox.setHgrow(textField, Priority.ALWAYS);
		var toScaleButton = new ToggleButton("Scale");
		toScaleButton.selectedProperty().bindBidirectional(toScale);
		rootPane.setTop(new ToolBar(textField, toScaleButton));
		centerPane.setOnMouseClicked(e -> {
			nodesGroup.getChildren().forEach(n -> n.setEffect(null));
			edgesGroup.getChildren().forEach(n -> n.setEffect(null));
			orderingGraphGroup.getChildren().clear();
			e.consume();
		});


		var optimizeButton = new Button("Optimize");
		optimizeButton.setOnAction(e -> {
			otherGroup.getChildren().clear();
			orderingGraphGroup.getChildren().clear();
			if (circular.get())
				CircularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, true, points);
			else
				RectangularPhylogenyLayout.apply(tree, toScale.get(), HeightAndAngles.Averaging.LeafAverage, OptimizeLayout.How.Rectangular, random, points);

			DrawNetwork.apply(circular.get(), tree, points, nodesGroup, edgesGroup, labelsGroup);

			tree.nodeStream().forEach(u -> setupMouseClicked(tree, u, points, nodesGroup, edgesGroup, labelsGroup, orderingGraphGroup));
		});
		var circularButton = new ToggleButton("Circular");
		circularButton.selectedProperty().bindBidirectional(circular);

		var buttonBar = new ButtonBar();
		buttonBar.getButtons().addAll(circularButton, optimizeButton);
		VBox.setMargin(buttonBar, new Insets(2, 10, 2, 10));
		rootPane.setBottom(new VBox(buttonBar));

		stage.setScene(new Scene(rootPane));
		stage.setWidth(width + 300);
		stage.setHeight(height + 150);
		stage.show();
	}

	public void setupMouseClicked(PhyloTree tree, Node v, Map<Node, Point2D> points, Group nodesGroup, Group edgesGroup, Group labelsGroup, Group orderingGraphGroup) {
		var lsaChildren = tree.getLSAChildrenMap();

		if (v.getInfo() instanceof Shape shape) {
			shape.setOnMouseClicked(e -> {
				nodesGroup.getChildren().forEach(n -> n.setEffect(null));
				edgesGroup.getChildren().forEach(n -> n.setEffect(null));
				labelsGroup.getChildren().forEach(n -> n.setEffect(null));

				var originalOrdering = new ArrayList<>(lsaChildren.get(v));
				DrawOrderingGraph.apply(v, originalOrdering, lsaChildren, points, orderingGraphGroup);
				if (e.getClickCount() == 2) {
					RectangularPhylogenyLayout.apply(tree, false, HeightAndAngles.Averaging.LeafAverage, false, points);
					if (e.isShiftDown()) {
						lsaChildren.put(v, CollectionUtils.reverse(lsaChildren.get(v)));
					} else {
						var originalScore = OptimizeLayout.computeCost(tree, lsaChildren, points, OptimizeLayout.How.None);
						if (OptimizeLayout.optimizeOrdering(v, lsaChildren, points, new Random(), circular.get() ? OptimizeLayout.How.Circular : OptimizeLayout.How.Rectangular)) {
							var newScore = OptimizeLayout.computeCost(tree, lsaChildren, points, circular.get() ? OptimizeLayout.How.Circular : OptimizeLayout.How.Rectangular);
							System.err.printf("Layout optimization: %d -> %d%n", originalScore, newScore);
						}
					}


					if (circular.get()) {
						CircularPhylogenyLayout.apply(tree, false, HeightAndAngles.Averaging.LeafAverage, false, points);
					} else {
						RectangularPhylogenyLayout.apply(tree, false, HeightAndAngles.Averaging.LeafAverage, false, points);
					}
					DrawNetwork.apply(circular.get(), tree, points, nodesGroup, edgesGroup, labelsGroup);

					tree.nodeStream().forEach(u -> setupMouseClicked(tree, u, points, nodesGroup, edgesGroup, labelsGroup, orderingGraphGroup));
					DrawOrderingGraph.apply(v, originalOrdering, lsaChildren, points, orderingGraphGroup);
				}
				e.consume();
			});
		}
	}
}

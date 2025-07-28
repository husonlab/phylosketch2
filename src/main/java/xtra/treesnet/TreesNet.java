/*
 * TreesNet.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jloda.fx.util.ArgsOptions;
import jloda.fx.util.RunAfterAWhile;
import jloda.phylo.PhyloTree;
import jloda.util.UsageException;
import phylosketch.main.PhyloSketch;

import java.io.IOException;

/**
 * The TreesNet main application class
 * Daniel Huson, 4.2025
 */
public class TreesNet extends Application {
	private static String initialTreesFile;
	private static String initialNetworkFile;

	@Override
	public void start(Stage stage) throws Exception {
		var viewer = new TreesNetViewer();
		stage.setTitle("TreesNet");
		stage.setScene(new Scene(viewer.getRoot(), 800, 800));
		stage.sizeToScene();
		stage.show();

		// load initial trees and networkL:
		RunAfterAWhile.applyInFXThread(this, () -> {
			try {
				System.err.println("Loading trees and network...");
				viewer.getPresenter().load(initialTreesFile, initialNetworkFile);
				viewer.getPresenter().embedNetwork();
				viewer.getPresenter().mapExample();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		});
	}

	public static void main(String[] args) throws UsageException, IOException {
		PhyloTree.SUPPORT_RICH_NEWICK = true;
		parseArguments(args);
		launch(args);
	}

	private static void parseArguments(String[] args) throws IOException, UsageException {
		var options = new ArgsOptions(args, PhyloSketch.class, "Sketch trees in network");

		options.comment("Input:");
		initialTreesFile = options.getOptionMandatory("-it", "trees", "Input trees file", "");
		initialNetworkFile = options.getOptionMandatory("-in", "network", "Input network", "");
		options.done();
	}
}

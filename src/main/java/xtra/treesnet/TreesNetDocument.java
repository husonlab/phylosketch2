/*
 * TreesNetDocument.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * the TreesNet document
 * Daniel Huson, 4.2025
 */
public class TreesNetDocument {
	private final BooleanProperty embedToScale = new SimpleBooleanProperty(this, "embedToScale", true);
	private final DoubleProperty embedWidth = new SimpleDoubleProperty(this, "embedWidth", 1.0);
	private final DoubleProperty embedHeight = new SimpleDoubleProperty(this, "embedHeight", 1.0);

	private final ObservableList<PhyloTree> trees = FXCollections.observableArrayList();
	private final ObjectProperty<PhyloTree> network = new SimpleObjectProperty<>(new PhyloTree());

	public TreesNetDocument() {
	}

	public void loadTrees(String inputFile) throws IOException {
		try (var r = new BufferedReader(new FileReader(inputFile))) {
			while (r.ready()) {
				var line = r.readLine();
				if (!line.startsWith("#") && !line.isEmpty()) {
					trees.add(NewickIO.valueOf(line));
				}
			}
		}
	}

	public void loadNetwork(String inputFile) throws IOException {
		try (var r = new BufferedReader(new FileReader(inputFile))) {
			while (r.ready()) {
				var line = r.readLine();
				if (!line.startsWith("#") && !line.isEmpty()) {
					network.set(NewickIO.valueOf(line));
					break;
				}
			}
		}
	}

	public ObservableList<PhyloTree> getTrees() {
		return trees;
	}

	public PhyloTree getNetwork() {
		return network.get();
	}

	public ObjectProperty<PhyloTree> networkProperty() {
		return network;
	}

	public boolean isEmbedToScale() {
		return embedToScale.get();
	}

	public BooleanProperty embedToScaleProperty() {
		return embedToScale;
	}

	public double getEmbedWidth() {
		return embedWidth.get();
	}

	public DoubleProperty embedWidthProperty() {
		return embedWidth;
	}

	public double getEmbedHeight() {
		return embedHeight.get();
	}

	public DoubleProperty embedHeightProperty() {
		return embedHeight;
	}
}

/*
 * TreesNetViewer.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

/**
 * The TreesNet viewer
 * Daniel Huson, 4.2025
 */
public class TreesNetViewer {
	private final TreesNetDocument document;
	private final TreesNetController controller;
	private final TreesNetPresenter presenter;
	private final Parent root;

	public TreesNetViewer() {
		this.document = new TreesNetDocument();
		var fxmlLoader = new FXMLLoader();
		try (var ins = Objects.requireNonNull(TreesNetController.class.getResource("TreesNet.fxml")).openStream()) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		controller = fxmlLoader.getController();
		root = fxmlLoader.getRoot();
		presenter = new TreesNetPresenter(this, document, controller);
	}

	public Parent getRoot() {
		return root;
	}

	public TreesNetDocument getDocument() {
		return document;
	}

	public TreesNetController getController() {
		return controller;
	}

	public TreesNetPresenter getPresenter() {
		return presenter;
	}
}

/*
 * FormatPaneView.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.format;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import jloda.fx.util.StatementFilter;
import phylosketch.main.PhyloSketch;
import phylosketch.view.DrawPane;

import java.io.IOException;
import java.util.Objects;

/**
 * the format pane window
 * Daniel Huson, 11.2024
 */
public class FormatPaneView {
	private final Pane pane;
	private final FormatPaneController controller;
	private final FormatPanePresenter presenter;

	public FormatPaneView(DrawPane drawPane, BooleanProperty show) {
		var fxmlLoader = new FXMLLoader();
		try (var ins = StatementFilter.applyMobileFXML(Objects.requireNonNull(FormatPaneController.class.getResource("FormatPane.fxml")).openStream(), PhyloSketch.isDesktop())) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		controller = fxmlLoader.getController();
		pane = controller.getRootPane();

		presenter = new FormatPanePresenter(drawPane, controller, show);

	}

	public Pane getPane() {
		return pane;
	}

	public FormatPaneController getController() {
		return controller;
	}

	public FormatPanePresenter getPresenter() {
		return presenter;
	}
}

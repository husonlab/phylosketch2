/*
 * HelpView.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.help;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import jloda.fx.util.StatementFilter;
import phylosketch.main.PhyloSketch;

import java.io.IOException;
import java.util.Objects;

/**
 * the help tab view
 * Daniel Huson, 12.2024
 */
public class HelpView {
	private final HelpController controller;
	private final HelpPresenter presenter;
	private final Pane pane;

	public HelpView() {
		var fxmlLoader = new FXMLLoader();
		try (var ins = StatementFilter.applyMobileFXML(Objects.requireNonNull(HelpController.class.getResource("help.fxml")).openStream(), PhyloSketch.isDesktop())) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		controller = fxmlLoader.getController();
		pane = controller.getRootPane();

		presenter = new HelpPresenter(controller);
	}

	public HelpController getController() {
		return controller;
	}

	public HelpPresenter getPresenter() {
		return presenter;
	}

	public Pane getPane() {
		return pane;
	}
}

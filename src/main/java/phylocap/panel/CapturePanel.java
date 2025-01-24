/*
 * CapturePanel.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.panel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import phylocap.capture.CaptureService;

import java.io.IOException;
import java.util.Objects;

public class CapturePanel {
	private final CapturePanelController controller;
	private final CapturePanelPresenter presenter;
	private final Parent root;

	private final Parameters parameters = new Parameters();
	private final CaptureService service = new CaptureService();


	public CapturePanel(Pane pane, Pane bottomPane) {
		var fxmlLoader = new FXMLLoader();
		try (var ins = (Objects.requireNonNull(CapturePanelController.class.getResource("CapturePanel.fxml"))).openStream()) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		controller = fxmlLoader.getController();
		root = fxmlLoader.getRoot();

		presenter = new CapturePanelPresenter(this);

		service.setProgressParentPane(bottomPane);
	}

	public CapturePanelController getController() {
		return controller;
	}

	public CapturePanelPresenter getPresenter() {
		return presenter;
	}

	public Parent getRoot() {
		return root;
	}

	public Parameters getParameters() {
		return parameters;
	}
}

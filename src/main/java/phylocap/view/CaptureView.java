/*
 * CaptureView.java Copyright (C) 2025 Daniel H. Huson
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

package phylocap.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import phylocap.capture.CaptureService;

import java.io.IOException;
import java.util.Objects;

/**
 * the phylo capture tab
 * Daniel Huson, 1.2025
 */
public class CaptureView {
	private final CaptureViewController controller;
	private final CaptureViewPresenter presenter;
	private final Parent root;
	private final CaptureService service = new CaptureService();

	public CaptureView() {
		var fxmlLoader = new FXMLLoader();
		try (var ins = (Objects.requireNonNull(CaptureViewController.class.getResource("CaptureView.fxml"))).openStream()) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		controller = fxmlLoader.getController();
		root = fxmlLoader.getRoot();

		presenter = new CaptureViewPresenter(this);
	}

	public CaptureViewController getController() {
		return controller;
	}

	public CaptureViewPresenter getPresenter() {
		return presenter;
	}

	public Parent getRoot() {
		return root;
	}

	public CaptureService getService() {
		return service;
	}
}

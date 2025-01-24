/*
 * CapturePanelPresenter.java Copyright (C) 2025 Daniel H. Huson
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

import jloda.util.NumberUtils;

public class CapturePanelPresenter {
	public CapturePanelPresenter(CapturePanel panel) {
		var controller = panel.getController();
		var parameters = panel.getParameters();

		controller.getParameterCBox().valueProperty().addListener((v, o, n) -> {
			if (n != null) {
				controller.getParameterTextField().setText(String.valueOf(parameters.getValue(n)));
			}
		});

		controller.getParameterTextField().textProperty().addListener((v, o, n) -> {
			if (NumberUtils.isDouble(n)) {
				var key = controller.getParameterCBox().getValue();
				if (key != null) {
					parameters.setValue(key, NumberUtils.parseDouble(n));
				}
			}
		});
		controller.getParameterTextField().disableProperty().bind(controller.getParameterCBox().valueProperty().isNull());

		controller.getResetButton().setOnAction(e -> {
			var key = controller.getParameterCBox().getValue();
			if (key != null) {
				controller.getParameterTextField().setText(String.valueOf(key.getDefaultValue()));
			}
		});
		controller.getResetButton().disableProperty().bind(controller.getParameterCBox().valueProperty().isNull());


	}
}

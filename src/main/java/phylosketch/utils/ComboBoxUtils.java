/*
 * ComboBoxUtils.java Copyright (C) 2024 Daniel H. Huson
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

package phylosketch.utils;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import jloda.util.StringUtils;

/**
 * combobox utilities
 * Daniel Huson, 12.2024
 */
public class ComboBoxUtils {
	/**
	 * ensure typed values are double
	 *
	 * @param comboBox the combo box
	 */
	public static void ensureDoubleInput(ComboBox<Double> comboBox) {
		comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
			if (!newValue.matches("\\d*(\\.\\d*)?")) {
				comboBox.getEditor().setText(oldValue);
			}
		});
		comboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(Double value) {
				return value == null ? "" : StringUtils.removeTrailingZerosAfterDot(value);
			}

			@Override
			public Double fromString(String text) {
				try {
					return Double.parseDouble(text);
				} catch (NumberFormatException e) {
					return comboBox.getValue();
				}
			}
		});
		comboBox.setOnAction(e -> {
			var input = comboBox.getEditor().getText();
			try {
				var value = Double.parseDouble(input);
				if (!comboBox.getItems().contains(value)) {
					comboBox.getItems().add(value);
					comboBox.getItems().sort(Double::compare);
				}
				comboBox.setValue(value);
			} catch (NumberFormatException ignored) {
			}
		});

	}
}

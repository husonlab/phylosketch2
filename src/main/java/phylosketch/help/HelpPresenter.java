/*
 * HelpPresenter.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.web.WebEngine;
import jloda.fx.util.ClipboardUtils;
import jloda.util.Single;

/**
 * the help tab presenter
 * Daniel Huson, 12.2024
 */
public class HelpPresenter {

	private final WebEngine webEngine;

	public HelpPresenter(HelpController controller) {
		webEngine = controller.getWebView().getEngine();

		webEngine.setOnAlert(event -> System.out.println("JavaScript Alert: " + event.getData()));

		webEngine.setOnError(event -> System.err.println("JavaScript Error: " + event.getMessage()));

		webEngine.executeScript("""
				    console.log = function(message) {
				        alert(message); // Use alert to capture in setOnAlert
				    }
				""");

		controller.getHomeButton().setOnAction(e -> webEngine.executeScript("window.scrollTo(0, 0);"));

		var matches = new Single<>(0);

		controller.getFindCBox().setOnAction(e -> {
			var value = controller.getFindCBox().getValue();
			if (value != null && !value.isBlank()) {
				if (webEngine.executeScript("searchText('" + value + "')") instanceof Integer count) {
					matches.set(count);
					controller.getCountLabel().setText(String.valueOf(count));
					if (count > 0) {
						if (!controller.getFindCBox().getItems().contains(value)) {
							Platform.runLater(() -> {
								controller.getFindCBox().getItems().add(0, value);
								controller.getFindCBox().setValue(value);
							});
						}
					}
				} else {
					matches.set(0);
					controller.getCountLabel().setText("");
				}
			}
		});

		controller.getNextButton().setOnAction(event -> {
			if (webEngine.executeScript("nextMatch()") instanceof Integer count) {
				controller.getCountLabel().setText("%d of %d".formatted(count, matches.get()));
			}
		});
		controller.getNextButton().disableProperty().bind(controller.getFindCBox().valueProperty().isEqualTo(""));
		controller.getPreviousButton().setOnAction(event -> {
			if (webEngine.executeScript("previousMatch()") instanceof Integer count) {
				controller.getCountLabel().setText("%d of %d".formatted(count, matches.get()));
			}
		});
		controller.getPreviousButton().disableProperty().bind(controller.getNextButton().disableProperty());

		controller.getHideButton().setOnAction(e -> controller.getRootPane().getScene().getWindow().hide());

		controller.getClearButton().setOnAction(e -> {
			controller.getFindCBox().setValue("");
			webEngine.executeScript("clearHighlights()");
			controller.getCountLabel().setText("");
		});

		controller.getCutMenuItem().setOnAction(e -> handleCut(controller.getFindCBox()));
		controller.getCutMenuItem().disableProperty().bind(ClipboardUtils.hasStringProperty().not().or(controller.getFindCBox().focusedProperty().not()).or(controller.getFindCBox().valueProperty().isNotEqualTo("")));

		controller.getCopyMenuItem().setOnAction(e -> handleCopy(controller.getFindCBox(), webEngine));
		controller.getCopyMenuItem().disableProperty().bind(controller.getWebView().focusedProperty().not()
				.and(ClipboardUtils.hasStringProperty().not().or(controller.getFindCBox().focusedProperty().not())
						.or(controller.getFindCBox().valueProperty().isNotEqualTo(""))));
		controller.getPasteMenuItem().setOnAction(e -> handlePaste(controller.getFindCBox()));
		controller.getPasteMenuItem().disableProperty().bind(controller.getFindCBox().focusedProperty().not());
	}

	public static void handleCut(ComboBox<String> comboBox) {
		if (comboBox.isFocused()) {
			var text = comboBox.getEditor().getSelectedText();
			if (text == null || text.isEmpty()) {
				text = comboBox.getEditor().getText();
			}
			if (!text.isEmpty()) {
				ClipboardUtils.putString(text);
				var fullText = comboBox.getEditor().getText();
				int start = comboBox.getEditor().getSelection().getStart();
				int end = comboBox.getEditor().getSelection().getEnd();
				comboBox.getEditor().setText(fullText.substring(0, start) + fullText.substring(end));
			}
		}
	}

	public static void handleCopy(ComboBox<String> comboBox, WebEngine webEngine) {
		if (comboBox.isFocused()) {
			var text = comboBox.getEditor().getSelectedText();
			if (text == null || text.isEmpty()) {
				text = comboBox.getEditor().getText();
			}
			ClipboardUtils.putString(text);
		} else {
			webEngine.executeScript("document.execCommand('copy');");
		}
	}

	public static void handlePaste(ComboBox<String> comboBox) {
		if (ClipboardUtils.hasString()) {
			var pasteText = ClipboardUtils.getString();
			if (comboBox.isFocused()) {
				comboBox.getEditor().appendText(pasteText);
			}
		}
	}
}

/*
 * HelpController.java Copyright (C) 2024 Daniel H. Huson
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

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import jloda.fx.icons.MaterialIcons;
import jloda.fx.util.RunAfterAWhile;
import jloda.util.ProgramProperties;
import phylosketch.main.PhyloSketch;

import java.util.Arrays;

/**
 * the help tab controller
 * Daniel Huson, 12.2024
 */
public class HelpController {
	@FXML
	private ToolBar toolBar;


	@FXML
	private Button clearButton;

	@FXML
	private MenuItem closeMenuItem;

	@FXML
	private MenuItem copyMenuItem;

	@FXML
	private Label countLabel;

	@FXML
	private MenuItem cutMenuItem;

	@FXML
	private MenuItem deleteMenuItem;

	@FXML
	private ComboBox<String> findCBox;

	@FXML
	private Button hideButton;

	@FXML
	private Button homeButton;

	@FXML
	private MenuItem homeMenuItem;

	@FXML
	private Button nextButton;

	@FXML
	private Button zoomInButton;

	@FXML
	private Button zoomOutButton;

	@FXML
	private MenuItem nextMenuItem;

	@FXML
	private MenuItem pasteMenuItem;

	@FXML
	private Button previousButton;

	@FXML
	private MenuItem previousMenuItem;

	@FXML
	private BorderPane rootPane;

	@FXML
	private WebView webView;


	@FXML
	private void initialize() {
		MaterialIcons.setIcon(homeButton, MaterialIcons.home);
		MaterialIcons.setIcon(clearButton, MaterialIcons.backspace);
		MaterialIcons.setIcon(nextButton, MaterialIcons.arrow_forward_ios);
		MaterialIcons.setIcon(previousButton, MaterialIcons.arrow_back_ios_new);
		MaterialIcons.setIcon(zoomInButton, MaterialIcons.zoom_in);
		MaterialIcons.setIcon(zoomOutButton, MaterialIcons.zoom_out);
		MaterialIcons.setIcon(hideButton, MaterialIcons.close);

		findCBox.getItems().addAll(Arrays.asList(ProgramProperties.get("HelpSearchTerms", new String[0])));
		findCBox.getItems().addListener((InvalidationListener) e -> RunAfterAWhile.applyInFXThread(findCBox, () -> ProgramProperties.put("HelpSearchTerms",
				findCBox.getItems().subList(Math.max(0, findCBox.getItems().size() - 20), findCBox.getItems().size()).toArray(new String[0]))));

		{
			var url = HelpController.class.getResource("help.html");
			if (url != null) {
				webView.getEngine().load(url.toExternalForm());
			} else System.err.println("File not found: help.html");
		}

		if (!PhyloSketch.isDesktop()) {
			if (hideButton.getParent() instanceof Pane pane) {
				pane.getChildren().remove(hideButton);
			}
		}

		closeMenuItem.setOnAction(e -> hideButton.fire());

		deleteMenuItem.setOnAction(e -> clearButton.fire());
		deleteMenuItem.disableProperty().bind(clearButton.disableProperty());

		homeMenuItem.setOnAction(e -> homeButton.fire());
		homeMenuItem.disableProperty().bind(homeButton.disableProperty());
		nextMenuItem.setOnAction(e -> nextButton.fire());
		nextMenuItem.disableProperty().bind(nextButton.disableProperty());
		previousMenuItem.setOnAction(e -> previousButton.fire());
		previousMenuItem.disableProperty().bind(previousButton.disableProperty());

		zoomInButton.setOnAction(e -> webView.setZoom(1.2 * webView.getZoom()));
		zoomInButton.disableProperty().bind(webView.zoomProperty().greaterThan(4));
		zoomOutButton.setOnAction(e -> webView.setZoom(1 / 1.2 * webView.getZoom()));
		zoomOutButton.disableProperty().bind(webView.zoomProperty().lessThan(0.25));
	}

	public BorderPane getRootPane() {
		return rootPane;
	}

	public ComboBox<String> getFindCBox() {
		return findCBox;
	}

	public Label getCountLabel() {
		return countLabel;
	}

	public Button getHomeButton() {
		return homeButton;
	}

	public Button getNextButton() {
		return nextButton;
	}

	public Button getPreviousButton() {
		return previousButton;
	}

	public WebView getWebView() {
		return webView;
	}

	public Button getHideButton() {
		return hideButton;
	}

	public Button getClearButton() {
		return clearButton;
	}

	public MenuItem getCopyMenuItem() {
		return copyMenuItem;
	}

	public MenuItem getCutMenuItem() {
		return cutMenuItem;
	}

	public MenuItem getPasteMenuItem() {
		return pasteMenuItem;
	}

	public ToolBar getToolBar() {
		return toolBar;
	}
}
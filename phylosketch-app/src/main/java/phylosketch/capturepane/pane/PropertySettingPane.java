/*
 * PropertySettingPane.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capturepane.pane;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import javafx.util.converter.BooleanStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import jloda.fx.icons.MaterialIcons;
import phylosketch.utils.ScrollPaneUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * a property settings panel for numbers, boolean and string
 * Daniel Huson, 2.2025
 */
public class PropertySettingPane {
	public enum PropertyType {String, Boolean, Double, Integer, Unsupported}

	private final HBox pane;
	private final ChoiceBox<Property<?>> choiceBox;
	private final TextField textField;
	private final Button resetButton;
	private final Button closeButton;

	private final ObservableList<Property<?>> properties = FXCollections.observableArrayList();
	private final Map<Property<?>, Object> propertyResetMap = new HashMap<>();

	public PropertySettingPane() {
		pane = new HBox();
		pane.setAlignment(Pos.CENTER);
		pane.setPrefHeight(30);
		pane.setSpacing(10);
		pane.setStyle("-fx-background-color: -fx-control-inner-background;-fx-border-color: grey;-fx-border-width: 1;");

		choiceBox = new ChoiceBox<>(properties);
		choiceBox.setPrefWidth(130);
		choiceBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(Property<?> property) {
				return (property.getName() != null ? property.getName() : property.getClass().getSimpleName());
			}

			@Override
			public Property<?> fromString(String string) {
				return null;
			}
		});

		textField = new TextField();
		textField.setPrefWidth(100);

		resetButton = new Button("Reset");
		MaterialIcons.setIcon(resetButton, MaterialIcons.refresh, "-fx-font-size: 12;-fx-text-fill: gray;", true);
		resetButton.setOnAction(e -> {
			resetPropertyValue(choiceBox.getValue());
		});
		resetButton.disableProperty().bind(choiceBox.valueProperty().isNull());

		closeButton = new Button("Close");
		MaterialIcons.setIcon(closeButton, MaterialIcons.close);
		closeButton.setOnAction(e -> {
			if (pane.getParent() instanceof Pane parentPane) {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(parentPane, () -> parentPane.getChildren().remove(pane));
			} else if (pane.getParent() instanceof Group group) {
				ScrollPaneUtils.runRemoveAndKeepScrollPositions(group, () -> group.getChildren().remove(pane));
			}
		});

		pane.getChildren().addAll(choiceBox, textField, resetButton, closeButton);

		choiceBox.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> {
			if (n != null) {
				switch (getPropertyType(n)) {
					default -> textField.setTextFormatter(null);
					case Double -> textField.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
					case Integer -> textField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
					case Boolean -> textField.setTextFormatter(new TextFormatter<>(new BooleanStringConverter()));
				}
				textField.setText(n.getValue().toString());
				textField.setOnAction(e -> setPropertyValue(n, textField.getText()));
			}
		});
	}

	private void resetPropertyValue(Property<?> property) {
		switch (getPropertyType(property)) {
			case String -> ((StringProperty) property).setValue((String) propertyResetMap.get(property));
			case Boolean -> ((BooleanProperty) property).setValue((Boolean) propertyResetMap.get(property));
			case Double -> ((DoubleProperty) property).setValue((Double) propertyResetMap.get(property));
			case Integer -> ((IntegerProperty) property).setValue((Integer) propertyResetMap.get(property));
		}
	}

	private void setPropertyValue(Property<?> property, String value) {
		switch (getPropertyType(property)) {
			case String -> ((StringProperty) property).setValue(value);
			case Double -> ((DoubleProperty) property).setValue(Double.parseDouble(value));
			case Integer -> ((IntegerProperty) property).setValue(Integer.parseInt(value));
			case Boolean -> ((BooleanProperty) property).setValue(Boolean.parseBoolean(value));
		}
	}

	public PropertyType getPropertyType(Property<?> property) {
		if (property instanceof DoubleProperty) {
			return PropertyType.Double;
		} else if (property instanceof IntegerProperty) {
			return PropertyType.Integer;
		} else if (property instanceof BooleanProperty) {
			return PropertyType.Boolean;
		} else if (property instanceof StringProperty) {
			return PropertyType.String;
		} else return PropertyType.Unsupported;
	}

	public Pane getPane() {
		return pane;
	}

	public void addProperty(Property<?> property) {
		if (getPropertyType(property) == PropertyType.Unsupported) {
			throw new IllegalArgumentException("Unsupported property type: " + property.getClass());
		}
		var show = properties.isEmpty();
		properties.add(property);
		propertyResetMap.put(property, property.getValue());
		if (show)
			choiceBox.setValue(property);
	}

	public void addProperties(Collection<Property<?>> properties) {
		for (Property<?> property : properties) {
			addProperty(property);
		}
	}

	public void removeProperty(Property<?> property) {
		properties.remove(property);
	}
}

/*
 * Parameters.java Copyright (C) 2025 Daniel H. Huson
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import jloda.fx.util.ProgramProperties;

import java.util.Map;

public class Parameters {
	public enum Key {
		whiteThreshold(0.9), minTextHeight(10), maxTextHeight(60), maxDustDistance(20), minDustExtent(50);
		private final double defaultValue;

		Key(double defaultValue) {
			this.defaultValue = defaultValue;
		}

		public double getDefaultValue() {
			return defaultValue;
		}
	}

	private final Map<Key, DoubleProperty> map = FXCollections.observableHashMap();

	Parameters() {
		for (var key : Key.values()) {
			var property = new SimpleDoubleProperty(this, key.name());
			ProgramProperties.track(property, key.getDefaultValue());
			map.put(key, property);
		}
	}

	public DoubleProperty keyProperty(Key key) {
		return map.get(key);
	}

	public double getValue(Key key) {
		return keyProperty(key).get();
	}

	public void setValue(Key key, double value) {
		keyProperty(key).set(value);
	}
}

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

package phylosketch.capturepane.capture;

import javafx.beans.property.*;

import java.util.List;

/**
 * parameters used by the capture service
 * Daniel Huson, 2.2025
 */
public class Parameters {
	private final DoubleProperty minDistanceNodes = new SimpleDoubleProperty(null, "minDistanceNodes", 10);
	private final IntegerProperty minWordLength = new SimpleIntegerProperty(null, "minWordLength", 1);
	private final BooleanProperty mustContainLetter = new SimpleBooleanProperty(null, "mustContainLetter", true);
	private final DoubleProperty minTextHeight = new SimpleDoubleProperty(null, "minTextHeight", 10);
	private final DoubleProperty maxTextHeight = new SimpleDoubleProperty(null, "maxTextHeight", 100);
	private final DoubleProperty maxDustDistance = new SimpleDoubleProperty(null, "maxDustDistance", 20);
	private final DoubleProperty minDustExtent = new SimpleDoubleProperty(null, "minDustExtent", 20);
	private final BooleanProperty mustStartAlphaNumeric = new SimpleBooleanProperty(null, "mustStartAlphaNumeric", true);
	private final BooleanProperty mustEndAlphaNumeric = new SimpleBooleanProperty(null, "mustEndAlphaNumeric", true);
	private final List<Property<?>> all = List.of(minDistanceNodes, minWordLength, mustContainLetter, minTextHeight, maxTextHeight, mustStartAlphaNumeric, mustEndAlphaNumeric, maxDustDistance, minDustExtent);

	public Parameters() {
	}

	public double getMinDistanceNodes() {
		return minDistanceNodes.get();
	}

	public DoubleProperty minDistanceNodesProperty() {
		return minDistanceNodes;
	}

	public double getMinTextHeight() {
		return minTextHeight.get();
	}

	public DoubleProperty minTextHeightProperty() {
		return minTextHeight;
	}

	public double getMaxTextHeight() {
		return maxTextHeight.get();
	}

	public DoubleProperty maxTextHeightProperty() {
		return maxTextHeight;
	}

	public double getMaxDustDistance() {
		return maxDustDistance.get();
	}

	public DoubleProperty maxDustDistanceProperty() {
		return maxDustDistance;
	}

	public double getMinDustExtent() {
		return minDustExtent.get();
	}

	public DoubleProperty minDustExtentProperty() {
		return minDustExtent;
	}

	public boolean isMustStartAlphaNumeric() {
		return mustStartAlphaNumeric.get();
	}

	public BooleanProperty mustStartAlphaNumericProperty() {
		return mustStartAlphaNumeric;
	}

	public boolean isMustEndAlphaNumeric() {
		return mustEndAlphaNumeric.get();
	}

	public BooleanProperty mustEndAlphaNumericProperty() {
		return mustEndAlphaNumeric;
	}

	public boolean isMustContainLetter() {
		return mustContainLetter.get();
	}

	public BooleanProperty mustContainLetterProperty() {
		return mustContainLetter;
	}

	public int getMinWordLength() {
		return minWordLength.get();
	}

	public IntegerProperty minWordLengthProperty() {
		return minWordLength;
	}

	public List<Property<?>> getAll() {
		return all;
	}
}
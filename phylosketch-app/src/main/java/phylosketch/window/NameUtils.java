/*
 *  NameUtils.java Copyright (C) 2026 Daniel H. Huson
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
 */

package phylosketch.window;

import jloda.fx.window.MainWindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * name utils
 * Daniel Huson, 6.2026
 */
public class NameUtils extends jloda.util.NameUtils {
	/**
	 * derive a document name
	 *
	 * @param window the window
	 * @return human-readable name
	 */
	public static String deriveDocumentName(MainWindow window) {
		var phylo = window.getDrawView().getGraph();
		Supplier<Collection<String>> taxaSupplier = () -> phylo.nodeStream().filter(v -> v.getOutDegree() == 0).map(phylo::getLabel).filter(s -> s != null && !s.isBlank()).toList();
		IntSupplier hSupplier = () -> phylo.nodeStream().filter(v -> v.getInDegree() > 1).mapToInt(v -> v.getInDegree() - 1).sum();
		var name = NameUtils.deriveDocumentName(taxaSupplier, hSupplier);
		var otherDocumentNames = new ArrayList<String>();
		for (var w : MainWindowManager.getInstance().getMainWindows()) {
			if (w instanceof MainWindow other && other != window) {
				otherDocumentNames.add(other.getDocument().getName());
			}
		}
		return NameUtils.uniqueName(name, otherDocumentNames);
	}
}

/*
 * NewWindow.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.main;

import jloda.fx.window.MainWindowManager;
import phylosketch.window.MainWindow;

public class NewWindow {
	/**
	 * create a new window
	 *
	 * @return new window
	 */
	public static MainWindow apply() {
		return (MainWindow) MainWindowManager.getInstance().createAndShowWindow(false);
	}
}


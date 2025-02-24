/*
 * ExtensionFilters.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.io;

import javafx.stage.FileChooser;
import jloda.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExtensionFilters {
	private final static FileChooser.ExtensionFilter phyloSketch = new FileChooser.ExtensionFilter("PhyloSketch file", "*.psketch", "*.psketch.gz");
	private final static FileChooser.ExtensionFilter newick = new FileChooser.ExtensionFilter("Newick file", "*.nwk", "*.newick", "*.new", "*.tree", "*.tre", "*.trees", "*.treefile");
	private final static FileChooser.ExtensionFilter nexusPhyloSketch1 = new FileChooser.ExtensionFilter("Nexus file (by PhyloSketch1)", "*.nexus");
	private final static FileChooser.ExtensionFilter text = new FileChooser.ExtensionFilter("Text file", "*.txt");
	private final static FileChooser.ExtensionFilter any = new FileChooser.ExtensionFilter("Any file", "*.*");
	private final static List<FileChooser.ExtensionFilter> allSupported = createAllSupported();

	public static FileChooser.ExtensionFilter phyloSketch() {
		return phyloSketch;
	}

	public static FileChooser.ExtensionFilter newick() {
		return newick;
	}

	public static FileChooser.ExtensionFilter nexusPhyloSketch1() {
		return nexusPhyloSketch1;
	}

	public static FileChooser.ExtensionFilter createText() {
		return text;
	}

	public static FileChooser.ExtensionFilter any() {
		return any;
	}

	public static FileChooser.ExtensionFilter createAllSupported(Collection<FileChooser.ExtensionFilter> filters) {
		var descriptions = filters.stream().map(FileChooser.ExtensionFilter::getDescription).toList();
		var extensions = new ArrayList<String>();
		for (var other : filters) {
			extensions.addAll(other.getExtensions());
		}
		return new FileChooser.ExtensionFilter("All supported (" + StringUtils.toString(descriptions, ", ") + ")", extensions);
	}

	public static List<FileChooser.ExtensionFilter> allSupported() {
		return allSupported;
	}

	private static List<FileChooser.ExtensionFilter> createAllSupported() {
		var all = new ArrayList<FileChooser.ExtensionFilter>();
		all.add(phyloSketch());
		all.add(newick());
		all.add(nexusPhyloSketch1());
		all.add(createText());
		all.add(any());
		all.add(0, createAllSupported(all));
		return all;

	}
}


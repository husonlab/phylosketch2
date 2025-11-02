/*
 * SetupImport.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.view;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import phylosketch.main.PhyloSketch;

import java.io.IOException;
import java.util.function.Consumer;

import static jloda.fx.util.ClipboardUtils.isImageFile;
import static jloda.fx.util.ClipboardUtils.isTextFile;

/**
 * setup draw pane is import drag-and-drop target
 * Daniel Huson, 11.2025
 */
public class SetupImport {
	public static void apply(Pane pane, Consumer<String> importStringConsumer, java.util.function.Consumer<Image> importImageConsumer) {
		var dragOver = new SimpleBooleanProperty(false);
		var isDesktop = new SimpleBooleanProperty(PhyloSketch.isDesktop());


		pane.setOnDragOver(e -> {
			var db = e.getDragboard();
			if (e.getGestureSource() == pane && db.getString() != null || db.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				dragOver.set(true);
			}
			e.consume();
		});

		// Set up event handler for when files are dropped onto the button
		pane.setOnDragDropped(e -> {
			var db = e.getDragboard();
			boolean success = false;
			if (db.getString() != null && importStringConsumer != null) {
				importStringConsumer.accept(db.getString());
				success = true;
			} else if (db.hasFiles()) {
				var buf = new StringBuilder();
				for (var file : db.getFiles()) {
					if (FileUtils.fileExistsAndIsNonEmpty(file)) {
						if (importStringConsumer != null) {
							System.err.println("file");

							if (isTextFile(file)) {
								try {
									buf.append(StringUtils.toString(FileUtils.getLinesFromFile(file.getPath()), "\n"));
									success = true;
								} catch (IOException ignored) {
								}
							}
						}
						if (importImageConsumer != null) {
							if (isImageFile(file)) {
								importImageConsumer.accept(new Image(file.toURI().toString()));
								success = true;
								break;
							}
						}
					}
				}
				if (!buf.isEmpty() && importStringConsumer != null)
					importStringConsumer.accept(buf.toString());
			}
			e.setDropCompleted(success);
			e.consume();
		});
		pane.setOnDragExited(event -> {
			dragOver.set(false);
			event.consume();
		});
	}
}

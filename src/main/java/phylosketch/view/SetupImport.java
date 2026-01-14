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
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import jloda.fx.util.ClipboardUtils;
import jloda.fx.util.ProgramProperties;
import jloda.util.FileUtils;
import jloda.util.StringUtils;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jloda.fx.util.ClipboardUtils.isImageFile;
import static jloda.fx.util.ClipboardUtils.isTextFile;

/**
 * setup draw pane is import drag-and-drop target
 * Daniel Huson, 11.2025
 */
public class SetupImport {
	public static void apply(Pane pane, MenuItem pasteMenuItem, BiConsumer<String, String> fileNameContentConsumer, Consumer<Image> imageConsumer) {
		var dragOver = new SimpleBooleanProperty(false);
		var isDesktop = new SimpleBooleanProperty(ProgramProperties.isDesktop());

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

			if (db.getString() != null && !db.getString().isBlank() && !db.getString().startsWith("file:") && fileNameContentConsumer != null) {
				fileNameContentConsumer.accept(null, db.getString());
				success = true;
			} else if (db.hasFiles()) {
				var buf = new StringBuilder();
				var name = "";
				for (var file : db.getFiles()) {
					if (FileUtils.fileExistsAndIsNonEmpty(file)) {
						if (fileNameContentConsumer != null) {
							if (isTextFile(file)) {
								try {
									buf.append(StringUtils.toString(FileUtils.getLinesFromFile(file.getPath()), "\n"));
									success = true;
									if (name != null) {
										if (name.isBlank())
											name = file.getPath();
										else name = null;
									}
								} catch (IOException ignored) {
								}
							}
						}
						if (imageConsumer != null) {
							if (isImageFile(file)) {
								imageConsumer.accept(new Image(file.toURI().toString()));
								success = true;
								break;
							}
						}
					}
				}
				if (!buf.isEmpty() && fileNameContentConsumer != null)
					fileNameContentConsumer.accept(name, buf.toString());
			}
			e.setDropCompleted(success);
			e.consume();
		});
		pane.setOnDragExited(event -> {
			dragOver.set(false);
			event.consume();
		});

		pasteMenuItem.setOnAction(e -> {
			if (ClipboardUtils.hasFiles()) {
				for (var file : ClipboardUtils.getFiles()) {
					if (FileUtils.fileExistsAndIsNonEmpty(file)) {
						if (fileNameContentConsumer != null) {
							if (isTextFile(file)) {
								try {
									fileNameContentConsumer.accept(file.getPath(), StringUtils.toString(FileUtils.getLinesFromFile(file.getPath()), "\n"));
									break;
								} catch (IOException ignored) {
								}
							}
						}
						if (imageConsumer != null) {
							if (isImageFile(file)) {
								var uri = file.toURI().toString();
								var image = new Image(uri);
								imageConsumer.accept(image);
								return;
							}
						}
					}
				}
			}
			if (imageConsumer != null && ClipboardUtils.hasImage())
				imageConsumer.accept(ClipboardUtils.getImage());
			else if (fileNameContentConsumer != null && ClipboardUtils.hasString())
				fileNameContentConsumer.accept(null, ClipboardUtils.getString());

		});
	}
}

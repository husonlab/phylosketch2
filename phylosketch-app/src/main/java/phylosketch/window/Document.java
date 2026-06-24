/*
 *  Document.java Copyright (C) 2026 Daniel H. Huson
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Document {
    private final BooleanProperty nameAuto = new SimpleBooleanProperty(this, "nameAuto", true);
    private final StringProperty name = new SimpleStringProperty(this, "name", "Untitled");
    private final StringProperty fileName = new SimpleStringProperty(this, "fileName", "untitled");
    private final BooleanProperty dirty = new SimpleBooleanProperty(this, "dirty", false);
    private final BooleanProperty empty = new SimpleBooleanProperty(this, "empty", true);

    /**
     * constructor
     */
    public Document() {
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public BooleanProperty emptyProperty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty.set(empty);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public boolean isNameAuto() {
        return nameAuto.get();
    }

    public BooleanProperty nameAutoProperty() {
        return nameAuto;
    }

    public void setNameAuto(boolean nameAuto) {
        this.nameAuto.set(nameAuto);
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }
}

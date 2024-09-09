/*
 * ChangeNodeLabelsCommand.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.view;

import jloda.fx.undo.UndoableRedoableCommand;

import java.util.List;

/**
 * change node labels command
 * Daniel Huson, 2.2020
 */
public class ChangeNodeLabelsCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public ChangeNodeLabelsCommand(DrawPane drawPane, List<Data> dataList) {
        super("Label");

        undo = () -> dataList.forEach(data -> drawPane.setLabel(data.getNodeId(),data.getOldLabel()));
        redo = () -> dataList.forEach(data ->drawPane.setLabel(data.getNodeId(),data.getNewLabel()));
    }

    @Override
    public void undo() {
        undo.run();
    }

    @Override
    public void redo() {
        redo.run();
    }

    public static class Data {
        private final int nodeId;
        private final String oldLabel;
        private final String newLabel;


        public Data(int nodeId, String oldLabel, String newLabel) {
            this.nodeId = nodeId;
            this.oldLabel = oldLabel;
            this.newLabel = newLabel;
        }

        public int getNodeId() {
            return nodeId;
        }

        public String getOldLabel() {
            return oldLabel;
        }

        public String getNewLabel() {
            return newLabel;
        }
    }
}

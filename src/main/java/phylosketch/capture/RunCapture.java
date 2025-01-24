/*
 * RunCapture.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.capture;

import jloda.fx.selection.SelectionModel;
import jloda.fx.selection.SetSelectionModel;
import net.sourceforge.tess4j.Word;
import phylocap.capture.CaptureService;
import phylocap.capture.DrawUtils;
import phylocap.capture.Segment;
import phylosketch.view.DrawPane;
import phylosketch.window.MainWindowController;

import java.util.ArrayList;

public class RunCapture {
	private final CapturePane capturePane;
	private final CaptureService service = new CaptureService();
	private final SelectionModel<Word> wordSelection = new SetSelectionModel<>();
	private final SelectionModel<Segment> pathSelection = new SetSelectionModel<>();


	public RunCapture(MainWindowController controller, DrawPane view, CapturePane capturePane) {
		this.capturePane = capturePane;
		service.setProgressParentPane(controller.getBottomFlowPane());


		service.setOnSucceeded(e -> {
			var words = new ArrayList<Word>();
			var segments = new ArrayList<Segment>();
			if (service.getPhase() >= CaptureService.WORDS) {
				words.addAll(capturePane.transformWords(service.getWords()));
				DrawUtils.createWordShapes(words, wordSelection, () -> true, capturePane.getWordsGroup());
			}
			if (service.getPhase() >= CaptureService.SEGMENTS) {
				segments.addAll(capturePane.transformSegments(service.getDustedSegments()));
				DrawUtils.createPaths(segments, pathSelection, () -> true, capturePane.getPathsGroup());
			}
			if (service.getPhase() == CaptureService.PHYLOGENY) {
				var rootLocation = capturePane.getWordsGroup().screenToLocal(capturePane.getRootLocationScreenCoordinates());
				ExtractPhylogeny.apply(view, rootLocation, segments, words);
			}
		});
	}

	public CaptureService getService() {
		return service;
	}
}

/*
 * AccordionManager.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.format;

import javafx.application.Platform;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * manage the number of the accordions that are open
 * Daniel Huson, 11.2024
 */
public class AccordionManager {

	public static void apply(Pane parentPane, Collection<Accordion> accordions) {


		var openAccordions = new LinkedList<Accordion>();
		for (var accordion : accordions) {
			accordion.expandedPaneProperty().addListener((v, o, n) -> handleAccordionState(parentPane, accordion, n, openAccordions));
		}
		parentPane.requestLayout();

		Platform.runLater(() -> {
					parentPane.getScene().heightProperty().addListener((v, o, n) -> {
						if (n.doubleValue() < o.doubleValue()) {
							for (var accordion : accordions) {
								accordion.setExpandedPane(null);
							}
						}
						Platform.runLater(() -> {
							for (var accordion : accordions) {
								handleAccordionState(parentPane, accordion, accordion.getExpandedPane(), openAccordions);
							}
						});
					});
				}
		);
	}

	private static void handleAccordionState(Pane parentPane, Accordion accordion, TitledPane newPane, Queue<Accordion> openAccordions) {
		var maxOpenAccordions = (parentPane.getScene() == null ? 1 : Math.max(1,
				(int) Math.ceil(Math.min(9, (parentPane.getScene().getHeight() - 200) / 250.0))));

		if (newPane != null) {
			if (!openAccordions.contains(accordion)) {
				openAccordions.add(accordion);
			}
			if (openAccordions.size() > maxOpenAccordions) {
				var oldestAccordion = openAccordions.poll();
				if (oldestAccordion != null) {
					oldestAccordion.setExpandedPane(null);
				}
			}
		} else {
			openAccordions.remove(accordion);
		}
		parentPane.requestLayout();
	}
}
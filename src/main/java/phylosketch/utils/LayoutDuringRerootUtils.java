/*
 * LayoutDuringRerootUtils.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import jloda.graph.algorithms.ConnectedComponents;
import jloda.util.Pair;
import phylosketch.commands.MoveNodesCommand;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * code for updating layout during rerooting
 * Daniel Huson, 11.2024
 */
public class LayoutDuringRerootUtils {
	/**
	 * compute the alpha and beta values for the current layout
	 * Alpha is the max angle of a node, as viewed from the reference between 0 and 180 in positive direction
	 * and beta is the max angle in negative direction (between 0 and 180)
	 *
	 * @param view      the window
	 * @param reference the reference node (usually the root of a component)
	 * @return alpha and beta for reference node
	 */
	public static Pair<Double, Double> computeAlphaBeta(DrawView view, Node reference) {
		var component = ConnectedComponents.component(reference);
		var rootLocation = RootPosition.compute(component);
		return computeAlphaBeta(view, reference, component, rootLocation);
	}

	/**
	 * compute the alpha and beta values for the reference node
	 * Alpha is the max angle of a node, as viewed from the reference between 0 and 180 in positive direction
	 * and beta is the max angle in negative direction (between 0 and 180)
	 *
	 * @param reference the reference node (usually the root of a component)
	 * @return alpha and beta for reference node
	 */
	public static Pair<Double, Double> computeAlphaBeta(DrawView view, Node reference, Set<Node> component, RootPosition rootLocation) {
		var delta = switch (rootLocation.side()) {
			case Left -> 0.0;
			case Right -> 180;
			case Top -> 90;
			case Bottom -> 270;
			case Center -> 360;
		};
		var ref = DrawView.getPoint(reference);
		var alpha = 0.0;
		var beta = 0.0;
		for (var v : component) {
			var position = DrawView.getPoint(v);
			var angle = GeometryUtilsFX.modulo360(GeometryUtilsFX.computeAngle(position.subtract(ref)) + delta);
			if (angle >= 0 && angle <= 180) {
				alpha = Math.max(alpha, angle);
			} else {
				beta = Math.max(beta, 360 - angle);
			}
		}
		return new Pair<>(alpha, beta);
	}

	public static void apply(DrawView view, Node reference, double newAlpha, double newBeta, boolean handleDegree2Case, boolean animate) {
		var component = ConnectedComponents.component(reference);
		var rootLocation = RootPosition.compute(component);
		var pair = computeAlphaBeta(view, reference, component, rootLocation);
		var alpha = pair.getFirst();
		var beta = pair.getSecond();
		var delta = switch (rootLocation.side()) {
			case Left -> 0.0;
			case Right -> 180;
			case Top -> 90;
			case Bottom -> 270;
			case Center -> 360;
		};

		var alpha1 = Math.max(1, alpha / beta) * newAlpha;
		var beta1 = Math.max(1, beta / alpha) * newBeta;

		var a = alpha1 / alpha;
		var b = beta1 / beta;

		var refPoint = DrawView.getPoint(reference);
		if (handleDegree2Case && reference.getDegree() == 2) {
			for (var v : reference.adjacentNodes()) {
				var position = DrawView.getPoint(v);
				var angle = GeometryUtilsFX.modulo360(GeometryUtilsFX.computeAngle(position.subtract(refPoint)) + delta);
				if (angle > 0 && angle <= 180) {
					a = Math.min(a, 90 / angle);
				} else if (angle > 180 && angle < 360) {
					b = Math.min(b, 90 / (360 - angle));
				}
			}
		}


		var keyValues = new ArrayList<KeyValue>();
		for (var v : component) {
			var position = DrawView.getPoint(v);
			var angle = GeometryUtilsFX.modulo360(GeometryUtilsFX.computeAngle(position.subtract(refPoint)) + delta);
			Point2D newPosition;
			if (angle >= 0 && angle <= 180) {
				var rotateBack = GeometryUtilsFX.rotateAbout(position, -angle, refPoint);
				newPosition = GeometryUtilsFX.rotateAbout(rotateBack, a * angle, refPoint);
			} else {
				var rotateBack = GeometryUtilsFX.rotateAbout(position, -angle, refPoint);
				newPosition = GeometryUtilsFX.rotateAbout(rotateBack, 360 - b * (alpha - 360), refPoint);
			}
			if (!newPosition.equals(position)) {
				var shape = DrawView.getShape(v);
				var x = new SimpleDoubleProperty(shape.getTranslateX());
				x.addListener((var, o, n) -> MoveNodesCommand.moveNodesAndEdges(view, List.of(v), n.doubleValue() - o.doubleValue(), 0, false));
				var y = new SimpleDoubleProperty(shape.getTranslateY());
				y.addListener((var, o, n) -> MoveNodesCommand.moveNodesAndEdges(view, List.of(v), 0, n.doubleValue() - o.doubleValue(), false));

				keyValues.add(new KeyValue(x, newPosition.getX()));
				keyValues.add(new KeyValue(y, newPosition.getY()));
			}
		}
		var timeline = new Timeline(new KeyFrame(animate ? Duration.seconds(0.7) : Duration.seconds(0), keyValues.toArray(new KeyValue[0])));
		timeline.setCycleCount(1);
		timeline.play();
	}
}

/*
 * EdgePath.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.paths;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.shape.*;
import jloda.fx.util.GeometryUtilsFX;
import jloda.util.CollectionUtils;
import phylosketch.utils.QuadraticCurve;
import phylosketch.view.CirclePathDiscretizer;

import java.util.ArrayList;
import java.util.List;

/**
 * a path representing an edge
 * Daniel Huson, 11.2025
 */
public class EdgePath extends Path {

	public enum Type {Straight, Rectangular, QuadCurve, Circular, Freeform}

	private final ObjectProperty<Type> type = new SimpleObjectProperty<Type>(this, "type", Type.Freeform);

	public EdgePath() {
		getStyleClass().add("graph-edge");
		// if the path gets changed outside of this class, the edge is set to type freeform
		getElements().addListener((InvalidationListener) e -> setType(Type.Freeform));
	}

	public EdgePath(Path path) {
		this();
		getElements().addAll(path.getElements());
		if (path.getElements().size() == 2 && path.getElements().get(0) instanceof MoveTo && path.getElements().get(1) instanceof LineTo)
			setType(Type.Straight);
		else if (path.getElements().size() == 2 && path.getElements().get(0) instanceof MoveTo && path.getElements().get(1) instanceof QuadCurveTo)
			setType(Type.QuadCurve);
		else if (path.getElements().size() == 3 && path.getElements().get(0) instanceof MoveTo && path.getElements().get(1) instanceof LineTo && path.getElements().get(2) instanceof LineTo)
			setType(Type.Rectangular);
		else if (path.getElements().size() == 3 && path.getElements().get(0) instanceof MoveTo && path.getElements().get(1) instanceof ArcTo && path.getElements().get(2) instanceof LineTo)
			setType(Type.Circular);
		else setType(Type.Freeform);
	}

	public EdgePath(Point2D start, Point2D end) {
		var result = new EdgePath();
		result.setStraight(start, end);
	}

	public EdgePath copy() {
		var result = new EdgePath();
		result.getElements().addAll(getElements());
		result.setType(getType());
		return result;
	}

	public EdgePath copyToFreeform() {
		var that = this.copy();
		that.changeToFreeform();
		return that;
	}

	public void set(List<PathElement> elements, Type type) {
		getElements().setAll(elements);
		setType(type);
	}

	public void setStraight(double startX, double startY, double endX, double endY) {
		getElements().setAll(new MoveTo(startX, startY), new LineTo(endX, endY));
		setType(Type.Straight);
	}

	public void setStraight(Point2D start, Point2D end) {
		setStraight(start.getX(), start.getY(), end.getX(), end.getY());
	}

	public void setStraight() {
		if (getElements().size() >= 2) {
			setStraight(PathUtils.getCoordinates(getElements().get(0)), PathUtils.getCoordinates(getElements().get(getElements().size() - 1)));
		} else
			throw new IllegalStateException("Broken path");
	}

	public void setRectangular(double startX, double startY, double midX, double midY, double endX, double endY) {
		getElements().setAll(new MoveTo(startX, startY), new LineTo(midX, midY), new LineTo(endX, endY));
		setType(Type.Rectangular);
	}

	public void setRectangular(Point2D start, Point2D mid, Point2D end) {
		setRectangular(start.getX(), start.getY(), mid.getX(), mid.getY(), end.getX(), end.getY());
	}

	public void setQuadCurve(double startX, double startY, double midX, double midY, double endX, double endY) {
		getElements().setAll(new MoveTo(startX, startY), new QuadCurveTo(midX, midY, endX, endY));
		setType(Type.QuadCurve);
	}

	public void setQuadCurve(Point2D start, Point2D mid, Point2D end) {
		setQuadCurve(start.getX(), start.getY(), mid.getX(), mid.getY(), end.getX(), end.getY());
	}

	public void setFreeform(List<Point2D> points) {
		getElements().setAll(PathUtils.createElements(points, true));
		setType(Type.Freeform);
	}

	public void setCircular(Point2D sourceNode, Point2D center, Point2D targetNode) {
		getElements().clear();

		getElements().add(new MoveTo(sourceNode.getX(), sourceNode.getY()));

		if (sourceNode.magnitude() > 0 && targetNode.magnitude() > 0) {
			var sourceDir = sourceNode.subtract(center);
			var targetDir = targetNode.subtract(center);
			var corner = center.add(targetDir.multiply(sourceDir.magnitude() / targetDir.magnitude()));

			var arcTo = new ArcTo();
			arcTo.setX(corner.getX());
			arcTo.setY(corner.getY());
			arcTo.setRadiusX(sourceDir.magnitude());
			arcTo.setRadiusY(sourceDir.magnitude());
			arcTo.setLargeArcFlag(GeometryUtilsFX.computeObservedAngle(center, sourceNode, targetNode) > 180);
			arcTo.setSweepFlag(GeometryUtilsFX.computeObservedAngle(center, sourceNode, targetNode) > 0);

			getElements().add(arcTo);
		}
		getElements().add(new LineTo(targetNode.getX(), targetNode.getY()));
		setType(Type.Circular);
	}

	public void changeToFreeform() {
		if (getType() == Type.Straight && getElements().size() == 2) {
			var a = PathUtils.getCoordinates(getElements().get(0));
			var b = PathUtils.getCoordinates(getElements().get(1));
			getElements().setAll(PathUtils.createElements(List.of(a, b), true));
		} else if (getType() == Type.Rectangular && getElements().size() == 3) {
			var a = PathUtils.getCoordinates(getElements().get(0));
			var b = PathUtils.getCoordinates(getElements().get(1));
			var c = PathUtils.getCoordinates(getElements().get(2));
			getElements().setAll(PathUtils.createElements(List.of(a, b, c), true));

		} else if (getType() == Type.QuadCurve && getElements().size() == 2 && getElements().get(1) instanceof QuadCurveTo quadCurveTo) {
			var a = PathUtils.getCoordinates(getElements().get(0));
			var b = new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY());
			var c = new Point2D(quadCurveTo.getX(), quadCurveTo.getY());
			getElements().setAll(PathUtils.createElements(QuadraticCurve.apply(a, b, c), true));
		} else if (getType() == Type.Circular && getElements().size() == 3 && getElements().get(0) instanceof MoveTo
				   && getElements().get(1) instanceof ArcTo && getElements().get(2) instanceof LineTo) {
			getElements().setAll(CirclePathDiscretizer.discretize(this, 5).getElements());
		} else if (getType() == Type.Freeform) { // converts into proper freeform
			var elements = new ArrayList<PathElement>();
			for (var e : getElements()) {
				if (e instanceof MoveTo || e instanceof LineTo) {
					elements.add(e);
				} else if (e instanceof QuadCurveTo quadCurveTo) {
					var a = PathUtils.getCoordinates(elements.get(elements.size() - 1));
					var b = new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY());
					var c = new Point2D(quadCurveTo.getX(), quadCurveTo.getY());
					getElements().setAll(PathUtils.createElements(QuadraticCurve.apply(a, b, c), true));
				} else if (e instanceof ArcTo arcTo) {
					var prev = PathUtils.getCoordinates(elements.get(elements.size() - 1));
					var path = new Path(List.of(new MoveTo(prev.getX(), prev.getY()), arcTo));
					getElements().setAll(CirclePathDiscretizer.discretize(path, 5).getElements());
				}
			}
		} else {
			throw new IllegalStateException("Broken path");
		}
		setType(Type.Freeform);
	}

	public EdgePath reverse() {
		switch (getType()) {
			case Straight -> {
				var a = PathUtils.getCoordinates(getElements().get(0));
				var b = PathUtils.getCoordinates(getElements().get(1));
				var path = new EdgePath();
				path.setStraight(a, b);
				return path;
			}
			case Rectangular -> {
				var a = PathUtils.getCoordinates(getElements().get(0));
				var b = PathUtils.getCoordinates(getElements().get(1));
				var c = PathUtils.getCoordinates(getElements().get(2));
				var path = new EdgePath();
				path.setRectangular(a, b, c);
				return path;
			}
			default -> {
				var points = PathUtils.getPoints(copyToFreeform());
				points = CollectionUtils.reverse(points);
				var path = new EdgePath();
				path.setFreeform(points);
				return path;
			}
		}
	}


	/**
	 * gets a point near the middle of the edge (for label positioning)
	 *
	 * @return middle point
	 */
	public Point2D getMiddle() {
		switch (getType()) {
			case Straight -> {
				if (getElements().size() == 2) {
					var a = PathUtils.getCoordinates(getElements().get(0));
					var b = PathUtils.getCoordinates(getElements().get(1));
					return (a.add(b)).multiply(0.5);
				}
			}
			case Rectangular -> {
				if (getElements().size() == 3) {
					var a = PathUtils.getCoordinates(getElements().get(1));
					var b = PathUtils.getCoordinates(getElements().get(2));
					return (a.add(b)).multiply(0.5);
				}
			}
			case QuadCurve -> {
				if (getElements().size() == 2 && getElements().get(0) instanceof MoveTo moveTo && getElements().get(1) instanceof QuadCurveTo quadCurveTo) {
					var a = PathUtils.getCoordinates(moveTo);
					var control = new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY());
					var b = new Point2D(quadCurveTo.getX(), quadCurveTo.getY());
					var points = QuadraticCurve.apply(a, control, b);
					return points.get(points.size() / 2);
				}
			}
			case Circular -> {
				if (true) {
					if (getElements().size() == 3 && getElements().get(0) instanceof MoveTo && getElements().get(1) instanceof ArcTo arcTo && getElements().get(2) instanceof LineTo lineTo)
						return (PathUtils.getCoordinates(arcTo).add(PathUtils.getCoordinates(lineTo))).multiply(0.5);
				} else {
					var tmp = new EdgePath();
					tmp.getElements().addAll(getElements());
					tmp.setType(Type.Circular);
					tmp.changeToFreeform();
					return PathUtils.getCoordinates(tmp.getElements().get(tmp.getElements().size() / 2));
				}
			}
			case Freeform -> {
				if (!getElements().isEmpty()) {
					if (getElements().size() == 2) {
						var a = getElements().get(0);
						var b = getElements().get(1);
						return (PathUtils.getCoordinates(a).add(PathUtils.getCoordinates(b))).multiply(0.5);
					} else
						return PathUtils.getCoordinates(getElements().get(getElements().size() / 2));
				}
			}
		}
		throw new IllegalStateException("Broken path");
	}

	public EdgePath rotate(Point2D center, double angle) {
		var path = PathTransforms.rotate(this, center.getX(), center.getY(), angle);
		var result = new EdgePath(path);
		result.setType(getType());
		return result;
	}

	public EdgePath flip(Point2D center, boolean horizontally) {
		var path = (horizontally ? PathTransforms.flipHorizontal(this, center.getX()) : PathTransforms.flipVertical(this, center.getY()));
		var result = new EdgePath(path);
		result.setType(getType());
		return result;
	}

	/**
	 * is point given in local coordinates of the path on the stroke, using default tolerance of 3
	 *
	 * @param localX local x
	 * @param localY local y
	 * @return true, if on stroke rather than just in encompassed region
	 */
	public boolean isPointOnStroke(double localX, double localY) {
		return PathHitTestUtils.isPointOnStroke(this, localX, localY);
	}

	/**
	 * is point given in local coordinates of the path on the stroke
	 *
	 * @param localX    local x
	 * @param localY    local y
	 * @param tolerance how close do we need to be?
	 * @return true, if on stroke rather than just in encompassed region
	 */
	public boolean isPointOnStroke(double localX, double localY, double tolerance) {
		return PathHitTestUtils.isPointOnStroke(this, localX, localY, tolerance);
	}

	public ObjectProperty<Type> typeProperty() {
		return type;
	}

	public Type getType() {
		return type.get();
	}

	private void setType(Type type) {
		this.type.set(type);
	}


	public record Data(List<PathElement> elements, EdgePath.Type type) {
	}

}

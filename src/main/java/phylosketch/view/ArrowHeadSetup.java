package phylosketch.view;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurveTo;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Edge;
import phylosketch.paths.EdgePath;
import phylosketch.paths.PathUtils;

public class ArrowHeadSetup {
	public static Polygon apply(Edge e, EdgePath path) {
		var arrowHead = new Polygon(7.0, 0.0, -7.0, 4.0, -7.0, -4.0);
		arrowHead.getStyleClass().add("graph-node");

		arrowHead.strokeProperty().bind(path.strokeProperty());
		arrowHead.fillProperty().bind(arrowHead.strokeProperty());
		arrowHead.setOnMouseClicked(path.getOnMouseClicked());

		InvalidationListener listener = a -> {
			var lastId = path.getElements().size() - 1;
			if (lastId > 0) {
				var last = PathUtils.getCoordinates(path.getElements().get(lastId));

				var firstId = lastId;
				Point2D first = null;
				while (firstId >= 0) {
					var element = path.getElements().get(firstId);
					if (element instanceof QuadCurveTo quadCurveTo) {
						first = new Point2D(quadCurveTo.getControlX(), quadCurveTo.getControlY());
					} else if (firstId < lastId) {
						first = PathUtils.getCoordinates(element);
					}
					if (first != null && last.distance(first) > 8) {
						break;
					}
					firstId--;
				}
				if (first == null)
					first = new Point2D(0, 0); // should never happen

				var f = Math.max(0.01, Math.min(2, path.getStrokeWidth() / 2));
				arrowHead.getPoints().setAll(7.0 * f, 0.0 * f, -7.0 * f, 4.0 * f, -7.0 * f, -4.0 * f);

				var direction = last.subtract(first);
				direction = direction.multiply(1.0 / direction.magnitude());
				arrowHead.setRotate(GeometryUtilsFX.computeAngle(direction));
				arrowHead.setTranslateX(last.getX() - f * 12 * direction.getX());
				arrowHead.setTranslateY(last.getY() - f * 12 * direction.getY());
			}
		};
		listener.invalidated(null);
		arrowHead.setUserData(listener);
		path.strokeWidthProperty().addListener(new WeakInvalidationListener(listener));
		arrowHead.setOnMouseClicked(path.getOnMouseClicked());
		path.getElements().addListener(new WeakInvalidationListener(listener));
		DrawView.getShape(e.getTarget()).translateXProperty().addListener(new WeakInvalidationListener(listener));
		DrawView.getShape(e.getTarget()).translateYProperty().addListener(new WeakInvalidationListener(listener));
		return arrowHead;
	}
}

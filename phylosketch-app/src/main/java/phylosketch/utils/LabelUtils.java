package phylosketch.utils;

import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import phylosketch.view.DrawView;
import phylosketch.view.RootPosition;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LabelUtils {
	public static List<String> getInOrder(RootPosition rootPosition, Collection<Node> selectedNodes) {
		Comparator<Node> order = switch (rootPosition.side()) {
			case Left, Right -> Comparator.comparingDouble(DrawView::getY);   // top to bottom
			case Top, Bottom -> Comparator.comparingDouble(DrawView::getX);   // left to right
			case Center -> Comparator.comparingDouble(v ->               // around the root
					GeometryUtilsFX.computeAngle(DrawView.getPoint(v).subtract(rootPosition.location())));
		};

		return selectedNodes.stream().sorted(order).map(v -> {
					var label = DrawView.getLabel(v);
					return label == null ? null : label.getRawText();
				})
				.filter(Objects::nonNull).filter(a -> !((String) a).isBlank()).toList();
	}
}

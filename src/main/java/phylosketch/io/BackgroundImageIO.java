/*
 * BackgroundImageIO.java Copyright (C) 2025 Daniel H. Huson
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

package phylosketch.io;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import jloda.fx.thirdparty.PngEncoderFX;

import java.io.*;
import java.util.Base64;

/**
 * IO for background image
 * Daniel Huson, 3.2025
 */
public class BackgroundImageIO {
	/**
	 * save the background image
	 *
	 * @param imageView imageview containing the image
	 * @param w         the writer
	 * @throws IOException write failed
	 */
	public static void save(ImageView imageView, Writer w) throws IOException {
		if (imageView.getImage() != null) {
			var bytes = PngEncoderFX.encode(imageView.getImage());
			var encoded = Base64.getEncoder().encodeToString(bytes);
			w.write("[Image: %.0f %.0f %.0f %.0f %s]\n".formatted(imageView.getX(), imageView.getY(), imageView.getFitWidth(), imageView.getFitHeight(), encoded));
		}
	}

	/**
	 * load the background image
	 *
	 * @param imageView imageview containing the image
	 * @param r         the reader
	 * @throws IOException read failed
	 */
	public static boolean load(ImageView imageView, Reader r) throws IOException {
		var bf = (r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r));
		String line = "";
		while (r.ready()) {
			line = bf.readLine();
			if (line == null)
				return false;
			if (!line.isBlank()) {
				break;
			}
		}
		if (line.startsWith("[Image: ") && line.endsWith("]")) {
			line = line.substring("[Image: ".length(), line.length() - 1);
			var tokens = line.split(" ");
			var x = Integer.parseInt(tokens[0]);
			var y = Integer.parseInt(tokens[1]);
			var width = Integer.parseInt(tokens[2]);
			var height = Integer.parseInt(tokens[3]);
			var bytes = Base64.getDecoder().decode(tokens[4]);
			var image = new Image(new ByteArrayInputStream(bytes));
			imageView.setImage(image);
			imageView.setX(x);
			imageView.setY(y);
			imageView.setFitWidth(width);
			imageView.setFitHeight(height);

			return true;
		}
		return false;
	}
}

/*
 * PngWriter.java Copyright (C) 2026 Daniel H. Huson
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

package phylosketch.ocr.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * minimal PNG writer for JavaFX images, written from the PNG specification.
 * Produces an 8-bit RGBA (colour type 6) image with filter type None on every scanline,
 * compressed with a zlib stream at the highest compression level.
 * Daniel Huson, 11.2026
 */
public class PngWriter {
	private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	private static final int MAX_IDAT_CHUNK_SIZE = 1 << 20;

	/**
	 * encodes an image as a PNG
	 *
	 * @param image the image
	 * @return the PNG data
	 * @throws IllegalArgumentException if the image has no pixel reader (e.g. still loading or failed to load)
	 */
	public static byte[] encode(Image image) {
		var buffer = new ByteArrayOutputStream();
		try {
			write(image, buffer);
		} catch (IOException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		return buffer.toByteArray();
	}

	/**
	 * writes an image to an output stream in PNG format
	 *
	 * @param image the image
	 * @param out   the output stream
	 * @throws IOException if the image has no pixel reader or writing fails
	 */
	public static void write(Image image, OutputStream out) throws IOException {
		if (image == null)
			throw new IOException("Cannot write PNG: image is null");
		var reader = image.getPixelReader();
		if (reader == null)
			throw new IOException("Cannot write PNG: image has no pixel reader (image not loaded or in error state)");

		var width = (int) Math.round(image.getWidth());
		var height = (int) Math.round(image.getHeight());
		if (width <= 0 || height <= 0)
			throw new IOException("Cannot write PNG: invalid image dimensions " + width + "x" + height);

		out.write(SIGNATURE);

		// IHDR: width, height, bit depth 8, colour type 6 (truecolour with alpha),
		// compression method 0, filter method 0, interlace method 0
		var ihdr = new byte[13];
		putInt(ihdr, 0, width);
		putInt(ihdr, 4, height);
		ihdr[8] = 8;
		ihdr[9] = 6;
		ihdr[10] = 0;
		ihdr[11] = 0;
		ihdr[12] = 0;
		writeChunk(out, "IHDR", ihdr, 0, ihdr.length);

		// IDAT: zlib-compressed scanlines, each prefixed by filter type 0 (None), pixels as RGBA
		var deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try {
			var bgra = new byte[width * 4];
			var scanline = new byte[1 + width * 4];
			var compressed = new byte[MAX_IDAT_CHUNK_SIZE];
			var filled = 0;
			var format = PixelFormat.getByteBgraInstance();

			for (var y = 0; y < height; y++) {
				reader.getPixels(0, y, width, 1, format, bgra, 0, width * 4);
				scanline[0] = 0;
				for (var x = 0; x < width; x++) {
					var src = 4 * x;
					var dst = 1 + 4 * x;
					scanline[dst] = bgra[src + 2];     // R
					scanline[dst + 1] = bgra[src + 1]; // G
					scanline[dst + 2] = bgra[src];     // B
					scanline[dst + 3] = bgra[src + 3]; // A
				}
				deflater.setInput(scanline, 0, scanline.length);
				while (!deflater.needsInput()) {
					filled += deflater.deflate(compressed, filled, compressed.length - filled);
					if (filled == compressed.length) {
						writeChunk(out, "IDAT", compressed, 0, filled);
						filled = 0;
					}
				}
			}
			deflater.finish();
			while (!deflater.finished()) {
				filled += deflater.deflate(compressed, filled, compressed.length - filled);
				if (filled == compressed.length) {
					writeChunk(out, "IDAT", compressed, 0, filled);
					filled = 0;
				}
			}
			if (filled > 0)
				writeChunk(out, "IDAT", compressed, 0, filled);
		} finally {
			deflater.end();
		}

		writeChunk(out, "IEND", new byte[0], 0, 0);
		out.flush();
	}

	/**
	 * writes one chunk: 4-byte length, 4-byte type, data, 4-byte CRC over type and data
	 */
	private static void writeChunk(OutputStream out, String type, byte[] data, int offset, int length) throws IOException {
		var typeBytes = type.getBytes(StandardCharsets.US_ASCII);
		var lengthBytes = new byte[4];
		putInt(lengthBytes, 0, length);
		out.write(lengthBytes);
		out.write(typeBytes);
		out.write(data, offset, length);
		var crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data, offset, length);
		var crcBytes = new byte[4];
		putInt(crcBytes, 0, (int) crc.getValue());
		out.write(crcBytes);
	}

	/**
	 * writes a big-endian 32-bit integer
	 */
	private static void putInt(byte[] buffer, int offset, int value) {
		buffer[offset] = (byte) (value >>> 24);
		buffer[offset + 1] = (byte) (value >>> 16);
		buffer[offset + 2] = (byte) (value >>> 8);
		buffer[offset + 3] = (byte) value;
	}
}

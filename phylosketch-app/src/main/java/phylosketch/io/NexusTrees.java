/*
 * NexusTrees.java Copyright (C) 2026 Daniel H. Huson
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

import jloda.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * reads the trees of a Nexus file: the tree statements of all trees blocks, as Newick strings in which the
 * taxon labels of the translate statement have been put back
 * Daniel Huson, 11.2026
 */
public class NexusTrees {
	private static final Pattern BEGIN_TREES_LINE = Pattern.compile("(?i)\\bbegin\\s+trees\\s*;");
	private static final Pattern BEGIN_TREES = Pattern.compile("(?i)begin\\s+trees");
	private static final Pattern END = Pattern.compile("(?i)end(block)?");
	private static final Pattern TRANSLATE = Pattern.compile("(?is)translate\\s.*");
	private static final Pattern TREE = Pattern.compile("(?is)u?tree\\s.*");

	/**
	 * does the file contain a trees block? Only looks for the line that begins the block
	 */
	public static boolean hasTreesBlock(File file) {
		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(file.getPath()))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (BEGIN_TREES_LINE.matcher(line).find())
					return true;
			}
		} catch (IOException ignored) {
		}
		return false;
	}

	/**
	 * extracts all trees from a Nexus file
	 *
	 * @param fileName the file, may be zipped or gzipped
	 * @return the trees in Newick format, each on one line and ending on a semicolon
	 */
	public static List<String> extract(String fileName) throws IOException {
		try (var r = new BufferedReader(FileUtils.getReaderPossiblyZIPorGZIP(fileName))) {
			return extract(r);
		}
	}

	/**
	 * extracts all trees from Nexus text
	 *
	 * @return the trees in Newick format, each on one line and ending on a semicolon
	 */
	public static List<String> extract(Reader reader) throws IOException {
		var trees = new ArrayList<String>();
		var translate = new HashMap<String, String>();
		var inTrees = false;
		var first = true;

		String statement;
		while ((statement = nextStatement(reader)) != null) {
			statement = skipComments(statement);
			if (first) {
				first = false;
				if (statement.toLowerCase().startsWith("#nexus"))
					statement = skipComments(statement.substring("#nexus".length()));
			}
			if (!inTrees) {
				if (BEGIN_TREES.matcher(statement).matches()) {
					inTrees = true;
					translate.clear();
				}
			} else if (END.matcher(statement).matches()) {
				inTrees = false;
			} else if (TRANSLATE.matcher(statement).matches()) {
				parseTranslate(statement.substring("translate".length()), translate);
			} else if (TREE.matcher(statement).matches()) {
				var pos = statement.indexOf('=');
				if (pos != -1) {
					// what follows the equals sign is the tree, usually after a comment such as [&R]
					var newick = skipComments(statement.substring(pos + 1)).replaceAll("\\s*[\\r\\n]+\\s*", "");
					if (newick.startsWith("("))
						trees.add(applyTranslate(newick, translate) + ";");
				}
			}
		}
		return trees;
	}

	/**
	 * the next statement, without the semicolon that ends it; a semicolon inside a quoted label or a comment does
	 * not end a statement
	 *
	 * @return the statement, or null at the end of the text
	 */
	private static String nextStatement(Reader reader) throws IOException {
		var buf = new StringBuilder();
		var inQuote = false;
		var commentDepth = 0;
		int ch;
		while ((ch = reader.read()) != -1) {
			var c = (char) ch;
			if (commentDepth > 0) {
				if (c == '[')
					commentDepth++;
				else if (c == ']')
					commentDepth--;
			} else if (inQuote) {
				if (c == '\'')
					inQuote = false;
			} else if (c == '[') {
				commentDepth++;
			} else if (c == '\'') {
				inQuote = true;
			} else if (c == ';') {
				return buf.toString();
			}
			buf.append(c);
		}
		return buf.toString().isBlank() ? null : buf.toString();
	}

	/**
	 * removes white space and comments from the start of the text, and white space from its end
	 */
	private static String skipComments(String text) {
		text = text.strip();
		while (text.startsWith("[")) {
			var end = endOfComment(text, 0);
			if (end == -1)
				return "";
			text = text.substring(end).strip();
		}
		return text;
	}

	/**
	 * the position after the comment that starts at the given position, or -1, if the comment does not end
	 */
	private static int endOfComment(String text, int start) {
		var depth = 0;
		for (var i = start; i < text.length(); i++) {
			var c = text.charAt(i);
			if (c == '[')
				depth++;
			else if (c == ']' && --depth == 0)
				return i + 1;
		}
		return -1;
	}

	/**
	 * the position after the quoted label that starts at the given position; two single quotes inside the label
	 * stand for one
	 */
	private static int endOfQuote(String text, int start) {
		var i = start + 1;
		while (i < text.length()) {
			if (text.charAt(i) == '\'') {
				if (i + 1 < text.length() && text.charAt(i + 1) == '\'')
					i++;
				else
					return i + 1;
			}
			i++;
		}
		return text.length();
	}

	/**
	 * parses the pairs of a translate statement: number or short name, followed by the taxon label
	 */
	private static void parseTranslate(String text, Map<String, String> translate) {
		var start = 0;
		var i = 0;
		while (i <= text.length()) {
			if (i == text.length() || text.charAt(i) == ',') {
				var pair = skipComments(text.substring(start, i));
				if (!pair.isEmpty()) {
					var keyEnd = pair.startsWith("'") ? endOfQuote(pair, 0) : endOfWord(pair);
					var key = pair.substring(0, keyEnd);
					var label = pair.substring(keyEnd).strip();
					if (!label.isEmpty())
						translate.put(unquote(key), label);
				}
				start = i + 1;
				i++;
			} else if (text.charAt(i) == '\'') {
				i = endOfQuote(text, i);
			} else if (text.charAt(i) == '[') {
				var end = endOfComment(text, i);
				i = (end == -1 ? text.length() : end);
			} else
				i++;
		}
	}

	private static int endOfWord(String text) {
		var i = 0;
		while (i < text.length() && !Character.isWhitespace(text.charAt(i)))
			i++;
		return i;
	}

	private static String unquote(String label) {
		if (label.length() >= 2 && label.startsWith("'") && label.endsWith("'"))
			return label.substring(1, label.length() - 1).replace("''", "'");
		else
			return label;
	}

	/**
	 * replaces the leaf labels of a Newick string by their translations. A leaf label follows an opening
	 * parenthesis or a comma; labels of internal nodes, which follow a closing parenthesis, are left alone, as they
	 * are usually support values, and so are edge weights and comments
	 */
	private static String applyTranslate(String newick, Map<String, String> translate) {
		if (translate.isEmpty())
			return newick;
		var buf = new StringBuilder();
		var previous = '(';
		var i = 0;
		while (i < newick.length()) {
			var c = newick.charAt(i);
			if (c == '[') {
				var end = endOfComment(newick, i);
				if (end == -1)
					end = newick.length();
				buf.append(newick, i, end);
				i = end;
			} else if (c == '(' || c == ')' || c == ',' || c == ':' || c == ';') {
				buf.append(c);
				previous = c;
				i++;
			} else if (Character.isWhitespace(c)) {
				buf.append(c);
				i++;
			} else {
				int end;
				if (c == '\'')
					end = endOfQuote(newick, i);
				else {
					end = i;
					while (end < newick.length() && "(),:;[".indexOf(newick.charAt(end)) == -1 && !Character.isWhitespace(newick.charAt(end)))
						end++;
				}
				var label = newick.substring(i, end);
				var translation = (previous == '(' || previous == ',') ? translate.get(unquote(label)) : null;
				buf.append(translation != null ? translation : label);
				previous = 'a';
				i = end;
			}
		}
		return buf.toString();
	}
}

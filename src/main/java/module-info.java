module phylosketch {
	requires com.install4j.runtime;

	requires transitive jloda_core;
	requires transitive jloda_fx;
	requires transitive javafx.controls;
    requires transitive javafx.fxml;
	requires java.sql.rowset;
	requires org.apache.commons.collections4;
	requires org.apache.commons.math4.legacy;
	requires java.desktop;
	requires javafx.web;
	requires org.apache.commons.numbers.gamma;
	requires java.management;
	requires java.scripting;

	requires org.bytedeco.tesseract;
	//requires org.bytedeco.opencv;

	opens phylosketch.format;
	opens phylosketch.help;
	opens phylosketch.help.figs;
	opens phylosketch.main;
	opens phylosketch.view;
	opens phylosketch.window;

	exports phylosketch.format;
	exports phylosketch.window;
	exports phylosketch.io;
	exports phylosketch.main;
	exports phylosketch.view;

	opens phylosketch.capturepane.capture;
	opens phylosketch.capturepane.pane;
	exports phylosketch.capturepane.capture;
	exports phylosketch.capturepane.pane;
	exports phylosketch.embed.optimize;
	exports phylosketch.embed;
	exports phylosketch.utils;
	opens phylosketch.utils;

	opens xtra.treesnet;
	exports xtra.treesnet;
}
module phylosketch {
	requires com.install4j.runtime;

	requires transitive jloda_core;
	requires transitive jloda_fx;
	requires transitive javafx.controls;
    requires transitive javafx.fxml;
	requires java.sql.rowset;
	requires org.apache.commons.collections4;

	opens phylosketch.main;
	opens phylosketch.view;
	opens phylosketch.window;

	exports phylosketch.window;
	exports phylosketch.io;
	exports phylosketch.main;
	exports phylosketch.view;
}
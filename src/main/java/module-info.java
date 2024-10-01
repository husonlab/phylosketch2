module phylosketch {
	requires com.install4j.runtime;

	requires transitive jloda_core;
	requires transitive jloda_fx;
	requires transitive javafx.controls;
    requires transitive javafx.fxml;
	requires java.sql.rowset;

	opens phylosketch2.main;
	//opens phylosketch2.view;
	opens phylosketch2.window;

	exports phylosketch2.main;

}
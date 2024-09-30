module catrenet {
	requires com.install4j.runtime;

	requires transitive jloda_core;
	requires transitive jloda_fx;
	requires transitive javafx.controls;
    requires transitive javafx.fxml;
	requires transitive javafx.web;
	requires org.apache.commons.numbers.gamma;

	opens phylosketch.main;
	//opens phylosketch.view;
	opens phylosketch.window;

	exports phylosketch.main;

}
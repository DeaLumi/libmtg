module emi.lib.mtg {
	requires com.google.common;
	requires java.desktop;
	requires javafx.graphics;
	requires javafx.swing;

	exports emi.lib.mtg;
	exports emi.lib.mtg.characteristic;
	exports emi.lib.mtg.characteristic.impl;
	exports emi.lib.mtg.game;
	exports emi.lib.mtg.img;
}
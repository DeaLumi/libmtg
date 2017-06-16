package org.whitefoxy.lib.mtg.characteristic;

import java.util.NoSuchElementException;

/**
 * Created by Emi on 5/6/2016.
 */
public enum Color {
	WHITE ("W", "White"),
	BLUE ("U", "Blue"),
	BLACK ("B", "Black"),
	RED ("R", "Red"),
	GREEN ("G", "Green"),
	COLORLESS ("C", "Colorless"); // SPECIFICALLY colorless. This should NOT be used for generic costs or 'colorless' creatures.

	public final String letter, name;

	Color(String letter, String name) {
		this.letter = letter;
		this.name = name;
	}

	public static Color fromString(String from) {
		for (Color c : Color.values()) {
			if (from.length() == 1 && c.letter.equals(from) || c.name.equals(from)) {
				return c;
			}
		}

		throw new NoSuchElementException(String.format("No MTG color associated with %s", from));
	}
}

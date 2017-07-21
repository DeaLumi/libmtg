package emi.lib.mtg.v2;

import emi.lib.mtg.characteristic.*;

import java.util.EnumSet;
import java.util.UUID;
import java.util.Set;

public interface Card {
	interface Face {
		enum Kind {
			Front,
			Transformed,
			Flipped,
			Left,
			Right,
			Other
		}

		Kind kind();

		String name();

		ManaCost manaCost();

		Set<Color> colorIndicator();

		CardTypeLine type();

		String rules();

		String power();

		String toughness();

		String loyalty();

		String handModifier();

		String lifeModifier();

		// TODO: Derived characteristics (net color, CMC, etc) here

		default Set<Color> color() {
			EnumSet<Color> color = EnumSet.copyOf(colorIndicator());
			color.addAll(manaCost().colors());
			return color;
		}

		default Double convertedManaCost() {
			return manaCost().convertedCost();
		}
	}

	interface Printing {
		String name();
	}

	Set<? extends Face> faces();

	Face face(Face.Kind kind);

	Set<? extends Printing> printings();

	Printing printing(String name);

	UUID id();

}

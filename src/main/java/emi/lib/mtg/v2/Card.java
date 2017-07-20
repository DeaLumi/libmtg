package emi.lib.mtg.v2;

import emi.lib.mtg.characteristic.*;

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

		Void illustration(); // TODO: Define illustration.

		Set<Color> colorIndicator();

		CardTypeLine type();

		Void expansionSymbol(); // TODO: Define expansion symbol

		String rules();

		String power();

		String toughness();

		String loyalty();

		String handModifier();

		String lifeModifier();

		String illustrationCredit(); // TODO: Do we care?

		String legalText(); // TODO: Do we care?

		String collectorNumber(); // TODO: Do we ca-- I mean, uh, is this a part of Card, or Face?

		// TODO: Derived characteristics (net color, CMC, etc) here

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

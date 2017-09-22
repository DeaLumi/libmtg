package emi.lib.mtg.game;

import emi.lib.mtg.Card;

import java.util.*;

public interface Deck {
	interface Variant {
		Deck deck();

		String name();

		String description();

		Collection<? extends Card.Printing> cards(Zone zone);
	}

	String name();

	String author();

	Format format();

	String description();

	Collection<? extends Variant> variants();
}

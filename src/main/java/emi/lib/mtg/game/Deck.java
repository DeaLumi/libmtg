package emi.lib.mtg.game;

import emi.lib.mtg.Card;

import java.util.*;

public interface Deck {
	String name();

	String author();

	Format format();

	String description();

	Collection<? extends Card.Print> cards(Zone zone);

	default Format.Validator.Result validate() {
		return format().validate(this);
	}
}

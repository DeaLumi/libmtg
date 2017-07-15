package emi.lib.mtg.game;

import emi.lib.mtg.card.Card;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Deck {
	String name();

	String author();

	Format format();

	default Set<String> validate() {
		if (format() != null) {
			return format().validate(this);
		} else {
			return Collections.emptySet();
		}
	}

	String description();

	Map<Zone, ? extends List<? extends Card>> cards();

	List<? extends Card> sideboard();
}

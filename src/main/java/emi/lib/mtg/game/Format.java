package emi.lib.mtg.game;

import emi.lib.Service;
import emi.lib.mtg.Card;

import java.util.HashSet;
import java.util.Set;

@Service
@Service.Property.String(name="name")
public interface Format {
	String name();

	Set<Zone> deckZones();

	boolean cardIsLegal(Card card);

	Set<String> validate(Deck.Variant deckVariant);

	default Set<String> validate(Deck deck) {
		Set<String> strings = new HashSet<>();

		for (Deck.Variant variant : deck.variants()) {
			this.validate(variant).stream()
					.map(s -> String.format("%s: %s", variant.name(), s))
					.forEach(strings::add);
		}

		return strings;
	}
}

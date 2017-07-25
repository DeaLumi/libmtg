package emi.lib.mtg.game;

import emi.lib.Service;
import emi.lib.mtg.Card;

import java.util.Set;

@Service
@Service.Property.String(name="name")
public interface Format {
	String name();

	Set<Zone> deckZones();

	boolean cardIsLegal(Card card);

	Set<String> validate(Deck deck);
}

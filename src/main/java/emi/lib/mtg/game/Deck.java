package emi.lib.mtg.game;

import emi.lib.mtg.card.Card;

import java.util.List;
import java.util.Map;

public interface Deck {
	String name();

	String author();

	String description();

	Map<Zone, List<Card>> cards();

	List<Card> sideboard();
}

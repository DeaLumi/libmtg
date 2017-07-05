package emi.lib.mtg.data;

import emi.lib.mtg.card.Card;

import java.util.Collection;

/**
 * Represents a set of Magic: the Gathering cards.
 */
public interface CardSet {
	/**
	 * Returns the name of this card set, in English.
	 *
	 * @return The name of this card set, in English.
	 */
	String name();

	/**
	 * Returns the abbreviated code of this card set, in English. Usually this is only three characters, capitalized.
	 *
	 * @return The abbreviated code of this card set, in English.
	 */
	String code();

	/**
	 * Returns a collection of all cards in this set. They are not necessarily in any specific order.
	 *
	 * @return A collection of all cards in this set.
	 */
	Collection<? extends Card> cards();
}

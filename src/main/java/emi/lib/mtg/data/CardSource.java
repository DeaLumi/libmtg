package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.Card;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A source of information on cards in Magic: the Gathering.
 */
@Service
@Service.Property.String(name="name")
public interface CardSource {
	/**
	 * Returns a set of cards this CardSource can provide information about. Must not be null.
	 *
	 * Provider note: If this set is empty, you don't know of any cards. Make sure you return at least one set.
	 *
	 * @return A set of cards this CardSource can provide information about, or an empty set if it doesn't know of any
	 * sets.
	 */
	Collection<? extends CardSet> sets();

	/**
	 * Get a card this card source knows about by UUID.
	 *
	 * @param id The UUID to fetch.
	 * @return The Card associated with that UUID (i.e. get(id).id().equals(id) is true), or null if no card could be
	 * found.
	 */
	Card get(UUID id);
}

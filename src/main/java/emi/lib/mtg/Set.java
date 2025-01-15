package emi.lib.mtg;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A set of card prints. This is meant to map cleanly onto i.e. "Unhinged".
 */
public interface Set {
	enum Type {
		/**
		 * A core or expansion set, which immediately enters Standard.
		 */
		Standard,

		/**
		 * A set containing only reprints of preexisting cards. May or may not be a remaster of a specific set.
		 */
		Remaster,

		/**
		 * A set containing only promotional prints of cards. May come out at the same time as the first prints of those cards.
		 */
		Promo,

		/**
		 * A preconstructed deck/set, including Premium Deck Series, Commander deck sets, and Challenger decks.
		 */
		Precon,

		/**
		 * A catch-all classification for sets that don't fit under other classifications.
		 */
		Other
	}

	/**
	 * @return This set's name.
	 */
	String name();

	/**
	 * @return This set's code.
	 */
	String code();

	/**
	 * @return This set's gross classification. See {@link Type}.
	 */
	Type type();

	/**
	 * @return The day this set was released (approximately).
	 */
	LocalDate releaseDate();

	/**
	 * @return True if this set is a digital-only set, from MTGO or Magic Arena, for instance.
	 */
	boolean digital();

	/**
	 * @return The set of cards printed in this set.
	 */
	java.util.Set<? extends Card.Print> prints();

	/**
	 * Finds a card print in this set by ID.
	 * @param id Unique ID of the card print.
	 * @return The print with that ID, or null if this set contains no such print.
	 */
	Card.Print print(UUID id);

	/**
	 * Finds a card print in this set by collector number.
	 * @param collectorNumber Collector number of the card print.
	 * @return The print with that collector number, or null if this set contains no such print.
	 */
	Card.Print print(String collectorNumber);
}

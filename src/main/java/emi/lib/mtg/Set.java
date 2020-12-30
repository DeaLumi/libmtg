package emi.lib.mtg;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A set of card printings. This is meant to map cleanly onto i.e. "Unhinged".
 */
public interface Set {
	/**
	 * @return This set's name.
	 */
	String name();

	/**
	 * @return This set's code.
	 */
	String code();

	/**
	 * @return The day this set was released (approximately).
	 */
	LocalDate releaseDate();

	/**
	 * @return True if this set is a digital set, from MTGO or Magic Arena, for instance.
	 */
	boolean digital();

	/**
	 * @return The set of cards printed in this set.
	 */
	java.util.Set<? extends Card.Printing> printings();

	/**
	 * Finds a card printing in this set by ID.
	 * @param id Unique ID of the card printing.
	 * @return The printing with that ID, or null if this set contains no such printing.
	 */
	Card.Printing printing(UUID id);
}

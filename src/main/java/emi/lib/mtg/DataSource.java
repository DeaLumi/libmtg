package emi.lib.mtg;

import emi.lib.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.function.DoubleConsumer;

/**
 * Represents a known universe of Magic: the Gathering cards and sets and formats and more.
 */
@Service
@Service.Property.String(name="name")
public interface DataSource {
	/**
	 * @return Set of all cards known to this data source.
	 */
	java.util.Set<? extends Card> cards();

	/**
	 * Finds a card by English card name.
	 * @param name Name of the card to find.
	 * @return Card with that name, or null if no such card exists.
	 */
	Card card(String name);

	/**
	 * @return Set of all card printings known to this data source.
	 */
	java.util.Set<? extends Card.Printing> printings();

	/**
	 * Finds a card printing by UUID.
	 * @param id ID of the card printing to find.
	 * @return Card printing with that ID, or null if no such printing is known.
	 */
	Card.Printing printing(UUID id);

	/**
	 * @return Set of all card sets known to this data source.
	 */
	java.util.Set<? extends Set> sets();

	/**
	 * Finds a card set by code.
	 * @param code Abbreviation of the set to find.
	 * @return Set with that set-code, or null if no such set exists.
	 */
	Set set(String code);

	/**
	 * Update this data source to reflect the most recent Magic universe.
	 * @param progress Optional target for progress updates
	 * @return True if the update caused a change in data.
	 */
	boolean update(DoubleConsumer progress) throws IOException;

	/**
	 * @return True if the data source seems to be stale.
	 */
	boolean needsUpdate();
}

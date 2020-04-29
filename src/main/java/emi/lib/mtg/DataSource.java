package emi.lib.mtg;

import java.io.IOException;
import java.util.UUID;
import java.util.function.DoubleConsumer;

/**
 * Represents a known universe of Magic: the Gathering cards and sets and formats and more.
 */
public interface DataSource {
	/**
	 * Called when this data source is selected and about to be used.
	 * If this completes successfully, all data access methods below should work.
	 * @throws IOException If the data couldn't be loaded for any reason.
	 */
	void loadData() throws IOException;

	/**
	 * @return Set of all cards known to this data source.
	 */
	java.util.Set<? extends Card> cards();

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
	 * @return True if the update completed successfully and caused a change in data.
	 * @throws IOException If an IO exception causes the update to fail.
	 */
	boolean update(DoubleConsumer progress) throws IOException;

	/**
	 * @return True if the data source seems to be stale.
	 */
	boolean needsUpdate();
}

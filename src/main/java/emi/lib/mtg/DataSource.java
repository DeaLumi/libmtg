package emi.lib.mtg;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.DoubleConsumer;

/**
 * Represents a known universe of Magic: the Gathering cards and sets and formats and more.
 */
public interface DataSource {
	/**
	 * Called when this data source is selected and about to be used.
	 * If this completes successfully, all data access methods below should work.
	 * @param dataDir The path where the data is/is to be stored.
	 * @param progress Optional callback to report loading progress percentage (0-1).
	 * @return True if data was successfully loaded.
	 * @throws IOException If the data couldn't be loaded for any reason.
	 */
	boolean loadData(Path dataDir, DoubleConsumer progress) throws IOException;

	/**
	 * @return Set of all cards known to this data source.
	 */
	java.util.Set<? extends Card> cards();

	/**
	 * @return Set of all card printings known to this data source.
	 */
	java.util.Set<? extends Card.Printing> printings();

	/**
	 * Finds a card printing by unique reference (combination of set code, card name, and collector number).
	 * @param reference The printing reference to look up.
	 * @return Card referred to by <code>reference</code>, or null if no such printing is known.
	 */
	default Card.Printing printing(Card.Printing.Reference reference) {
		Set set = set(reference.setCode());
		if (set == null) return null;

		Card.Printing printing = set.printing(reference.collectorNumber());
		if (printing == null) return null;
		if (!reference.name().equals(printing.card().name())) return null;

		return printing;
	}

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
	 * @param dataDir The path where the data is/is to be stored.
	 * @param progress Optional target for progress updates (0-1).
	 * @return True if the update completed successfully and caused a change in data.
	 * @throws IOException If an IO exception causes the update to fail.
	 */
	boolean update(Path dataDir, DoubleConsumer progress) throws IOException;

	/**
	 * @return True if the data source seems to be stale.
	 * @param dataDir The path where the data is/is to be stored.
	 */
	boolean needsUpdate(Path dataDir);
}

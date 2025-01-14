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
	 * Finds a card by English card name. The returned card is such that <code>name.equals({@link Card#name})</code>.
	 * Note that a very small number of cards are not uniquely named despite having varied rules texts. See
	 * {@link DataSource#card(String, char)}.
	 *
	 * @param name The English card name to look up.
	 * @return A card with the given English card name.
	 */
	default Card card(String name) {
		return card(name, 'a');
	}

	/**
	 * Finds a card by English card name and variation letter. The returned card is such that
	 * <code>name.equals({@link Card#name})</code>. The variation letter applies in an extremely narrow set of cards,
	 * but is required to completely and uniquely identify all cards. If the given card name does not have variations,
	 * the value of variation should be ignored.
	 *
	 * To wit, the following cards require a variation letter ('a' - 'f'):
	 * <ul>
	 *     <li>Knight of the Kitchen Sink (UST) 12a-f</li>
	 *     <li>Very Cryptic Command (UST) 49a-f</li>
	 *     <li>Sly Spy (UST) 67a-f</li>
	 *     <li>Garbage Elemental (UST) 82a-f</li>
	 *     <li>Ineffable Blessing (UST) 113a-f</li>
	 *     <li>Everythingamajig (UST) 147a-f</li>
	 * </ul>
	 *
	 *
	 * @param name The English card name to look up.
	 * @param variation The variation letter for a few specific cards. Ignored if the card name is unique on its own.
	 * @return The unique card with the given English card name and, possibly, variation letter.
	 */
	Card card(String name, char variation);

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
}

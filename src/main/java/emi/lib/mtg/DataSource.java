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
	 * @return Set of all card prints known to this data source.
	 */
	java.util.Set<? extends Card.Print> prints();

	/**
	 * Finds a card print by unique reference (combination of set code, card name, and collector number).
	 * @param reference The print reference to look up.
	 * @return Card referred to by <code>reference</code>, or null if no such print is known.
	 */
	default Card.Print print(Card.Print.Reference reference) {
		Set set = set(reference.setCode());
		if (set == null) return null;

		Card.Print print = set.print(reference.collectorNumber());
		if (print == null) return null;
		if (!reference.name().equals(print.card().name())) return null;

		return print;
	}

	/**
	 * Finds a card print by UUID.
	 * @param id ID of the card print to find.
	 * @return Card print with that ID, or null if no such print is known.
	 * @deprecated Avoid using UUIDs to refer to prints, if possible.
	 */
	@Deprecated
	Card.Print print(UUID id);

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

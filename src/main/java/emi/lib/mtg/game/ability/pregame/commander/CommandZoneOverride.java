package emi.lib.mtg.game.ability.pregame.commander;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.pregame.DeckConstructionAbility;

import java.util.Collection;

public interface CommandZoneOverride extends DeckConstructionAbility {
	/**
	 * Check this commander override ability against the deck.
	 * @param source The specific card bearing this ability. Is present in the deck's command zone and the commanders list.
	 * @param deck The decklist being validated. Implementers may not attempt to modify the deck.
	 * @param commanders The list of commanders, provided for convenience. May or may not be equivalent to the collection of cards in the deck's command zone.
	 * @return A Format.ValidationResult containing any errors resulting from the validation. Should be null or {@link Format.ValidationResult#empty} if the commanders list is valid according to this ability.
	 */
	Format.ValidationResult check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders);

	/**
	 * Returns a parent class for a family of override abilities. If a card has multiple abilities inheriting from the
	 * returned type, each is checked. If any succeeds (returns an {@link Format.ValidationResult#empty} result), the
	 * other results are discarded. If all fail, the results are all merged into the final validation result for that
	 * family. A given card only passes validation if all families have empty validation results.
	 *
	 * @return A parent class for a family of override abilities. If this ability has no alternative abilities, it
	 *    should return its own type.
	 */
	default Class<? extends CommandZoneOverride> constraintFamily() {
		return this.getClass();
	}
}

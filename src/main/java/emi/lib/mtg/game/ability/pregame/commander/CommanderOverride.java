package emi.lib.mtg.game.ability.pregame.commander;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.Ability;
import emi.lib.mtg.game.ability.pregame.DeckConstructionAbility;

import java.util.regex.Matcher;

public interface CommanderOverride extends DeckConstructionAbility {
	enum CheckResult {
		Invalid,
		Indeterminate,
		Valid;
	}

	/**
	 * Check whether or not <code>source</code> is allowed to be a commander in the context of the deck.
	 * @param source The specific card with this override ability.
	 * @param deck The deck containing the card. <code>source</code> should be present in deck's command zone.
	 * @param validation The in-progress validation result; if the check result is invalid, this should be updated to
	 *                      explain why.
	 * @return {@link CheckResult#Invalid} if the source cannot be a commander for this deck on account of this ability.
	 * 		      {@link CheckResult#Valid} if the source is legal as a commander on account of this ability.
	 * 		      {@link CheckResult#Indeterminate} if this ability has not influenced the source's legality as a
	 * 		      commander; another check's result will be used, or the default if no other checks occur.
	 * 		      Note that Invalid &gt; Valid &gt; default validity.
	 */
	CheckResult check(Card.Printing source, Deck deck, Format.Validator.Result validation);

	class CanBeCommander implements CommanderOverride {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<CanBeCommander> type() {
				return CanBeCommander.class;
			}

			@Override
			public String pattern() {
				return CARD_NAME + " can be your commander.";
			}

			@Override
			public CanBeCommander make(Card.Face face, Matcher result) {
				return new CanBeCommander();
			}
		}

		public CanBeCommander() {

		}

		@Override
		public String text() {
			return "This card can be your commander.";
		}

		@Override
		public CheckResult check(Card.Printing source, Deck deck, Format.Validator.Result result) {
			// Basically a no-op; if source has this ability, it is a valid commander. Nothing to flag.
			return CheckResult.Valid;
		}
	}
}

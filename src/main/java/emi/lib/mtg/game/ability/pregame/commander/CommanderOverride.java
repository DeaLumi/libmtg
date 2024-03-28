package emi.lib.mtg.game.ability.pregame.commander;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.ability.Ability;
import emi.lib.mtg.game.ability.pregame.DeckConstructionAbility;

import java.util.regex.Matcher;

public interface CommanderOverride extends DeckConstructionAbility {
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
	}
}

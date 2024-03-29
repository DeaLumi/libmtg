package emi.lib.mtg.game.ability.pregame;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.Ability;

public interface DeckConstructionAbility extends Ability.PreGameAbility {
	interface Restriction extends DeckConstructionAbility {
		boolean check(Card.Printing source, Deck deck, Format.ValidationResult result);
	}
}

package emi.lib.mtg.game.ability;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Zone;

import java.util.Set;
import java.util.regex.Matcher;

public interface Ability {
	interface Parser {
		String CARD_NAME = "[- ,A-Za-z0-9']+";

		Class<? extends Ability> type();
		String pattern();
		Ability make(Card.Face card, Matcher match);
	}

	String text();

	interface PreGameAbility extends Ability {
	}

	interface GameAbility extends Ability {
		Set<Zone> functionalZones();
	}
}

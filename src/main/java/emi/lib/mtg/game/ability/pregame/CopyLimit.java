package emi.lib.mtg.game.ability.pregame;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.ability.Ability;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class CopyLimit implements DeckConstructionAbility {
	public static class Parser implements Ability.Parser {
		public static final List<String> NUMBER_WORDS = Arrays.asList(
				"zero",
				"one",
				"two",
				"three",
				"four",
				"five",
				"six",
				"seven",
				"eight",
				"nine",
				"ten"
		); // I'll add more if they really need to be here...

		@Override
		public Class<CopyLimit> type() {
			return CopyLimit.class;
		}

		@Override
		public String pattern() {
			return "A deck can have (any number of|(?<copyLimitExact>only|up to) (?<copyLimitN>[-a-z]+)) cards? named " + CARD_NAME + "\\.";
		}

		@Override
		public CopyLimit make(Card.Face face, Matcher match) {
			return new CopyLimit(NUMBER_WORDS.indexOf(match.group("copyLimitN")), "only".equals(match.group("copyLimitExact")));
		}
	}

	public final int min, max;

	public CopyLimit(int limit, boolean exact) {
		this.max = limit;
		this.min = exact ? max : -1;
	}

	@Override
	public String text() {
		return "A deck can have " + (max < 0 ? "any number of" : (min == max ? "only" : "up to ") + Parser.NUMBER_WORDS.get(max)) + " card" + (max > 1 ? "s" : "") + " with this name.";
	}
}

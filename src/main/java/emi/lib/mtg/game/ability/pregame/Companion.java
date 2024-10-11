package emi.lib.mtg.game.ability.pregame;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;
import emi.lib.mtg.game.ability.Ability;
import emi.lib.mtg.game.ability.StaticAbility;
import emi.lib.mtg.game.validation.Companions;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;

public class Companion implements DeckConstructionAbility, StaticAbility {
	public static class Parser implements Ability.Parser {
		@Override
		public Class<Companion> type() {
			return Companion.class;
		}

		@Override
		public String pattern() {
			return "Companion [-\u2014] (?<companionRequirement>[A-Za-z0-9, .]+)(?: \\([^)]+\\))?";
		}

		@Override
		public Companion make(Card.Face face, Matcher match) {
			return new Companion(face.name(), match.group("companionRequirement"), Companions.COMPANIONS.get(face.name()));
		}
	}

	public interface Validator {
		boolean check(Deck deck, Format format, Format.Validator.Result result);
	}

	public final String name;
	public final String requirement;
	public final Validator validator;

	public Companion(String name, String requirement, Validator validator) {
		this.name = name;
		this.requirement = requirement;
		this.validator = validator;
	}

	@Override
	public String text() {
		return "If this card is your chosen companion, you may put it into your hand from outside the game for {3} any time you could cast a sorcery.";
	}

	public boolean check(Card.Printing source, Deck deck, Format format, Format.Validator.Result result) {
		if (validator.check(deck, format, result)) {
			result.card(source).notices.add(source.card().name() + " is a satisfied companion!");
			return true;
		}

		return false;
	}

	@Override
	public Set<Zone> functionalZones() {
		return EnumSet.of(Zone.Sideboard, Zone.Command);
	}
}

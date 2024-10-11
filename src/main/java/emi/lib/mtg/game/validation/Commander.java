package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.enums.Color;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;
import emi.lib.mtg.game.ability.pregame.commander.CommandZoneOverride;
import emi.lib.mtg.game.ability.pregame.commander.CommanderOverride;
import emi.lib.mtg.game.ability.pregame.Companion;

import java.util.*;

public class Commander implements Format.Validator {
	public static final Commander INSTANCE = new Commander();
	public static final Format.Validator AND_COMPANIONS = INSTANCE.andThen(Companions.INSTANCE);

	public static boolean isCommander(Card card) {
		Card.Face front = card.front();
		if (front == null) return false; // For now, at least, no split cards are commanders.

		// This is a slight hack, since we don't need to evaluate the card instance in the context of the deck for this
		// particular ability.
		if (front.abilities().ofType(CommanderOverride.CanBeCommander.class).findAny().isPresent()) return true;

		TypeLine type = front.type();
		return type.is(Supertype.Legendary) && type.is(CardType.Creature);
	}

	public static boolean validateCommander(Card.Printing pr, Deck deck, Result result) {
		Card.Face front = pr.card().front();
		if (front == null) return false; // TODO: Could a split card be allowed to be a commander?

		CommanderOverride.CheckResult check = CommanderOverride.CheckResult.Indeterminate;
		for (CommanderOverride override : front.abilities().ofType(CommanderOverride.class).toArray(CommanderOverride[]::new)) {
			CommanderOverride.CheckResult thisCheck = override.check(pr, deck, result);

			switch (thisCheck) {
				case Invalid:
					check = CommanderOverride.CheckResult.Invalid;
					break;
				case Indeterminate:
					break;
				case Valid:
					if (check != CommanderOverride.CheckResult.Invalid) check = CommanderOverride.CheckResult.Valid;
					break;
			}
		}

		switch (check) {
			case Invalid:
				return false;
			case Valid:
				return true;
			case Indeterminate:
				if (!isCommander(pr.card())) {
					result.card(pr).errors.add(String.format("%s is not a legal commander.", pr.card().name()));
					return false;
				} else {
					return true;
				}
		}

		// This isn't possible.
		throw new IllegalStateException("Unhandled CommanderOverride CheckResult!");
	}

	@Override
	public Result validate(Deck deck, Format format, Result result) {
		/*
		 * The rules as they stand:
		 * 1. A command zone can have three cards if one is a satisfied companion and the other two satisfy at least one partner constraint.
		 * 2. A command zone can have two cards if they satisfy a partner constraint, or if one is a satisfied companion and the other is a valid commander.
		 * 3. A command zone can have one card if it is a valid commander.
		 *
		 * A valid commander is a card which:
		 * a. Is a legendary creature card.
		 * b. Has the ability "CARDNAME can be your commander."
		 *
		 * Two cards satisfy a partner constraint if they:
		 * a. Are both valid commanders with the Partner ability.
		 * b. Are both valid commanders with the Partner-With ability, where each ability references the other card.
		 * c. Are both valid commanders with the Friends forever ability.
		 * d. Are a valid commander with "Choose a background" and a legendary Background enchantment[1]
		 * e. Are a valid commander with "Doctor's Companion" and a legendary Time Lord Doctor creature with no other creature types.
		 * f. Are a valid commander with "Legendary partner" and a creature[2]
		 * g. Are a nonlegendary creature and a legendary Background enchantment with "Create a Character"[2]
		 *
		 * [1] Technically, CR702.124k says choose-a-background is exclusive of other partner abilities, but I suspect
		 * that's a mistake. My implementation will allow a card with both partner and choose a background to satisfy
		 * either or none.
		 * [2] These two abilities aren't in the comprehensives rules, and have only been printed on Heroes of the Realm
		 * cards Sol, Advocate Eternal and Wizard from Beyond, respectively. But I'd like to implement them because I
		 * want to build with those commanders.
		 *
		 * Plan for generalizing command zone validation in the presence of abilities:
		 * 1. Allow abilities to override the commander validity check, including for cards besides the source of the ability.
		 * 2. If an ability expresses alternatives, try all of them; drop failures if any succeeds.
		 */

		Collection<? extends Card.Printing> cmdZone = deck.cards(Zone.Command) == null ? Collections.emptyList() : deck.cards(Zone.Command);

		Collection<Card.Printing> commanders = new ArrayList<>();
		Collection<Card.Printing> satisfiedCompanions = new ArrayList<>();
		Map<Card.Printing, Map<Class<? extends CommandZoneOverride>, Set<CommandZoneOverride>>> overrides = new HashMap<>();

		for (Card.Printing pr : cmdZone) {
			Card.Face front = pr.card().front();

			if (front != null) {
				// N.B. This can't be .anyMatch or any other short-circuiting terminal operation.
				if (front.abilities().ofType(Companion.class)
						.filter(c -> c.check(pr, deck, format, result))
						.count() > 0) {
					satisfiedCompanions.add(pr);
				} else {
					commanders.add(pr);
				}

				final Card.Printing fpr = pr;
				front.abilities().ofType(CommandZoneOverride.class)
						.forEach(override -> overrides.computeIfAbsent(fpr, k -> new HashMap<>()).computeIfAbsent(override.constraintFamily(), k -> new HashSet<>()).add(override));
			}
		}

		Color.Combination colorIdentity = Color.Combination.Empty;

		if (!overrides.isEmpty()) {
			// TODO Validate overrides
			for (Map.Entry<Card.Printing, Map<Class<? extends CommandZoneOverride>, Set<CommandZoneOverride>>> entry : overrides.entrySet()) {
				family: for (Map.Entry<Class<? extends CommandZoneOverride>, Set<CommandZoneOverride>> family : entry.getValue().entrySet()) {
					Result squashed = new Result();
					for (CommandZoneOverride override : family.getValue()) {
						Result overrideCheckResult = override.check(entry.getKey(), deck, commanders);
						if (overrideCheckResult.empty()) continue family;
						squashed.merge(overrideCheckResult);
					}
					result.merge(squashed);
				}
			}
		} else {
			if (commanders.isEmpty()) {
				result.zoneErrors(Zone.Command).add("A Commander deck must designate a legendary creature to act as the deck's commander.");
				colorIdentity = Color.Combination.FiveColorC; // Don't complain about colors until there's a commander.
			} else if (commanders.size() > 1) {
				result.zoneErrors(Zone.Command).add("A Commander deck can't have more than one designated commander unless an ability like Partner allows it.");
			}

			// In the absence of an override, all commanders are subject to standard validity checking.
			for (Card.Printing pr : commanders) {
				validateCommander(pr, deck, result);
			}
		}

		for (Card.Printing pr : commanders) {
			colorIdentity = colorIdentity.plus(pr.card().colorIdentity());
		}
		final Color.Combination fci = colorIdentity;

		Format.ZoneInfo lib = format.zones.get(Zone.Library);
		Format.ZoneInfo cmd = format.zones.get(Zone.Command);

		int minmax = lib.minCards + cmd.maxCards, maxmin = lib.maxCards + cmd.minCards;

		if (minmax == maxmin) {
			if (deck.cards(Zone.Library) == null || deck.cards(Zone.Library).size() + commanders.size() != minmax) {
				result.deckErrors.add("A Commander deck must contain exactly " + minmax + " cards, including its commander(s).");
			}
		}

		deck.cards(Zone.Library).stream()
				.filter(pr -> !fci.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.card(pr).errors.add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));

		satisfiedCompanions.stream()
				.filter(pr -> !fci.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.card(pr).errors.add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));

		return result;
	}
}

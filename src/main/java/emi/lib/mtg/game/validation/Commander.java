package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.enums.Color;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;
import emi.lib.mtg.game.ability.pregame.CommanderOverride;
import emi.lib.mtg.game.ability.pregame.Companion;
import emi.lib.mtg.game.ability.pregame.PartnerCommander;

import java.util.*;
import java.util.function.BiConsumer;

public class Commander implements BiConsumer<Deck, Format.ValidationResult> {
	public static final Commander INSTANCE = new Commander();

	public static boolean isCommander(Card card) {
		Card.Face front = card.front();
		if (front == null) return false; // For now, at least, no split cards are commanders.

		if (front.abilities().ofType(CommanderOverride.CanBeCommander.class).findAny().isPresent()) return true;

		TypeLine type = front.type();
		return type.is(Supertype.Legendary) && type.is(CardType.Creature);
	}

	@Override
	public void accept(Deck deck, Format.ValidationResult result) {
		Collection<? extends Card.Printing> cmdZone = deck.cards(Zone.Command) == null ? Collections.emptyList() : deck.cards(Zone.Command);

		Collection<Card.Printing> commanders = new ArrayList<>();
		Collection<Card.Printing> satisfiedCompanions = new ArrayList<>();
		Map<Card.Printing, PartnerCommander> partners = new HashMap<>();
		boolean legendaryPartner = false;

		for (Card.Printing pr : cmdZone) {
			Card.Face front = pr.card().front();

			if (front != null) {
				// N.B. This can't be .anyMatch or any other short-circuiting terminal operation.
				if (front.abilities().ofType(Companion.class)
						.filter(c -> c.check(pr, deck, result))
						.count() > 0) {
					satisfiedCompanions.add(pr);
				} else {
					commanders.add(pr);
				}

				PartnerCommander partner = front.abilities().ofType(PartnerCommander.class).findAny().orElse(null);
				if (partner != null) {
					partners.put(pr, partner);
					if (partner.legendary) legendaryPartner = true;
				}
			}
		}

		if (commanders.isEmpty() || (commanders.size() == 1 && legendaryPartner && !satisfiedCompanions.isEmpty())) {
			commanders.addAll(satisfiedCompanions);
		}

		Color.Combination colorIdentity = Color.Combination.Empty;

		if (partners.isEmpty()) {
			if (commanders.isEmpty()) {
				result.zoneErrors(Zone.Command).add("A Commander deck must contain a legendary creature to act as the deck's commander.");
				colorIdentity = Color.Combination.FiveColorC; // Don't complain about colors until there's a commander.
			} else if (commanders.size() > 1) {
				result.zoneErrors(Zone.Command).add("A Commander deck shouldn't contain any more than one commander.");
			}
		} else {
			for (Map.Entry<Card.Printing, PartnerCommander> pr : partners.entrySet()) {
				pr.getValue().check(pr.getKey(), commanders, result);
			}

			if (!legendaryPartner) {
				for (Card.Printing pr : commanders) {
					if (!partners.containsKey(pr)) {
						result.card(pr).errors.add(pr.card().name() + " can't be partnered with another commander.");
					}
				}
			}
		}

		for (Card.Printing pr : commanders) {
			if (!legendaryPartner && !isCommander(pr.card())) {
				result.card(pr).errors.add(String.format("%s is not a valid commander.", pr.card().name()));
			}
			colorIdentity = colorIdentity.plus(pr.card().colorIdentity());
		}
		final Color.Combination fci = colorIdentity;

		if (deck.cards(Zone.Library) == null || deck.cards(Zone.Library).size() + commanders.size() != 100) {
			result.deckErrors.add("A Commander deck must contain exactly 100 cards, including its commander(s).");
		}

		deck.cards(Zone.Library).stream()
				.filter(pr -> !fci.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.card(pr).errors.add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));

		satisfiedCompanions.stream()
				.filter(pr -> !fci.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.card(pr).errors.add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));
	}
}

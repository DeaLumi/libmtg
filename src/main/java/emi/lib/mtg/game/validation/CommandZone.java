package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandZone implements BiConsumer<Deck, Format.ValidationResult> {
	public static final CommandZone INSTANCE = new CommandZone();

	private static final Pattern PARTNER_PATTERN = Pattern.compile("(?<legendary>Legendary )?[Pp]artner(?: with (?<with>[-A-Za-z0-9 ,]+))?");

	private static boolean isCreature(Card.Printing pr) {
		Card.Face front = pr.card().face(Card.Face.Kind.Front);
		return front != null && front.type().cardTypes().contains(CardType.Creature);
	}

	private static void validateCommander(Format.ValidationResult result, Collection<? extends Card.Printing> cmdZone, Card.Printing cmdr) {
		Card.Face front = cmdr.card().face(Card.Face.Kind.Front);

		if (front != null) {
			// cmdr is a valid commander if it says it is.
			if (front.rules().contains("can be your commander.")) return;

			if (front.type().cardTypes().contains(CardType.Creature)) {
				if (front.type().supertypes().contains(Supertype.Legendary)) {
					Matcher matcher = PARTNER_PATTERN.matcher(front.rules());
					if (matcher.find()) {
						if (matcher.group("legendary") != null) {
							// Legendary partner. It's a valid commander if there's exactly one other creature card in the command zone.
							// (If that card has partner, it will fail its own validation if we're not the right partner for it.)
							if (cmdZone.size() <= 2 && cmdZone.stream().allMatch(pr -> pr == cmdr || isCreature(pr))) {
								return;
							} else {
								result.cardErrors(cmdr).add(String.format("%s must be partnered with exactly one creature card.", front.name()));
								return;
							}
						} else if (matcher.group("with") != null) {
							// Partner-with. It's a valid commander if there's exactly one other creature card in the command zone,
							// *and* its name matches.
							if (cmdZone.size() <= 2) {
								Card.Printing other = cmdZone.stream().filter(pr -> pr != cmdr).findAny().orElse(null);
								if (other == null || other.card().name().equals(matcher.group("with").trim())) {
									return;
								} else {
									result.cardErrors(cmdr).add(String.format("%s must be partnered with exactly %s, not %s.", front.name(), matcher.group("with").trim(), other.card().name()));
									return;
								}
							} else {
								result.cardErrors(cmdr).add(String.format("%s must be partnered with exactly %s.", front.name(), matcher.group("with").trim()));
								return;
							}
						} else {
							// Ordinary partner. It's a valid commander if there's exactly one other creature card in the command zone,
							// *and* it too has partner. (If it has partner-with, it will fail its validation if we're not the right partner.)
							if (cmdZone.size() <= 2) {
								Card.Printing other = cmdZone.stream().filter(pr -> pr != cmdr).findAny().orElse(null);
								if (other == null || (isCreature(other) && PARTNER_PATTERN.matcher(other.card().face(Card.Face.Kind.Front).rules()).find())) {
									return;
								} else {
									result.cardErrors(cmdr).add(String.format("%s must be partnered with a creature card with partner.", front.name()));
									return;
								}
							} else {
								result.cardErrors(cmdr).add(String.format("%s must be partnered with exactly one creature card with partner.", front.name()));
								return;
							}
						}
					} else {
						// No 'partner' in cmdr's rules. It's a valid commander if it's the only one.
						if (cmdZone.size() == 1) {
							return;
						} else {
							result.cardErrors(cmdr).add(String.format("%s can't be partnered with any other cards.", front.name()));
							return;
						}
					}
				} else {
					// Nonlegendary creature card. It's a valid commander if there's exactly one other creature card in the command zone and it has legendary partner.
					if (cmdZone.size() == 2) {
						Card.Printing other = cmdZone.stream().filter(pr -> pr != cmdr).findAny().orElseThrow(AssertionError::new);
						if (other != null && other.card().face(Card.Face.Kind.Front) != null) {
							Matcher matcher = PARTNER_PATTERN.matcher(other.card().face(Card.Face.Kind.Front).rules());
							if (matcher.find() && matcher.group("legendary") != null) {
								return;
							}
						}
					}
				}
			}
		}

		result.cardErrors(cmdr).add(String.format("%s is not a valid commander.", cmdr.card().name()));
	}

	@Override
	public void accept(Deck deck, Format.ValidationResult result) {
		Collection<? extends Card.Printing> commanders = deck.cards(Zone.Command) == null ? Collections.emptyList() : deck.cards(Zone.Command);
		Collection<? extends Card.Printing> library = deck.cards(Zone.Library) == null ? Collections.emptyList() : deck.cards(Zone.Library);

		Set<Color> cmdrColors = EnumSet.of(Color.COLORLESS);
		for (Card.Printing pr : commanders) {
			validateCommander(result, commanders, pr);
			cmdrColors.addAll(pr.card().colorIdentity());
		}

		library.stream()
				.filter(pr -> !cmdrColors.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.cardErrors.computeIfAbsent(pr, p -> new HashSet<>()).add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));
	}
}

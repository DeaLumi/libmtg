package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commander implements BiConsumer<Deck, Format.ValidationResult> {
	public static final Commander INSTANCE = new Commander();

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
								result.card(cmdr).errors.add(String.format("%s must be partnered with exactly one creature card.", front.name()));
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
									result.card(cmdr).errors.add(String.format("%s must be partnered with exactly %s, not %s.", front.name(), matcher.group("with").trim(), other.card().name()));
									return;
								}
							} else {
								result.card(cmdr).errors.add(String.format("%s must be partnered with exactly %s.", front.name(), matcher.group("with").trim()));
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
									result.card(cmdr).errors.add(String.format("%s must be partnered with a creature card with partner.", front.name()));
									return;
								}
							} else {
								result.card(cmdr).errors.add(String.format("%s must be partnered with exactly one creature card with partner.", front.name()));
								return;
							}
						}
					} else {
						// No 'partner' in cmdr's rules. It's a valid commander if it's the only one.
						if (cmdZone.size() == 1) {
							return;
						} else {
							result.card(cmdr).errors.add(String.format("%s can't be partnered with any other cards.", front.name()));
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

		result.card(cmdr).errors.add(String.format("%s is not a valid commander.", cmdr.card().name()));
	}

	private static boolean isCommander(Card.Face face) {
		if (face.rules().contains("can be your commander.")) return true;

		CardTypeLine type = face.type();
		return type.supertypes().contains(Supertype.Legendary) &&
				type.cardTypes().contains(CardType.Creature);
	}

	@Override
	public void accept(Deck deck, Format.ValidationResult result) {
		Collection<? extends Card.Printing> cmdZone = deck.cards(Zone.Command) == null ? Collections.emptyList() : deck.cards(Zone.Command);

		Collection<Card.Printing> cmdrs = new ArrayList<>();
		Collection<Card.Printing> companions = new ArrayList<>();
		Collection<Card.Printing> startDeck = new ArrayList<>();

		// Pass 1: Filter compansions and valid commanders from the other cards in the zone.
		for (Card.Printing pr : cmdZone) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);
			if (front != null) {
				if (front.rules().startsWith("Companion ")) {
					companions.add(pr);
					continue;
				} else if (isCommander(front)) {
					cmdrs.add(pr);
					continue;
				}
			}
			startDeck.add(pr);
		}

		// Pass 2: If there are no commanders, take the companions.
		if (cmdrs.isEmpty()) {
			cmdrs.addAll(companions);
			companions.clear();
		}

		// Pass 3: If there's exactly one commander with legendary partner, take all creatures.
		if (cmdrs.size() == 1) {
			Card.Face front = cmdrs.iterator().next().card().face(Card.Face.Kind.Front);
			Matcher m = PARTNER_PATTERN.matcher(front.rules());
			if (m.find() && m.group("legendary") != null) {
				Iterator<Card.Printing> iter = startDeck.iterator();
				while (iter.hasNext()) {
					Card.Printing pr = iter.next();
					Card.Face fr = pr.card().face(Card.Face.Kind.Front);
					if (fr == null) continue;

					if (fr.type().cardTypes().contains(CardType.Creature)) {
						iter.remove();
						cmdrs.add(pr);
					}
				}
			}
		}

		Set<Color> cmdrColors = EnumSet.of(Color.Colorless);
		for (Card.Printing pr : cmdrs) {
			validateCommander(result, cmdrs, pr);
			cmdrColors.addAll(pr.card().colorIdentity());
		}

		for (Card.Printing pr : startDeck) {
			result.card(pr).warnings.add(String.format("%s is not expected in the command zone.", pr.card().name()));
		}

		startDeck.addAll(cmdrs);
		if (deck.cards(Zone.Library) != null) startDeck.addAll(deck.cards(Zone.Library));
		for (Card.Printing pr : companions) {
			Companions.validateCompanion(startDeck, result, pr);
		}

		if (deck.cards(Zone.Library) == null || deck.cards(Zone.Library).size() + cmdrs.size() != 100) {
			result.deckErrors.add("A Commander deck must contain exactly 100 cards, including its commander.");
		}

		Arrays.stream(Zone.values())
				.map(deck::cards)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(pr -> !cmdrColors.containsAll(pr.card().colorIdentity()))
				.forEach(pr -> result.card(pr).errors.add(String.format("%s contains colors not in your commander's color identity.", pr.card().name())));
	}
}

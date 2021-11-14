package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.Mana;
import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Companions implements BiConsumer<Deck, Format.ValidationResult> {
	public static final Companions INSTANCE = new Companions();

	private static boolean gyruda(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allEven = true;
		for (Card.Printing pr : startDeck) {
			double cmc = pr.card().manaCost().value();

			if (Double.isFinite(cmc) && Math.floor(cmc) == cmc && ((int) cmc % 2) == 0) {
				continue;
			}

			allEven = false;
			result.card(pr).warnings.add(String.format("%s has an odd mana cost.", pr.card().name()));
		}

		return allEven;
	}

	private static boolean jegantha(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allUnique = true;
		for (Card.Printing pr : startDeck) {
			Set<Mana.Symbol> seen = new HashSet<>();

			for (Mana.Symbol sym : pr.card().manaCost().symbols()) {
				if (!seen.add(sym)) {
					allUnique = false;
					result.card(pr).warnings.add(String.format("%s has duplicate symbols in its mana cost.", pr.card().name()));
					break;
				}
			}
		}

		return allUnique;
	}

	private static final Set<String> KAHEERA_SUBTYPES = kaheeraSubtypes();

	private static Set<String> kaheeraSubtypes() {
		Set<String> tmp = new HashSet<>();
		tmp.add("Cat");
		tmp.add("Elemental");
		tmp.add("Nightmare");
		tmp.add("Dinosaur");
		tmp.add("Beast");
		return Collections.unmodifiableSet(tmp);
	}

	private static boolean kaheera(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allCuties = true;
		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front == null || !front.type().cardTypes().contains(CardType.Creature)) continue;

			boolean isCute = false;
			for (String subtype : front.type().subtypes()) {
				if (KAHEERA_SUBTYPES.contains(subtype)) {
					isCute = true;
					break;
				}
			}

			if (isCute) {
				continue;
			}

			allCuties = false;
			result.card(pr).warnings.add(String.format("%s is not a Cat, Elemental, Nightmare, Dinosaur, or Beast.", pr.card().name()));
		}

		return allCuties;
	}

	private static boolean keruga(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allBig = true;
		for (Card.Printing pr : startDeck) {
			double cmc = pr.card().manaCost().value();

			if (cmc >= 3) {
				continue;
			}

			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front != null && front.type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			allBig = false;
			result.card(pr).warnings.add(String.format("%s's converted mana cost is less than 3.", pr.card().name()));
		}

		return allBig;
	}

	private static boolean lurrus(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allSmol = true;
		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front == null) {
				continue;
			}

			TypeLine type = front.type();
			if (!type.isPermanent()) {
				continue;
			}

			double cmc = front.manaValue();
			if (cmc <= 2) {
				continue;
			}

			allSmol = false;
			result.card(pr).warnings.add(String.format("%s has converted mana cost 3 or greater.", front.name()));
		}

		return allSmol;
	}

	private static boolean lutri(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allUnique = true;
		Set<String> seen = new HashSet<>();

		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front != null && front.type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			if (seen.add(pr.card().name())) {
				continue;
			}

			allUnique = false;
			result.card(pr).warnings.add(String.format("%s has the same name as another nonland card.", pr.card().name()));
		}

		return allUnique;
	}

	private static boolean obosh(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allOdd = true;
		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front != null && front.type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			double cmc = pr.card().manaCost().value();
			if (Double.isFinite(cmc) && Math.floor(cmc) == cmc && ((int) cmc) % 2 == 1) {
				continue;
			}

			allOdd = false;
			result.card(pr).warnings.add(String.format("%s has an even converted mana cost.", pr.card().name()));
		}

		return allOdd;
	}

	private static boolean umori(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allSameish = true;
		EnumSet<CardType> sharedType = EnumSet.noneOf(CardType.class);

		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front != null && front.type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			if (sharedType.isEmpty()) {
				pr.card().faces().stream()
						.map(Card.Face::type)
						.map(TypeLine::cardTypes)
						.flatMap(Collection::stream)
						.forEach(sharedType::add);
				continue;
			}

			EnumSet<CardType> intersection = EnumSet.copyOf(sharedType);
			intersection.retainAll(pr.card().faces().stream()
					.map(Card.Face::type)
					.map(TypeLine::cardTypes)
					.flatMap(Collection::stream)
					.collect(Collectors.toCollection(() -> EnumSet.noneOf(CardType.class))));

			if (!intersection.isEmpty()) {
				sharedType.retainAll(intersection);
				continue;
			}

			allSameish = false;
			result.card(pr).warnings.add(String.format("%s doesn't share a card type with all other nonland cards.", pr.card().name()));
		}

		return allSameish;
	}

	private static boolean yorion(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		if (startDeck.size() < result.format().minDeckSize + 20) {
			result.deckErrors.add("Your deck doesn't contain at least 20 cards more than the minimum deck size.");
			return false;
		}

		return true;
	}

	private static final Pattern ACTIVATED_ABILITY_PATTERN = Pattern.compile("(?m)^(?:(?:(?:Equip|Cycling)[— ])|[^\"]+: .+$)");

	private static boolean zirda(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result) {
		boolean allActive = true;
		for (Card.Printing pr : startDeck) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);

			if (front == null || !front.type().isPermanent()) {
				continue;
			}

			TypeLine type = front.type();
			if (type.cardTypes().contains(CardType.Land) && (
					type.subtypes().contains("Plains") ||
					type.subtypes().contains("Island") ||
					type.subtypes().contains("Swamp") ||
					type.subtypes().contains("Mountain") ||
					type.subtypes().contains("Forest")
					)) {
				continue;
			}

			Matcher m = ACTIVATED_ABILITY_PATTERN.matcher(front.rules());

			if (m.find()) {
				continue;
			}

			result.card(pr).warnings.add(String.format("%s does not have an activated ability.", pr.card().name()));
			allActive = false;
		}

		return allActive;
	}

	public static void validateCompanion(Collection<? extends Card.Printing> startDeck, Format.ValidationResult result, Card.Printing companion) {
		Card.Face front = companion.card().face(Card.Face.Kind.Front);
		if (front == null) return;

		switch (front.name()) {
			case "Gyruda, Doom of Depths":
				if (!gyruda(startDeck, result)) return;
				break;
			case "Jegantha, the Wellspring":
				if (!jegantha(startDeck, result)) return;
				break;
			case "Kaheera, the Orphanguard":
				if (!kaheera(startDeck, result)) return;
				break;
			case "Keruga, the Macrosage":
				if (!keruga(startDeck, result)) return;
				break;
			case "Lurrus of the Dream-Den":
				if (!lurrus(startDeck, result)) return;
				break;
			case "Lutri, the Spellchaser":
				if (!lutri(startDeck, result)) return;
				break;
			case "Obosh, the Preypiercer":
				if (!obosh(startDeck, result)) return;
				break;
			case "Umori, the Collector":
				if (!umori(startDeck, result)) return;
				break;
			case "Yorion, Sky Nomad":
				if (!yorion(startDeck, result)) return;
				break;
			case "Zirda, the Dawnwaker":
				if (!zirda(startDeck, result)) return;
				break;
			default:
				return;
		}

		result.card(companion).notices.add(String.format("%s is a satisfied companion!", companion.card().name()));
	}

	@Override
	public void accept(Deck deck, Format.ValidationResult result) {
		Collection<? extends Card.Printing> startDeck = deck.cards(Zone.Library);
		if (startDeck == null) startDeck = Collections.emptyList();

		Collection<? extends Card.Printing> sideboard = deck.cards(Zone.Sideboard);
		if (sideboard == null) sideboard = Collections.emptyList();

		for (Card.Printing pr : sideboard) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);
			if (front == null) continue;

			if (front.rules().startsWith("Companion ")) {
				validateCompanion(startDeck, result, pr);
			}
		}
	}
}

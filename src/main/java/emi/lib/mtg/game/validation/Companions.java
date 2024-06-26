package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.Mana;
import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;
import emi.lib.mtg.game.ability.pregame.Companion;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Companions implements BiConsumer<Deck, Format.ValidationResult> {
	public static final Companions INSTANCE = new Companions();

	public static final Map<String, BiFunction<Deck, Format.ValidationResult, Boolean>> COMPANIONS = companions();

	private static Map<String, BiFunction<Deck, Format.ValidationResult, Boolean>> companions() {
		Map<String, BiFunction<Deck, Format.ValidationResult, Boolean>> map = new HashMap<>();

		map.put("Gyruda, Doom of Depths", Companions::gyruda);
		map.put("Jegantha, the Wellspring", Companions::jegantha);
		map.put("Kaheera, the Orphanguard", Companions::kaheera);
		map.put("Keruga, the Macrosage", Companions::keruga);
		map.put("Lurrus of the Dream-Den", Companions::lurrus);
		map.put("Lutri, the Spellchaser", Companions::lutri);
		map.put("Obosh, the Preypiercer", Companions::obosh);
		map.put("Umori, the Collector", Companions::umori);
		map.put("Yorion, Sky Nomad", Companions::yorion);
		map.put("Zirda, the Dawnwaker", Companions::zirda);

		return Collections.unmodifiableMap(map);
	}

	public static boolean isCompanion(Card.Printing pr) {
		if (pr.card().front() == null) return false;
		return pr.card().front().abilities().ofType(Companion.class).findAny().isPresent();
	}

	private static boolean notCompanion(Card.Printing pr) {
		return !isCompanion(pr);
	}

	private static Stream<Card.Printing> startDeckStream(Deck deck) {
		if (deck.cards(Zone.Library) == null && deck.cards(Zone.Command) == null) return Stream.empty();
		if (deck.cards(Zone.Command) == null) return deck.cards(Zone.Library).stream().map(x -> (Card.Printing) x);
		if (deck.cards(Zone.Library) == null) return deck.cards(Zone.Command).stream().filter(Companions::notCompanion).map(x -> (Card.Printing) x);
		return Stream.concat(deck.cards(Zone.Library).stream(), deck.cards(Zone.Command).stream().filter(Companions::notCompanion));
	}

	private static Iterable<? extends Card.Printing> startDeck(Deck deck) {
		return startDeckStream(deck)::iterator;
	}

	public static boolean gyruda(Deck deck, Format.ValidationResult result) {
		boolean allEven = true;
		for (Card.Printing pr : startDeck(deck)) {
			double cmc = pr.card().manaCost().value();

			if (Double.isFinite(cmc) && Math.floor(cmc) == cmc && ((int) cmc % 2) == 0) {
				continue;
			}

			allEven = false;
			result.card(pr).warnings.add(String.format("%s has an odd mana cost.", pr.card().name()));
		}

		return allEven;
	}

	public static boolean jegantha(Deck deck, Format.ValidationResult result) {
		boolean allUnique = true;
		for (Card.Printing pr : startDeck(deck)) {
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

	public static boolean kaheera(Deck deck, Format.ValidationResult result) {
		boolean allCuties = true;
		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

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

	public static boolean keruga(Deck deck, Format.ValidationResult result) {
		boolean allBig = true;
		for (Card.Printing pr : startDeck(deck)) {
			double cmc = pr.card().manaCost().value();

			if (cmc >= 3) {
				continue;
			}

			Card.Face front = pr.card().front();

			if (front != null && front.type().is(CardType.Land)) {
				continue;
			}

			allBig = false;
			result.card(pr).warnings.add(String.format("%s's converted mana cost is less than 3.", pr.card().name()));
		}

		return allBig;
	}

	public static boolean lurrus(Deck deck, Format.ValidationResult result) {
		boolean allSmol = true;
		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

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

	public static boolean lutri(Deck deck, Format.ValidationResult result) {
		boolean allUnique = true;
		Set<String> seen = new HashSet<>();

		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

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

	public static boolean obosh(Deck deck, Format.ValidationResult result) {
		boolean allOdd = true;
		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

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

	public static boolean umori(Deck deck, Format.ValidationResult result) {
		boolean allSameish = true;
		EnumSet<CardType> sharedType = EnumSet.noneOf(CardType.class);

		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

			if (front != null && front.type().is(CardType.Land)) {
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

	public static boolean yorion(Deck deck, Format.ValidationResult result) {
		int size = 0;

		if (deck.cards(Zone.Library) != null) size += deck.cards(Zone.Library).size();

		if (deck.cards(Zone.Command) != null) {
			size += deck.cards(Zone.Command).stream().filter(Companions::notCompanion).count();
		}

		if (size < deck.format().minDeckSize + 20) {
			result.deckErrors.add("Your deck doesn't contain at least 20 cards more than the minimum deck size.");
			return false;
		}

		return true;
	}

	private static final Pattern ACTIVATED_ABILITY_PATTERN = Pattern.compile("(?m)^(?:(?:(?:Equip|Cycling)[— ])|[^\"]+: .+$)");

	public static boolean zirda(Deck deck, Format.ValidationResult result) {
		boolean allActive = true;
		for (Card.Printing pr : startDeck(deck)) {
			Card.Face front = pr.card().front();

			if (front == null || !front.type().isPermanent()) {
				continue;
			}

			TypeLine type = front.type();
			if (type.is(CardType.Land) && (
					type.is("Plains") ||
					type.is("Island") ||
					type.is("Swamp") ||
					type.is("Mountain") ||
					type.is("Forest")
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

	@Override
	public void accept(Deck deck, Format.ValidationResult result) {
		Collection<? extends Card.Printing> sideboard = deck.cards(Zone.Sideboard);
		if (sideboard == null) sideboard = Collections.emptyList();

		for (Card.Printing pr : sideboard) {
			Card.Face front = pr.card().front();
			if (front == null) continue;

			front.abilities().ofType(Companion.class).forEach(a -> a.check(pr, deck, result));
		}
	}
}

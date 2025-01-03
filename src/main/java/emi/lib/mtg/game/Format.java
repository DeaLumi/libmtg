package emi.lib.mtg.game;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.validation.CardCount;
import emi.lib.mtg.game.validation.CardLegality;
import emi.lib.mtg.game.validation.Companions;

import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Format {
	Freeform (CardCount.Freeform, Companions.INSTANCE),
	Standard,
	Future,
	Pioneer,
	Modern,
	Legacy,
	Vintage,
	Brawl(CardCount.Brawl, CardLegality.INSTANCE, emi.lib.mtg.game.validation.Commander.INSTANCE, Companions.INSTANCE),
	Historic,
	HistoricBrawl(CardCount.Brawl, CardLegality.INSTANCE, emi.lib.mtg.game.validation.Commander.INSTANCE, Companions.INSTANCE),
	Pauper,
	Penny,
	Commander(CardCount.Commander, CardLegality.INSTANCE, emi.lib.mtg.game.validation.Commander.INSTANCE, Companions.INSTANCE),
	Alchemy,
	Explorer;

	public final CardCount cardCount;
	private final Validator validator;

	Format() {
		this(CardCount.ConstructedSixtyCard, CardLegality.INSTANCE, Companions.INSTANCE);
	}

	Format(CardCount cardCount, Validator... validators) {
		this.cardCount = cardCount;

		if (validators.length == 0) {
			validator = null;
		} else {
			Validator tmp = validators[0];

			for (int i = 1; i < validators.length; ++i) tmp = tmp.andThen(validators[i]);

			validator = tmp;
		}
	}

	public Set<Zone> deckZones() {
		return cardCount.deckZones();
	}

	public interface Validator {
		Result validate(Deck deck, Format format, Result result);

		default Validator andThen(Format.Validator other) {
			Objects.requireNonNull(other);

			return (d, f, r) -> other.validate(d, f, validate(d, f, r));
		}

		class Result {
			public static class CardResult {
				public final Set<String> errors = new HashSet<>();
				public final Set<String> warnings = new HashSet<>();
				public final Set<String> notices = new HashSet<>();

				private CardResult() {

				}

				private CardResult(CardResult other) {
					merge(other);
				}

				@Override
				public String toString() {
					StringBuilder sb = new StringBuilder();
					if (!errors.isEmpty()) {
						sb.append("Errors:\n\u2022 ").append(String.join("\n\u2022 ", errors));
					}
					if (!errors.isEmpty() && !warnings.isEmpty()) {
						sb.append('\n').append('\n');
					}
					if (!warnings.isEmpty()) {
						sb.append("Warnings:\n\u2022 ").append(String.join("\n\u2022 ", warnings));
					}
					if ((!errors.isEmpty() || !warnings.isEmpty()) && !notices.isEmpty()) {
						sb.append('\n').append('\n');
					}
					if (!notices.isEmpty()) {
						sb.append("Notices:\n\u2022 ").append(String.join("\n\u2022 ", notices));
					}
					return sb.toString();
				}

				public boolean empty() {
					return errors.isEmpty() && warnings.isEmpty() && notices.isEmpty();
				}

				public CardResult merge(CardResult other) {
					this.errors.addAll(other.errors);
					this.warnings.addAll(other.warnings);
					this.notices.addAll(other.notices);
					return this;
				}
			}

			public static final Result EMPTY = new Result(Collections::emptySet, Collections::emptyMap, Collections::emptyMap);

			public final Set<String> deckErrors;
			public final Map<Zone, Set<String>> zoneErrors;
			public final Map<Card.Printing, CardResult> cards;

			public Result() {
				this.deckErrors = new HashSet<>();
				this.zoneErrors = new HashMap<>();
				this.cards = new HashMap<>();
			}

			public Result(Result other) {
				this();
				merge(other);
			}

			public Result(Supplier<Set<String>> deckErrorsProvider, Supplier<Map<Zone, Set<String>>> zoneErrorsProvider, Supplier<Map<Card.Printing, CardResult>> cardsProvider) {
				this.deckErrors = deckErrorsProvider.get();
				this.zoneErrors = zoneErrorsProvider.get();
				this.cards = cardsProvider.get();
			}

			public CardResult card(Card.Printing pr) {
				return cards.computeIfAbsent(pr, x -> new CardResult());
			}

			public Set<String> zoneErrors(Zone zone) {
				return zoneErrors.computeIfAbsent(zone, x -> new HashSet<>());
			}

			public boolean empty() {
				return deckErrors.isEmpty() && zoneErrors.values().stream().allMatch(Set::isEmpty) && cards.values().stream().allMatch(CardResult::empty);
			}

			public Result merge(Result other) {
				this.deckErrors.addAll(other.deckErrors);

				for (Zone z : this.zoneErrors.keySet()) if (other.zoneErrors.containsKey(z)) this.zoneErrors.get(z).addAll(other.zoneErrors.get(z));
				for (Zone z : other.zoneErrors.keySet()) if (!this.zoneErrors.containsKey(z)) this.zoneErrors.put(z, new HashSet<>(other.zoneErrors.get(z)));

				for (Card.Printing pr : this.cards.keySet()) if (other.cards.containsKey(pr)) this.cards.get(pr).merge(other.cards.get(pr));
				for (Card.Printing pr : other.cards.keySet()) if (!this.cards.containsKey(pr)) this.cards.put(pr, new CardResult(other.cards.get(pr)));

				return this;
			}
		}
	}

	/**
	 * Validates a deck according to this format's card legality and deck construction rules.
	 * @param deck The deck to validate.
	 * @return A map containing any validation errors. Errors associated with the deck in general (
	 */
	public Validator.Result validate(Deck deck) {
		Validator.Result result = new Validator.Result();
		result = cardCount.validate(deck, this, result);
		if (validator != null) result = validator.validate(deck, this, result);
		return result;
	}
}

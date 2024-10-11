package emi.lib.mtg.game;

import emi.lib.mtg.Card;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.game.ability.pregame.CopyLimit;
import emi.lib.mtg.game.validation.Companions;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Format {
	Freeform (-1, 0, -1, ZoneInfo.FREEFORM, null),
	Standard,
	Future,
	Pioneer,
	Modern,
	Legacy,
	Vintage,
	Brawl(1, 60, 60, ZoneInfo.BRAWL, emi.lib.mtg.game.validation.Commander.INSTANCE),
	Historic,
	HistoricBrawl(1, 60, 60, ZoneInfo.BRAWL, emi.lib.mtg.game.validation.Commander.INSTANCE),
	Pauper,
	Penny,
	Commander(1, 100, 100, ZoneInfo.COMMANDER, emi.lib.mtg.game.validation.Commander.INSTANCE),
	Alchemy,
	Explorer;

	public enum ZoneInfo {
		Unlimited (0, -1),
		ConstructedLibrary(60, -1),
		ConstructedSideboard(0, 15),
		BrawlLibrary(58, 59),
		CommanderLibrary(98, 99),
		CommandZone(1, 2),
		;

		ZoneInfo(int minCards, int maxCards) {
			this.minCards = minCards;
			this.maxCards = maxCards;
		}

		public final int minCards, maxCards;

		private static final Map<Zone, ZoneInfo> FREEFORM = freeformFormatZoneInfo();
		private static Map<Zone, ZoneInfo> freeformFormatZoneInfo() {
			Map<Zone, ZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, ZoneInfo.Unlimited);
			tmp.put(Zone.Sideboard, ZoneInfo.Unlimited);
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, ZoneInfo> BASIC = basicFormatZoneInfo();
		private static Map<Zone, ZoneInfo> basicFormatZoneInfo() {
			Map<Zone, ZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, ZoneInfo.ConstructedLibrary);
			tmp.put(Zone.Sideboard, ZoneInfo.ConstructedSideboard);
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, ZoneInfo> BRAWL = brawlFormatZoneInfo();
		private static Map<Zone, ZoneInfo> brawlFormatZoneInfo() {
			Map<Zone, ZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, ZoneInfo.BrawlLibrary);
			tmp.put(Zone.Command, ZoneInfo.CommandZone);
			tmp.put(Zone.Sideboard, ZoneInfo.Unlimited);
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, ZoneInfo> COMMANDER = commanderFormatZoneInfo();
		private static Map<Zone, ZoneInfo> commanderFormatZoneInfo() {
			Map<Zone, ZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, ZoneInfo.CommanderLibrary);
			tmp.put(Zone.Command, ZoneInfo.CommandZone);
			tmp.put(Zone.Sideboard, ZoneInfo.Unlimited);
			return Collections.unmodifiableMap(tmp);
		}
	}

	public final int maxCopies;
	public final Map<Zone, ZoneInfo> zones;
	private final BiConsumer<Deck, ValidationResult> validator;

	Format() {
		this(4, 60, -1, ZoneInfo.BASIC, Companions.INSTANCE);
	}

	Format(int maxCopies, int minDeckSize, int maxDeckSize, Map<Zone, ZoneInfo> zones, BiConsumer<Deck, ValidationResult> validator) {
		this.maxCopies = maxCopies;
		this.zones = Collections.unmodifiableMap(zones);
		this.validator = validator;
	}

	public Set<Zone> deckZones() {
		return zones.keySet();
	}

	public static class ValidationResult {
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

		public static final ValidationResult EMPTY = new ValidationResult(Collections::emptySet, Collections::emptyMap, Collections::emptyMap);

		public final Set<String> deckErrors;
		public final Map<Zone, Set<String>> zoneErrors;
		public final Map<Card.Printing, CardResult> cards;

		public ValidationResult() {
			this.deckErrors = new HashSet<>();
			this.zoneErrors = new HashMap<>();
			this.cards = new HashMap<>();
		}

		public ValidationResult(ValidationResult other) {
			this();
			merge(other);
		}

		public ValidationResult(Supplier<Set<String>> deckErrorsProvider, Supplier<Map<Zone, Set<String>>> zoneErrorsProvider, Supplier<Map<Card.Printing, CardResult>> cardsProvider) {
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

		public ValidationResult merge(ValidationResult other) {
			this.deckErrors.addAll(other.deckErrors);

			for (Zone z : this.zoneErrors.keySet()) if (other.zoneErrors.containsKey(z)) this.zoneErrors.get(z).addAll(other.zoneErrors.get(z));
			for (Zone z : other.zoneErrors.keySet()) if (!this.zoneErrors.containsKey(z)) this.zoneErrors.put(z, new HashSet<>(other.zoneErrors.get(z)));

			for (Card.Printing pr : this.cards.keySet()) if (other.cards.containsKey(pr)) this.cards.get(pr).merge(other.cards.get(pr));
			for (Card.Printing pr : other.cards.keySet()) if (!this.cards.containsKey(pr)) this.cards.put(pr, new CardResult(other.cards.get(pr)));

			return this;
		}
	}

	/**
	 * Validates a deck according to this format's card legality and deck construction rules.
	 * @param deck The deck to validate.
	 * @return A map containing any validation errors. Errors associated with the deck in general (
	 */
	public ValidationResult validate(Deck deck) {
		ValidationResult result = new ValidationResult();

		Map<String, AtomicInteger> histogram = new HashMap<>();

		for (Zone zone : deckZones()) {
			ZoneInfo fzi = zones.get(zone);
			Collection<? extends Card.Printing> ciz = deck.cards(zone) == null ? Collections.emptyList() : deck.cards(zone);

			ciz.stream()
					.peek(pr -> histogram.computeIfAbsent(pr.card().name(), n -> new AtomicInteger(0)).incrementAndGet())
					.forEach(pr -> {
						switch (pr.card().legality(Format.this)) {
							case Banned:
								result.card(pr).errors.add(String.format("%s is banned in %s!", pr.card().name(), Format.this.name()));
								break;
							case NotLegal:
								if (pr.releaseDate().isAfter(LocalDate.now())) {
									result.card(pr).warnings.add(String.format("%s has not released yet.", pr.card().name()));
								} else {
									result.card(pr).errors.add(String.format("%s is not legal in %s.", pr.card().name(), Format.this.name()));
								}
								break;
							case Restricted:
								if (histogram.get(pr.card().name()).get() > 1) {
									result.card(pr).errors.add(String.format("%s is restricted to one copy per deck in %s.", pr.card().name(), Format.this.name()));
								}
								break;
							case Legal:
								break;
							case Unknown:
								result.card(pr).warnings.add(String.format("Couldn't verify legality of %s in %s.", pr.card().name(), Format.this.name()));
								break;
						}
					});

			ciz.forEach(pr -> {
				if (!pr.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
					int min = 0, max = maxCopies;
					if (pr.card().front() != null) {
						CopyLimit override = pr.card().front().abilities().only(CopyLimit.class);
						if (override != null) {
							min = override.min;
							max = override.max;
						}
					}

					if (min > 0 && histogram.get(pr.card().name()).get() < min) {
						result.card(pr).errors.add(String.format("In %s, a deck must contain no fewer than %d cop%s of %s.",
								Format.this.name(),
								min,
								min == 1 ? "y" : "ies",
								pr.card().name()));
					}

					if (max > 0 && histogram.get(pr.card().name()).get() > max) {
						result.card(pr).errors.add(String.format("In %s, a deck can contain no more than %d cop%s of %s.",
								Format.this.name(),
								max,
								max == 1 ? "y" : "ies",
								pr.card().name()));
					}
				}
			});

			if (fzi.minCards > 0 && ciz.size() < fzi.minCards) {
				result.zoneErrors(zone).add(String.format("In %s, the %s zone must contain at least %d cards.",
						Format.this.name(),
						zone.name(),
						fzi.minCards));
			} else if (fzi.maxCards > 0 && ciz.size() > fzi.maxCards) {
				result.zoneErrors(zone).add(String.format("In %s, the %s zone may contain no more than %d cards.",
						Format.this.name(),
						zone.name(),
						fzi.maxCards));
			}
		}

		if (validator != null) validator.accept(deck, result);

		return result;
	}
}

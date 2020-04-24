package emi.lib.mtg.game;

import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.validation.CommandZone;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public enum Format {
	Freeform (-1, FormatZoneInfo.FREEFORM, null),
	Standard,
	Future,
	Pioneer,
	Modern,
	Legacy,
	Vintage,
	Brawl(1, FormatZoneInfo.BRAWL, CommandZone.INSTANCE),
	Historic,
	Pauper,
	Penny,
	Commander(1, FormatZoneInfo.COMMANDER, CommandZone.INSTANCE);

	private static class FormatZoneInfo {
		public FormatZoneInfo(int minCards, int maxCards) {
			this.minCards = minCards;
			this.maxCards = maxCards;
		}

		public final int minCards, maxCards;

		private static final Map<Zone, FormatZoneInfo> FREEFORM = freeformFormatZoneInfo();
		private static Map<Zone, FormatZoneInfo> freeformFormatZoneInfo() {
			Map<Zone, FormatZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, new FormatZoneInfo(0, -1));
			tmp.put(Zone.Sideboard, new FormatZoneInfo(0, -1));
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, FormatZoneInfo> BASIC = basicFormatZoneInfo();
		private static Map<Zone, FormatZoneInfo> basicFormatZoneInfo() {
			Map<Zone, FormatZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, new FormatZoneInfo(60, -1));
			tmp.put(Zone.Sideboard, new FormatZoneInfo(0, 15));
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, FormatZoneInfo> BRAWL = brawlFormatZoneInfo();
		private static Map<Zone, FormatZoneInfo> brawlFormatZoneInfo() {
			Map<Zone, FormatZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, new FormatZoneInfo(59,59));
			tmp.put(Zone.Command, new FormatZoneInfo(1,1));
			return Collections.unmodifiableMap(tmp);
		}

		private static final Map<Zone, FormatZoneInfo> COMMANDER = commanderFormatZoneInfo();
		private static Map<Zone, FormatZoneInfo> commanderFormatZoneInfo() {
			Map<Zone, FormatZoneInfo> tmp = new EnumMap<>(Zone.class);
			tmp.put(Zone.Library, new FormatZoneInfo(98,99));
			tmp.put(Zone.Command, new FormatZoneInfo(1,2));
			return Collections.unmodifiableMap(tmp);
		}
	}

	public final int maxCopies;
	private final Map<Zone, FormatZoneInfo> zones;
	private final BiConsumer<Deck, ValidationResult> validator;

	Format() {
		this(4, FormatZoneInfo.BASIC, null);
	}

	Format(int maxCopies, Map<Zone, FormatZoneInfo> zones, BiConsumer<Deck, ValidationResult> validator) {
		this.maxCopies = maxCopies;
		this.zones = Collections.unmodifiableMap(zones);
		this.validator = validator;
	}

	public Set<Zone> deckZones() {
		return zones.keySet();
	}

	public class ValidationResult {
		public final Set<String> deckErrors;
		public final Map<Zone, Set<String>> zoneErrors;
		public final Map<Card.Printing, Set<String>> cardErrors;

		private ValidationResult() {
			this.deckErrors = new HashSet<>();
			this.zoneErrors = new HashMap<>();
			for (Zone zone : deckZones()) {
				this.zoneErrors.put(zone, new HashSet<>());
			}
			this.cardErrors = new HashMap<>();
		}

		public Set<String> cardErrors(Card.Printing pr) {
			return cardErrors.computeIfAbsent(pr, x -> new HashSet<>());
		}

		public Set<String> zoneErrors(Zone zone) {
			return zoneErrors.computeIfAbsent(zone, x -> new HashSet<>());
		}
	}

	private static final Pattern COUNT_PATTERN = Pattern.compile("A deck can have (?<any>any number of|up to (?<numword>[- a-z0-9]+)) cards named");

	private static int numberWordToInt(String str) {
		// TODO: Move this to a utility library or something. Heck.
		if ("seven".equals(str)) {
			return 7;
		}
		return -1;
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
			FormatZoneInfo fzi = zones.get(zone);
			Collection<? extends Card.Printing> ciz = deck.cards(zone) == null ? Collections.emptyList() : deck.cards(zone);

			ciz.stream()
					.peek(pr -> histogram.computeIfAbsent(pr.card().name(), n -> new AtomicInteger(0)).incrementAndGet())
					.forEach(pr -> {
						switch (pr.card().legality(Format.this)) {
							case Banned:
								result.cardErrors(pr).add(String.format("%s is banned in %s!", pr.card().name(), Format.this.name()));
								break;
							case NotLegal:
								result.cardErrors(pr).add(String.format("%s is not legal in %s.", pr.card().name(), Format.this.name()));
								break;
							case Restricted:
								if (histogram.get(pr.card().name()).get() > 1) {
									result.cardErrors(pr).add(String.format("%s is restricted to one copy per deck in %s.", pr.card().name(), Format.this.name()));
								}
								break;
							case Legal:
								break;
							case Unknown:
								result.cardErrors(pr).add(String.format("Couldn't verify legality of %s in %s.", pr.card().name(), Format.this.name()));
								break;
						}

						if (!pr.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
							int max = maxCopies;
							if (pr.card().face(Card.Face.Kind.Front) != null) {
								Matcher countMatcher = COUNT_PATTERN.matcher(pr.card().face(Card.Face.Kind.Front).rules());
								if (countMatcher.find()) {
									max = countMatcher.group("numword") != null ? numberWordToInt(countMatcher.group("numword")) : -1;
								}
							}

							if (max > 0 && histogram.get(pr.card().name()).get() > max) {
								result.cardErrors(pr).add(String.format("In %s, a deck can contain no more than %d cop%s of %s.",
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

package emi.lib.mtg.game;

import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;

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
	Brawl(1, FormatZoneInfo.BRAWL, Validators.COMMAND_ZONE),
	Historic,
	Pauper,
	Penny,
	Commander(1, FormatZoneInfo.COMMANDER, Validators.COMMAND_ZONE);

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

	private static class Validators {
		private static final Pattern PARTNER_PATTERN = Pattern.compile("(?<legendary>Legendary )?[Pp]artner(?: with (?<with>[-A-Za-z0-9 ,]+))?");

		private static boolean isCreature(Card.Printing pr) {
			Card.Face front = pr.card().face(Card.Face.Kind.Front);
			return front != null && front.type().cardTypes().contains(CardType.Creature);
		}

		private static void validateCommander(ValidationResult result, Collection<? extends Card.Printing> cmdZone, Card.Printing cmdr) {
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

		private static final BiConsumer<Deck, ValidationResult> COMMAND_ZONE = (deck, result) -> {
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
		};
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

		private Set<String> cardErrors(Card.Printing pr) {
			return cardErrors.computeIfAbsent(pr, x -> new HashSet<>());
		}

		private Set<String> zoneErrors(Zone zone) {
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

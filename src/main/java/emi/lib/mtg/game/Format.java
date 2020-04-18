package emi.lib.mtg.game;

import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		private static Pattern partner = Pattern.compile("(?<legendary>Legendary )?[Pp]artner(?: with (?<partner>[-A-Za-z0-9 ,]+))?(?: \\(.*\\))?");

		private static boolean commanderIsLegal(Card.Face front) {
			return front.rules().contains("can be your commander.") || (front.type().supertypes().contains(Supertype.Legendary) && front.type().cardTypes().contains(CardType.Creature));
		}

		private static final BiConsumer<Deck, ValidationResult> COMMAND_ZONE = (deck, result) -> {
			Collection<? extends Card.Printing> commanders = deck.cards(Zone.Command) == null ? Collections.emptyList() : deck.cards(Zone.Command);
			Collection<? extends Card.Printing> library = deck.cards(Zone.Library) == null ? Collections.emptyList() : deck.cards(Zone.Library);

			Set<Color> cmdrColors = EnumSet.of(Color.COLORLESS);
			if (commanders.size() == 1) {
				Card.Printing cmdr = commanders.iterator().next();
				Card.Face face = cmdr.card().face(Card.Face.Kind.Front);
				CardTypeLine ctl = face.type();

				if (!commanderIsLegal(face)) {
					result.cardErrors.computeIfAbsent(cmdr, s -> new HashSet<>()).add(String.format("%s is not a legal commander.", face.name()));
				}

				cmdrColors.addAll(face.colorIdentity());
			} else if (commanders.size() == 2) {
				Iterator<? extends Card.Printing> iter = commanders.iterator();
				Card.Printing cmdr1 = iter.next(), cmdr2 = iter.next();
				Card.Face face1 = cmdr1.card().face(Card.Face.Kind.Front), face2 = cmdr2.card().face(Card.Face.Kind.Front);
				Matcher match1 = partner.matcher(face1.rules()), match2 = partner.matcher(face2.rules());
				boolean found1 = match1.find(), found2 = match2.find();

				if ((found1 || found2) && (found1 ? match1 : match2).group("legendary") != null) {
					Card.Printing cmdrMain = found1 ? cmdr1 : cmdr2, cmdrOther = found1 ? cmdr2 : cmdr1;
					Card.Face faceMain = found1 ? face1 : face2, faceOther = found1 ? face2 : face1;

					if (!commanderIsLegal(faceMain)) {
						result.cardErrors.computeIfAbsent(cmdrMain, pr -> new HashSet<>()).add(String.format("%s is not a legal commander.", faceMain.name()));
					}
					if (!faceOther.type().cardTypes().contains(CardType.Creature)) {
						result.cardErrors.computeIfAbsent(cmdrOther, pr -> new HashSet<>()).add(String.format("%s must be partnered with a creature card.", faceMain.name()));
					}
				} else if (found1 && found2) {
					if (match1.group("partner") != null && !match1.group("partner").trim().equals(face2.name())) {
						result.cardErrors.computeIfAbsent(cmdr2, pr -> new HashSet<>()).add(String.format("%s is partners with %s, not %s.", face1.name(), match1.group("partner").trim(), face2.name()));
					}
					if (match2.group("partner") != null && !match2.group("partner").trim().equals(face1.name())) {
						result.cardErrors.computeIfAbsent(cmdr1, pr -> new HashSet<>()).add(String.format("%s is partners with %s, not %s.", face2.name(), match2.group("partner").trim(), face1.name()));
					}
					if (!commanderIsLegal(face1)) {
						result.cardErrors.computeIfAbsent(cmdr1, pr -> new HashSet<>()).add(String.format("%s is not a legal commander.", face1.name()));
					}
					if (!commanderIsLegal(face2)) {
						result.cardErrors.computeIfAbsent(cmdr2, pr -> new HashSet<>()).add(String.format("%s is not a legal commander.", face2.name()));
					}
				} else {
					result.zoneErrors.get(Zone.Command).add("You can have two commanders only if both have partner.");
				}

				cmdrColors.addAll(face1.colorIdentity());
				cmdrColors.addAll(face2.colorIdentity());
			} else {
				result.zoneErrors.get(Zone.Command).add("You must have exactly one or two commanders.");
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
	}

	private static final Pattern countPattern = Pattern.compile("A deck can have (?<any>any number of|up to (?<numword>[- a-z0-9]-)) cards named");

	private static int numberWordToInt(String str) {
		// TODO: Move this to a utility library or something. Heck.
		switch(str) {
			case "seven":
				return 7;
			default:
				return -1;
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
			FormatZoneInfo fzi = zones.get(zone);
			Collection<? extends Card.Printing> ciz = deck.cards(zone) == null ? Collections.emptyList() : deck.cards(zone);

			ciz.stream()
					.peek(pr -> histogram.computeIfAbsent(pr.card().name(), n -> new AtomicInteger(0)).incrementAndGet())
					.forEach(pr -> {
						Set<String> messages = result.cardErrors.containsKey(pr) ? result.cardErrors.get(pr) : new HashSet<>();
						switch (pr.card().legality(Format.this)) {
							case Banned:
								messages.add(String.format("%s is banned in %s!", pr.card().name(), Format.this.name()));
								break;
							case NotLegal:
								messages.add(String.format("%s is not legal in %s.", pr.card().name(), Format.this.name()));
								break;
							case Restricted:
								if (histogram.get(pr.card().name()).get() > 1) {
									messages.add(String.format("%s is restricted to one copy per deck in %s.", pr.card().name(), Format.this.name()));
								}
								break;
							case Legal:
								break;
							case Unknown:
								messages.add(String.format("Couldn't verify legality of %s in %s.", pr.card().name(), Format.this.name()));
								break;
						}

						if (!pr.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
							int max = maxCopies;
							if (pr.card().face(Card.Face.Kind.Front) != null) {
								Matcher countMatcher = countPattern.matcher(pr.card().face(Card.Face.Kind.Front).rules());
								if (countMatcher.find()) {
									max = countMatcher.group("numword") != null ? numberWordToInt(countMatcher.group("numword")) : -1;
								}
							}

							if (pr.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
								max = -1;
							}

							if (max >= 0 && histogram.get(pr.card().name()).get() > maxCopies) {
								messages.add(String.format("In %s, a deck can contain no more than %d copies of %s.", Format.this.name(), maxCopies, pr.card().name()));
							}
						}

						if (!messages.isEmpty()) {
							result.cardErrors.put(pr, messages);
						}
					});

			if (ciz.size() < fzi.minCards || ciz.size() > fzi.maxCards) {
				Set<String> zoneMessages = result.zoneErrors.computeIfAbsent(zone, z -> new HashSet<>());
				if (fzi.minCards == fzi.maxCards) {
					zoneMessages.add(String.format("In %s, the %s zone must contain exactly %d cards.", Format.this.name(), zone.name(), fzi.minCards));
				} else {
					zoneMessages.add(String.format("In %s, the %s zone must contain between %d and %d cards.", Format.this.name(), zone.name(), fzi.minCards, fzi.maxCards));
				}
			}
		}

		if (validator != null) validator.accept(deck, result);

		return result;
	}
}

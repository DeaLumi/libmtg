package emi.lib.mtg.game.impl.formats;

import emi.lib.Service;
import emi.lib.mtg.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="EDH")
public class EDH extends AbstractFormat {
	private static final String BAD_CMD_COUNT = "An EDH-legal deck's command zone must contain either one legendary creature, or two legendary creatures with Partner.";

	private static final Set<String> BANLIST = banlist();

	private static Set<String> banlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Ancestral Recall",
				"Balance",
				"Biorhythm",
				"Black Lotus",
				"Braids, Cabal Minion",
				"Channel",
				"Coalition Victory",
				"Emrakul, the Aeons Torn",
				"Erayo, Soratami Ascendant",
				"Fastbond",
				"Gifts Ungiven",
				"Griselbrand",
				"Karakas",
				"Leovold, Emissary of Trest",
				"Library of Alexandria",
				"Limited Resources",
				"Mox Emerald",
				"Mox Jet",
				"Mox Pearl",
				"Mox Ruby",
				"Mox Sapphire",
				"Painter's Servant",
				"Panoptic Mirror",
				"Primeval Titan",
				"Prophet of Kruphix",
				"Recurring Nightmare",
				"Rofellos, Llanowar Emissary",
				"Sundering Titan",
				"Sway of the Stars",
				"Sylvan Primordial",
				"Time Vault",
				"Time Walk",
				"Tinker",
				"Tolarian Academy",
				"Trade Secrets",
				"Upheaval",
				"Worldfire",
				"Yawgmoth's Bargain"
		).collect(Collectors.toSet()));
	}

	@Override
	public String name() {
		return "EDH";
	}

	// TODO: Either include sideboard here, or force it to zero in min/maxCards.
	@Override
	public Set<Zone> deckZones() {
		return EnumSet.of(Zone.Library, Zone.Command);
	}

	@Override
	public int minCards(Zone zone) {
		switch (zone) {
			case Library:
				return 98;
			case Hand:
			case Battlefield:
			case Graveyard:
			case Stack:
			case Exile:
				return 0;
			case Command:
				return 1;
			case Sideboard:
				return 0;
			default:
				assert false;
				return 0;
		}
	}

	@Override
	public int maxCards(Zone zone) {
		switch (zone) {
			case Library:
				return 99;
			case Hand:
			case Battlefield:
			case Graveyard:
			case Stack:
			case Exile:
				return 0;
			case Command:
				return 2;
			case Sideboard:
				return -1;
			default:
				assert false;
				return 0;
		}
	}

	@Override
	public int minCards() {
		return 100;
	}

	@Override
	public int maxCards() {
		return 115;
	}

	@Override
	public int maxCardCopies() {
		return 1;
	}

	@Override
	public boolean setIsLegal(emi.lib.mtg.Set set) {
		return true;
	}

	@Override
	public boolean cardIsBanned(Card card) {
		return Vintage.BANLIST.contains(card.name()) || BANLIST.contains(card.name());
	}

	private static String isLegalCommander(Card.Face cmdr) {
		if (!cmdr.type().supertypes().contains(Supertype.Legendary)) {
			return String.format("%s is not a legal commander.", cmdr.name());
		}

		if (!cmdr.type().cardTypes().contains(CardType.Creature) && !cmdr.rules().contains("can be your commander.")) {
			return String.format("%s is not a legal commander.", cmdr.name());
		}

		return null; // Null is good here. It means no reason to reject this commander.
	}

	private static Pattern partner = Pattern.compile("Partner(?: with (?<partner>[-A-Za-z0-9 ,]+))?(?: \\(.*\\))?");

	private static String canBePartners(Card.Face cmdr1, Card.Face cmdr2) {
		Matcher m1 = partner.matcher(cmdr1.rules());

		if (!m1.find()) {
			return String.format("%s does not have Partner.", cmdr1.name());
		}

		Matcher m2 = partner.matcher(cmdr2.rules());

		if (!m2.find()) {
			return String.format("%s does not have Partner.", cmdr2.name());
		}

		if (m1.group("partner") != null && !m1.group("partner").equals(cmdr2.name())) {
			return String.format("%s can only be partnered with %s, not %s.", cmdr1.name(), m1.group("partner"), cmdr2.name());
		}

		if (m2.group("partner") != null && !m2.group("partner").equals(cmdr1.name())) {
			return String.format("%s can only be partnered with %s, not %s.", cmdr2.name(), m2.group("partner"), cmdr1.name());
		}

		return null;
	}

	@Override
	protected Set<String> furtherValidation(Deck.Variant variant) {
		Set<String> messages = new HashSet<>();

		Collection<? extends Card.Printing> cmd = variant.cards(Zone.Command);
		Collection<? extends Card.Printing> lib = variant.cards(Zone.Library);

		if (cmd == null) {
			cmd = Collections.emptyList();
		}

		Card.Face cmdr1, cmdr2;
		switch (cmd.size()) {
			case 1: {
				cmdr1 = cmd.iterator().next().card().face(Card.Face.Kind.Front);
				cmdr2 = null;
				break;
			}
			case 2: {
				Iterator<? extends Card.Printing> it = cmd.iterator();
				cmdr1 = it.next().card().face(Card.Face.Kind.Front);
				cmdr2 = it.next().card().face(Card.Face.Kind.Front);
				break;
			}
			default: {
				cmdr1 = null;
				cmdr2 = null;
				break;
			}
		}

		if (cmdr1 != null) {
			String badCmdr1 = isLegalCommander(cmdr1);
			if (badCmdr1 != null) {
				messages.add(badCmdr1);
			}
		} else {
			messages.add(BAD_CMD_COUNT);
		}

		if (cmdr2 != null) {
			String badCmdr2 = isLegalCommander(cmdr2);
			if (badCmdr2 != null) {
				messages.add(badCmdr2);
			} else {
				badCmdr2 = canBePartners(cmdr1, cmdr2);
				if (badCmdr2 != null) {
					messages.add(badCmdr2);
				}
			}
		}

		if (lib == null) {
			lib = Collections.emptyList();
		}

		Set<Color> deckCI = cmd.stream().collect(() -> EnumSet.noneOf(Color.class), (s, c) -> s.addAll(c.card().colorIdentity()), Set::addAll);
		deckCI.add(Color.COLORLESS);

		for (Card.Printing c : lib) {
			if (!deckCI.containsAll(c.card().colorIdentity())) {
				messages.add(String.format("%s contains colors not in your commander's color identity!", c.card().name()));
			}
		}

		return messages;
	}
}

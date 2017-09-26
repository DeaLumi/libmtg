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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="EDH")
public class EDH extends AbstractFormat {
	private static final String BAD_CMD_COUNT = "An EDH-legal deck's command zone must contain either one legendary creature, or two legendary creatures with Partner.";
	private static final String COLOR_IDENTITY_ERROR = "An EDH-legal deck's library zone can't contain cards with color identities outside that of the deck's commanders.";

	private static final Set<String> BANLIST = banlist();

	private static Set<String> banlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Ancestral Recall",
				"Balance",
				"Biorhythm",
				"Black Lotus",
				"Braids, Cabal Minion",
				"Coalition Victory",
				"Channel",
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
				"Sway of the Stars",
				"Sundering Titan",
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
	protected int minCards(Zone zone) {
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
	protected int maxCards(Zone zone) {
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
	protected int minCards() {
		return 100;
	}

	@Override
	protected int maxCards() {
		return 115;
	}

	@Override
	protected int maxCardCopies() {
		return 1;
	}

	@Override
	protected boolean setIsLegal(emi.lib.mtg.Set set) {
		return true;
	}

	@Override
	protected boolean cardIsBanned(Card card) {
		return Vintage.BANLIST.contains(card.name()) || BANLIST.contains(card.name());
	}

	@Override
	protected Set<String> furtherValidation(Deck.Variant variant) {
		Set<String> messages = new HashSet<>();

		Collection<? extends Card.Printing> cmd = variant.cards(Zone.Command);
		Collection<? extends Card.Printing> lib = variant.cards(Zone.Library);

		if (cmd == null) {
			cmd = Collections.emptyList();
			messages.add(BAD_CMD_COUNT);
		} else if (cmd.size() > 2) {
			messages.add(BAD_CMD_COUNT);
		} else {
			for (Card.Printing pr : cmd) {
				Card.Face front = pr.card().face(Card.Face.Kind.Front);
				if (front == null ||
						!front.type().supertypes().contains(Supertype.Legendary) ||
						!front.type().cardTypes().contains(CardType.Creature) ||
						(cmd.size() != 1 && !front.rules().contains("Partner"))) {
					messages.add(BAD_CMD_COUNT);
					break;
				}
			}
		}

		if (lib == null) {
			lib = Collections.emptyList();
		}

		Set<Color> deckCI = cmd.stream().collect(() -> EnumSet.noneOf(Color.class), (s, c) -> s.addAll(c.card().colorIdentity()), Set::addAll);

		if (!lib.stream().allMatch(c -> deckCI.containsAll(c.card().colorIdentity()))) {
			messages.add(COLOR_IDENTITY_ERROR);
		}

		return messages;
	}
}

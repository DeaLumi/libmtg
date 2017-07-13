package emi.lib.mtg.game.formats;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="EDH")
public class EDH extends AbstractFormat {
	public static final String BAD_CMD_COUNT = "An EDH-legal deck's command zone must contain either one legendary creature, or two legendary creatures with Partner.";
	public static final String COLOR_IDENTITY_ERROR = "An EDH-legal deck's library zone can't contain cards with color identities outside that of the deck's commanders.";

	@Override
	public String name() {
		return "EDH";
	}

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
	protected int minSideboard() {
		return 0;
	}

	@Override
	protected int maxSideboard() {
		return 15;
	}

	@Override
	protected int maxCardCopies() {
		return 1;
	}

	@Override
	protected Set<String> furtherValidation(Deck deck) {
		Set<String> messages = new HashSet<>();

		List<? extends Card> cmd = deck.cards().get(Zone.Command);
		List<? extends Card> lib = deck.cards().get(Zone.Library);

		if (cmd == null) {
			cmd = Collections.emptyList();
			messages.add(BAD_CMD_COUNT);
		} else if (cmd.size() > 2
				|| (cmd.size() == 2 && !cmd.stream().allMatch(c -> c.front().text().contains("Partner")))
				|| !cmd.stream().map(c -> c.front().type()).allMatch(t -> t.supertypes().contains(Supertype.Legendary) && t.cardTypes().contains(CardType.Creature))) {
			messages.add(BAD_CMD_COUNT);
		}

		if (lib == null) {
			lib = Collections.emptyList();
		}

		Set<Color> deckCI = cmd.stream().collect(() -> EnumSet.noneOf(Color.class), (s, c) -> s.addAll(c.colorIdentity()), Set::addAll);

		if (!Stream.concat(lib.stream(), deck.sideboard().stream())
				.allMatch(c -> deckCI.containsAll(c.colorIdentity()))) {
			messages.add(COLOR_IDENTITY_ERROR);
		}

		return messages;
	}
}

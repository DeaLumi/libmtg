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
public class EDH implements Format {
	public static final String BAD_CARD_COUNT = "An EDH-legal deck must contain exactly 100 cards.";
	public static final String ILLEGAL_ZONES = "An EDH-legal deck can only have cards in the library zone, command zone, and sideboard.";
	public static final String BAD_CMD_COUNT = "An EDH-legal deck's command zone must contain either one legendary creature, or two legendary creatures with Partner.";
	public static final String TOO_MANY_COPIES = "An EDH-legal deck and sideboard can contain no more than 1 of any card with a particular English name, other than basic lands.";
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
	public Set<String> validate(Deck deck) {
		Set<String> messages = new HashSet<>();

		for (Zone zone : Zone.values()) {
			if (deckZones().contains(zone)) {
				if (!deck.cards().containsKey(zone)) {
					messages.add(BAD_CARD_COUNT);
				}
			} else {
				if (deck.cards().containsKey(zone) && !deck.cards().get(zone).isEmpty()) {
					messages.add(ILLEGAL_ZONES);
				}
			}
		}

		List<Card> cmd = deck.cards().get(Zone.Command);
		List<Card> lib = deck.cards().get(Zone.Library);

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

		if (cmd.size() + lib.size() != 100) {
			messages.add(BAD_CARD_COUNT);
		}

		Set<Color> deckCI = cmd.stream().collect(() -> EnumSet.noneOf(Color.class), (s, c) -> s.addAll(c.colorIdentity()), Set::addAll);
		Set<String> cards = new HashSet<>();

		Iterator<Card> iter = Stream.concat(lib.stream(), deck.sideboard().stream())
				.filter(c -> !c.front().type().supertypes().contains(Supertype.Basic) && !c.front().type().cardTypes().contains(CardType.Land))
				.iterator();

		while (iter.hasNext()) {
			Card next = iter.next();
			if (cards.contains(next.name())) {
				messages.add(TOO_MANY_COPIES);
			} else {
				cards.add(next.name());
			}

			if (!deckCI.containsAll(next.colorIdentity())) {
				messages.add(COLOR_IDENTITY_ERROR);
			}
		}

		return messages;
	}
}

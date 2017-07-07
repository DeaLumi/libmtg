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

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="EDH")
public class EDH implements Format {
	public static final String BAD_CARD_COUNT = "An EDH-legal deck must contain exactly 100 cards.";
	public static final String BAD_CMD_COUNT = "An EDH-legal deck's command zone must contain either one legendary creature, or two legendary creatures with Partner.";
	public static final String TOO_MANY_COPIES = "An EDH-legal deck and sideboard can contain no more than 1 of any card with a particular English name, other than basic lands.";

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

		List<Card> cmd = deck.cards().get(Zone.Command);
		List<Card> lib = deck.cards().get(Zone.Library);

		if (cmd == null) {
			cmd = Collections.emptyList();
			messages.add(BAD_CMD_COUNT);
		} else if (cmd.size() > 2
				|| !cmd.stream().allMatch(c -> c.front().text().contains("Partner"))
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
		Set<Card> cards = new HashSet<>();

		for (Card c : lib) {
			if (c.front().type().supertypes().contains(Supertype.Basic) && c.front().type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			if (cards.contains(c) || !deckCI.containsAll(c.colorIdentity())) {
				messages.add(TOO_MANY_COPIES);
				break;
			} else {
				cards.add(c);
			}
		}

		if (!messages.contains(TOO_MANY_COPIES)) {
			for (Card c : deck.sideboard()) {
				if (c.front().type().supertypes().contains(Supertype.Basic) && c.front().type().cardTypes().contains(CardType.Land)) {
					continue;
				}

				if (cards.contains(c) || !deckCI.containsAll(c.colorIdentity())) {
					messages.add(TOO_MANY_COPIES);
					break;
				} else {
					cards.add(c);
				}
			}
		}

		return messages;
	}
}

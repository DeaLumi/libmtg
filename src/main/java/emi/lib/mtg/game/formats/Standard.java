package emi.lib.mtg.game.formats;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="Standard")
public class Standard implements Format {
	public static final String TOO_FEW_CARDS = "A Standard-legal deck must contain at least 60 cards.";
	public static final String ILLEGAL_ZONES = "A Standard-legal deck can only have cards in the library and sideboard.";
	public static final String SIDEBOARD_TOO_LARGE = "A Standard-legal sideboard may contain no more than 15 cards.";
	public static final String TOO_MANY_COPIES = "A Standard-legal deck and sideboard can contain no more than 4 of any card with a particular English name, other than basic lands.";

	@Override
	public String name() {
		return "Standard";
	}

	@Override
	public Set<Zone> deckZones() {
		return Collections.singleton(Zone.Library);
	}

	// TODO: For now, we only validate a deck's approximate structure. Legality of cards isn't presently considered.
	@Override
	public Set<String> validate(Deck deck) {
		Set<String> messages = new HashSet<>();

		List<Card> lib = deck.cards().get(Zone.Library);
		if (lib == null) {
			lib = Collections.emptyList();
			messages.add(TOO_FEW_CARDS);

			if (deck.cards().size() != 0) {
				messages.add(ILLEGAL_ZONES);
			}
		} else if (deck.cards().size() != 1) {
			messages.add(ILLEGAL_ZONES);
		}

		if (deck.sideboard().size() > 15) {
			messages.add(SIDEBOARD_TOO_LARGE);
		}

		List<Card> cards = deck.cards().get(Zone.Library);

		if (cards.size() < 60) {
			messages.add(TOO_FEW_CARDS);
		}

		Map<String, AtomicInteger> histogram = new HashMap<>();

		for (Card c : cards) {
			if (c.front().type().supertypes().contains(Supertype.Basic) && c.front().type().cardTypes().contains(CardType.Land)) {
				continue;
			}

			if (histogram.computeIfAbsent(c.name(), k -> new AtomicInteger(0)).incrementAndGet() > 4) {
				messages.add(TOO_MANY_COPIES);
				break;
			}
		}

		if (!messages.contains(TOO_MANY_COPIES)) {
			for (Card c : deck.sideboard()) {
				if (c.front().type().supertypes().contains(Supertype.Basic) && c.front().type().cardTypes().contains(CardType.Land)) {
					continue;
				}

				if (histogram.computeIfAbsent(c.name(), k -> new AtomicInteger(0)).incrementAndGet() > 4) {
					messages.add(TOO_MANY_COPIES);
					break;
				}
			}
		}

		return messages;
	}
}

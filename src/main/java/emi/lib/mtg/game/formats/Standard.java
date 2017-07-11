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
import java.util.stream.Stream;

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

		for (Zone zone : Zone.values()) {
			if (deckZones().contains(zone)) {
				if (!deck.cards().containsKey(zone)) {
					messages.add(TOO_FEW_CARDS);
				}
			} else {
				if (deck.cards().containsKey(zone) && !deck.cards().get(zone).isEmpty()) {
					messages.add(ILLEGAL_ZONES);
				}
			}
		}

		List<? extends Card> lib = deck.cards().get(Zone.Library);
		if (lib == null) {
			lib = Collections.emptyList();
		}

		if (deck.sideboard().size() > 15) {
			messages.add(SIDEBOARD_TOO_LARGE);
		}

		if (lib.size() < 60) {
			messages.add(TOO_FEW_CARDS);
		}

		Map<String, AtomicInteger> histogram = new HashMap<>();

		Iterator<Card> iter = Stream.concat(lib.stream(), deck.sideboard().stream())
				.filter(c -> !c.front().type().supertypes().contains(Supertype.Basic) && !c.front().type().cardTypes().contains(CardType.Land))
				.iterator();

		while (iter.hasNext()) {
			if (histogram.computeIfAbsent(iter.next().name(), k -> new AtomicInteger(0)).incrementAndGet() > 4) {
				messages.add(TOO_MANY_COPIES);
				break;
			}
		}

		return messages;
	}
}

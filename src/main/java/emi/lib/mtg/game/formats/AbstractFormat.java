package emi.lib.mtg.game.formats;

import emi.lib.mtg.card.Card;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractFormat implements Format {
	protected abstract int minCards(Zone zone);

	protected abstract int maxCards(Zone zone);

	protected abstract int minCards();

	protected abstract int maxCards();

	protected abstract int minSideboard();

	protected abstract int maxSideboard();

	protected abstract int maxCardCopies();

	protected abstract Set<String> furtherValidation(Deck deck);

	@Override
	public Set<String> validate(Deck deck) {
		Set<String> messages = new HashSet<>();

		Map<String, AtomicInteger> histogram = new HashMap<>();
		int nCards = 0;

		for (Zone zone : deckZones()) {
			int min = minCards(zone);
			int max = maxCards(zone);

			List<? extends Card> cardsInZone = deck.cards().get(zone);
			if (cardsInZone == null) {
				cardsInZone = Collections.emptyList();
			}

			final int n = cardsInZone.size();

			if (min >= 0 && n < min) {
				messages.add(String.format("%s must contain no fewer than %d cards.", zone.name(), min));
			} else if (max >= 0 && n > max) {
				messages.add(String.format("%s must contain no more than %d cards.", zone.name(), max));
			}

			nCards += cardsInZone.size();

			for (Card card : cardsInZone) {
				if (card.faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
					continue;
				}

				if (histogram.computeIfAbsent(card.name(), c -> new AtomicInteger(0)).incrementAndGet() > maxCardCopies()) {
					messages.add(String.format("There must be no more than %d copies of %s.", maxCardCopies(), card.name()));
				}
			}
		}

		final int nSideboard = deck.sideboard().size();
		nCards += nSideboard;

		if (nSideboard < minSideboard()) {
			messages.add(String.format("Sideboard must contain no fewer than %d cards.", minSideboard()));
		} else if (nSideboard > maxSideboard()) {
			messages.add(String.format("Sideboard must contain no more than %d cards.", maxSideboard()));
		}

		for (Card card : deck.sideboard()) {
			if (card.faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
				continue;
			}

			if (histogram.computeIfAbsent(card.name(), c -> new AtomicInteger(0)).incrementAndGet() > maxCardCopies()) {
				messages.add(String.format("There must be no more than %d copies of %s.", maxCardCopies(), card.name()));
			}
		}

		int globalMin = minCards();
		int globalMax = maxCards();

		if (globalMin >= 0 && nCards < globalMin) {
			messages.add(String.format("Deck must contain no fewer than %d cards.", globalMin));
		} else if (globalMax >= 0 && nCards > globalMax) {
			messages.add(String.format("Deck must contain no more than %d cards.", globalMax));
		}

		messages.addAll(furtherValidation(deck));

		return messages;
	}
}

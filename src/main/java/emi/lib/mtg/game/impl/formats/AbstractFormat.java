package emi.lib.mtg.game.impl.formats;

import emi.lib.mtg.*;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractFormat implements Format {
	@Override
	public String toString() {
		return name();
	}

	protected abstract int minCards(Zone zone);

	protected abstract int maxCards(Zone zone);

	protected abstract int minCards();

	protected abstract int maxCards();

	protected abstract int maxCardCopies();

	protected boolean setIsLegal(emi.lib.mtg.Set set) {
		return true;
	}

	protected boolean cardIsBanned(Card card) {
		return false;
	}

	@Override
	public boolean cardIsLegal(Card card) {
		return !cardIsBanned(card) && card.printings().stream().anyMatch(pr -> setIsLegal(pr.set()));
	}

	protected abstract Set<String> furtherValidation(Deck.Variant variant);

	@Override
	public Set<String> validate(Deck.Variant variant) {
		Set<String> messages = new HashSet<>();

		Map<String, AtomicInteger> histogram = new HashMap<>();
		int nCards = 0;

		for (Zone zone : deckZones()) {
			int min = minCards(zone);
			int max = maxCards(zone);

			Collection<? extends Card.Printing> cardsInZone = variant.cards(zone);
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

			for (Card.Printing printing : cardsInZone) {
				if (!cardIsLegal(printing.card())) {
					messages.add(String.format("%s is not legal in this format.", printing.card().name()));
				}

				if (printing.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
					continue;
				}

				if (histogram.computeIfAbsent(printing.card().name(), c -> new AtomicInteger(0)).incrementAndGet() > maxCardCopies()) {
					messages.add(String.format("There must be no more than %d copies of %s.", maxCardCopies(), printing.card().name()));
				}
			}
		}

		int globalMin = minCards();
		int globalMax = maxCards();

		if (globalMin >= 0 && nCards < globalMin) {
			messages.add(String.format("Deck must contain no fewer than %d cards.", globalMin));
		} else if (globalMax >= 0 && nCards > globalMax) {
			messages.add(String.format("Deck must contain no more than %d cards.", globalMax));
		}

		messages.addAll(furtherValidation(variant));

		return messages;
	}
}

package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;
import emi.lib.mtg.game.ability.pregame.CopyLimit;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public enum CardCount implements Format.Validator {
	Freeform(-1, ZoneInfo.Unlimited, null, ZoneInfo.Unlimited),
	ConstructedSixtyCard(4, ZoneInfo.ConstructedLibrary, null, ZoneInfo.ConstructedSideboard),
	Brawl(1, ZoneInfo.BrawlLibrary, ZoneInfo.CommandZone, ZoneInfo.Unlimited),
	Commander(1, ZoneInfo.CommanderLibrary, ZoneInfo.CommandZone, ZoneInfo.Unlimited),
	;

	public enum ZoneInfo {
		Unlimited(0, -1),
		ConstructedLibrary(60, -1),
		ConstructedSideboard(0, 15),
		BrawlLibrary(58, 59),
		CommanderLibrary(98, 99),
		CommandZone(1, 2),
		;

		ZoneInfo(int minCards, int maxCards) {
			this.minCards = minCards;
			this.maxCards = maxCards;
		}

		public final int minCards, maxCards;
	}

	public final int maxCopies;
	public final Map<Zone, ZoneInfo> zones;

	CardCount(int maxCopies, ZoneInfo library, ZoneInfo command, ZoneInfo sideboard) {
		this.maxCopies = maxCopies;

		Map<Zone, ZoneInfo> zones = new EnumMap<>(Zone.class);
		if (library != null) zones.put(Zone.Library, library);
		if (command != null) zones.put(Zone.Command, command);
		if (sideboard != null) zones.put(Zone.Sideboard, sideboard);
		this.zones = Collections.unmodifiableMap(zones);
	}

	public Set<Zone> deckZones() {
		return zones.keySet();
	}

	@Override
	public Result validate(Deck deck, Format format, Result result) {
		if (result == null) result = new Result();

		Map<String, AtomicInteger> histogram = new HashMap<>();

		for (Zone zone : format.deckZones()) {
			Collection<? extends Card.Printing> ciz = deck.cards(zone);
			if (ciz == null || ciz.isEmpty()) continue;

			for (Card.Printing pr : ciz) {
				histogram.computeIfAbsent(pr.card().name(), n -> new AtomicInteger(0)).incrementAndGet();
			}
		}

		for (Zone zone : format.deckZones()) {
			Collection<? extends Card.Printing> ciz = deck.cards(zone);
			if (ciz == null || ciz.isEmpty()) continue;

			ZoneInfo fzi = zones.get(zone);

			for (Card.Printing pr : ciz) {
				if (!pr.card().faces().stream().allMatch(f -> f.type().supertypes().contains(Supertype.Basic))) {
					int min = 0, max = maxCopies;
					if (pr.card().front() != null) {
						CopyLimit override = pr.card().front().abilities().only(CopyLimit.class);
						if (override != null) {
							min = override.min;
							max = override.max;
						}
					}

					if (min > 0 && histogram.get(pr.card().name()).get() < min) {
						result.card(pr).errors.add(String.format("In %s, a deck must contain no fewer than %d cop%s of %s.",
								format.name(),
								min,
								min == 1 ? "y" : "ies",
								pr.card().name()));
					}

					if (max > 0 && histogram.get(pr.card().name()).get() > max) {
						result.card(pr).errors.add(String.format("In %s, a deck can contain no more than %d cop%s of %s.",
								format.name(),
								max,
								max == 1 ? "y" : "ies",
								pr.card().name()));
					}
				}
			}

			if (fzi.minCards > 0 && ciz.size() < fzi.minCards) {
				result.zoneErrors(zone).add(String.format("In %s, the %s zone must contain at least %d cards.",
						format.name(),
						zone.name(),
						fzi.minCards));
			} else if (fzi.maxCards > 0 && ciz.size() > fzi.maxCards) {
				result.zoneErrors(zone).add(String.format("In %s, the %s zone may contain no more than %d cards.",
						format.name(),
						zone.name(),
						fzi.maxCards));
			}
		}

		return result;
	}
}

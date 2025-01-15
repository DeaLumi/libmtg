package emi.lib.mtg.game.validation;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CardLegality implements Format.Validator {
	public static final CardLegality INSTANCE = new CardLegality();

	@Override
	public Result validate(Deck deck, Format format, Result result) {
		if (result == null) result = new Result();

		Set<String> cardNames = new HashSet<>();
		for (Zone zone : format.deckZones()) {
			Collection<? extends Card.Print> ciz = deck.cards(zone);

			if (ciz == null || ciz.isEmpty()) continue;

			for (Card.Print pr : ciz) {
				boolean firstSeen = cardNames.add(pr.card().name());

				switch (pr.card().legality(format)) {
					case Banned:
						result.card(pr).errors.add(String.format("%s is banned in %s!", pr.card().name(), format.name()));
						break;
					case NotLegal:
						if (pr.releaseDate().isAfter(LocalDate.now())) {
							result.card(pr).warnings.add(String.format("%s has not released yet.", pr.card().name()));
						} else {
							result.card(pr).errors.add(String.format("%s is not legal in %s.", pr.card().name(), format.name()));
						}
						break;
					case Restricted:
						if (!firstSeen) {
							result.card(pr).errors.add(String.format("%s is restricted to one copy per deck in %s.", pr.card().name(), format.name()));
						}
						break;
					case Legal:
						break;
					case Unknown:
						result.card(pr).warnings.add(String.format("Couldn't verify legality of %s in %s.", pr.card().name(), format.name()));
						break;
				}
			}
		}

		return result;
	}
}

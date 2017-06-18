package org.whitefoxy.lib.mtg.data;

import emi.lib.Service;
import org.whitefoxy.lib.mtg.card.Card;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Emi on 5/7/2017.
 */
@Service
@Service.Property.String(name="name")
public interface CardSource {
	Collection<? extends CardSet> sets();

	Card get(UUID id);

	default Collection<Card> cards() {
		return sets().stream().flatMap(cs -> cs.cards().stream()).collect(Collectors.toSet());
	}
}

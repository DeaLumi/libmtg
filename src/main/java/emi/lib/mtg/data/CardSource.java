package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.CardFace;

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

	CardFace get(UUID id);

	default Collection<CardFace> cards() {
		return sets().stream().flatMap(cs -> cs.cards().stream()).collect(Collectors.toSet());
	}
}

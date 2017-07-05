package emi.lib.mtg.data;

import emi.lib.mtg.card.CardFace;

import java.util.Collection;

/**
 * Created by Emi on 5/7/2017.
 */
public interface CardSet {
	String name();

	String code();

	Collection<? extends CardFace> cards();
}

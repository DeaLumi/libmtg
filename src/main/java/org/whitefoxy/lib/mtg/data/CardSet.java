package org.whitefoxy.lib.mtg.data;

import org.whitefoxy.lib.mtg.card.Card;

import java.util.Collection;

/**
 * Created by Emi on 5/7/2017.
 */
public interface CardSet {
	String name();

	String code();

	Collection<? extends Card> cards();
}

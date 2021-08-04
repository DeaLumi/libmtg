package emi.lib.mtg.characteristic;

import java.util.Collection;

/**
 * Created by Emi on 5/6/2016.
 */
public interface CardTypeLine {
	Collection<Supertype> supertypes();

	Collection<CardType> cardTypes();

	Collection<String> subtypes();

	default boolean isPermanent() {
		return cardTypes().stream().anyMatch(t -> t.permanent);
	}
}

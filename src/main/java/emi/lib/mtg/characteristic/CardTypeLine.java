package emi.lib.mtg.characteristic;

import java.util.Collection;
import java.util.Set;

/**
 * Created by Emi on 5/6/2016.
 */
public interface CardTypeLine {
	Set<Supertype> supertypes();

	Set<CardType> cardTypes();

	Set<String> subtypes();

	default boolean isPermanent() {
		return cardTypes().stream().anyMatch(t -> t.permanent);
	}
}

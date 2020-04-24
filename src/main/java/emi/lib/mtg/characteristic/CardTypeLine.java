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
		Collection<CardType> types = cardTypes();

		return types.contains(CardType.Artifact) ||
				types.contains(CardType.Creature) ||
				types.contains(CardType.Enchantment) ||
				types.contains(CardType.Land) ||
				types.contains(CardType.Planeswalker);
	}
}

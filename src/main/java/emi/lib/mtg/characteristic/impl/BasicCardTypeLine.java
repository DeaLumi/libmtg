package emi.lib.mtg.characteristic.impl;

import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Supertype;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Emi on 5/19/2017.
 */
public class BasicCardTypeLine implements CardTypeLine {

	public static BasicCardTypeLine parse(String typeLine) {
		typeLine = typeLine.trim();
		int split = typeLine.indexOf('—');

		final String[] upperTypes;

		if (split >= 0) {
			upperTypes = typeLine.substring(0, split).split(" ");
		} else {
			upperTypes = typeLine.split(" ");
		}

		Set<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
		Set<CardType> cardTypes = EnumSet.noneOf(CardType.class);

		for (String s : upperTypes) {
			try {
				supertypes.add(Supertype.valueOf(s));
			} catch (IllegalArgumentException iae) {
				try {
					cardTypes.add(CardType.valueOf(s));
				} catch (IllegalArgumentException iae2) {
					System.err.println(typeLine);
					iae2.printStackTrace();
				}
			}
		}

		Set<String> subtypes = split >= 0 ? Arrays.stream(typeLine.substring(Math.max(0, split + 1)).split(" ")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();

		return new BasicCardTypeLine(supertypes, cardTypes, subtypes);
	}

	protected final Set<Supertype> supertypes;
	protected final Set<CardType> cardTypes;
	protected final Set<String> subtypes;

	public BasicCardTypeLine(Set<Supertype> supertypes, Set<CardType> cardTypes, Set<String> subtypes) {
		this.supertypes = Collections.unmodifiableSet(supertypes);
		this.cardTypes = Collections.unmodifiableSet(cardTypes);
		this.subtypes = Collections.unmodifiableSet(subtypes);
	}

	@Override
	public Set<Supertype> supertypes() {
		return supertypes;
	}

	@Override
	public Set<CardType> cardTypes() {
		return cardTypes;
	}

	@Override
	public Set<String> subtypes() {
		return subtypes;
	}

	@Override
	public String toString() {
		if (!subtypes.isEmpty()) {
			return String.format("%s %s — %s",
					this.supertypes.stream().map(Supertype::name).collect(Collectors.joining(" ")),
					this.cardTypes.stream().map(CardType::name).collect(Collectors.joining(" ")),
					this.subtypes.stream().collect(Collectors.joining(" ")));
		} else {
			return String.format("%s %s",
					this.supertypes.stream().map(Supertype::name).collect(Collectors.joining(" ")),
					this.cardTypes.stream().map(CardType::name).collect(Collectors.joining(" ")));
		}
	}
}

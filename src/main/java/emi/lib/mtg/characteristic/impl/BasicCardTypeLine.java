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

	private static <E extends Enum<E>> E insensitiveValueOf(E[] values, String name) {
		for (E e : values) {
			if (e.name().toLowerCase().equals(name.toLowerCase())) {
				return e;
			}
		}

		throw new IllegalArgumentException("No such enum element " + name);
	}

	public static BasicCardTypeLine parse(String typeLine) {
		typeLine = typeLine.trim();
		int split = typeLine.indexOf('\u2014');

		if (split < 0) {
			split = typeLine.indexOf('\u0097');
		}

		final String[] upperTypes;

		if (split >= 0) {
			upperTypes = typeLine.substring(0, split).split(" +");
		} else {
			upperTypes = typeLine.split(" +");
		}

		Set<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
		Set<CardType> cardTypes = EnumSet.noneOf(CardType.class);

		for (String s : upperTypes) {
			if (s.isEmpty()) {
				continue;
			}

			try {
				supertypes.add(insensitiveValueOf(Supertype.values(), s));
			} catch (IllegalArgumentException iae) {
				try {
					cardTypes.add(insensitiveValueOf(CardType.values(), s));
				} catch (IllegalArgumentException iae2) {
					synchronized (System.err) {
						System.err.println(typeLine);
						for (int i = 0; i < typeLine.length(); ++i) {
							System.err.print(String.format("0x%x ", typeLine.codePointAt(i)));
						}
						System.err.println();
						System.err.println("Split: " + split);
						iae2.printStackTrace();
						System.err.flush();
					}
				}
			}
		}

		Set<String> subtypes = split >= 0 ? Arrays.stream(typeLine.substring(split + 2).split(" +")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();

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
		String fmtString;
		if (!supertypes.isEmpty()) {
			if (!subtypes.isEmpty()) {
				fmtString = "%1$s %2$s — %3$s";
			} else {
				fmtString = "%1$s %2$s";
			}
		} else {
			if (!subtypes.isEmpty()) {
				fmtString = "%2$s — %3$s";
			} else {
				fmtString = "%2$s";
			}
		}

		return String.format(fmtString,
				this.supertypes.stream().map(Supertype::name).collect(Collectors.joining(" ")),
				this.cardTypes.stream().map(CardType::name).collect(Collectors.joining(" ")),
				this.subtypes.stream().collect(Collectors.joining(" ")));
	}
}

package emi.lib.mtg;

import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.enums.Supertype;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

public interface TypeLine {
	Set<Supertype> supertypes();

	Set<CardType> cardTypes();

	Set<String> subtypes();

	default boolean is(Supertype supertype) {
		return supertypes().contains(supertype);
	}

	default boolean is(CardType cardType) {
		return cardTypes().contains(cardType);
	}

	default boolean is(String subtype) {
		return subtypes().contains(subtype);
	}

	default boolean isPermanent() {
		return cardTypes().stream().anyMatch(CardType::permanent);
	}

	class Basic implements TypeLine {

		private static <E extends Enum<E>> E insensitiveValueOf(E[] values, String name) {
			for (E e : values) {
				if (e.name().toLowerCase().equals(name.toLowerCase())) {
					return e;
				}
			}

			throw new IllegalArgumentException("No such enum element " + name);
		}

		public static Basic parse(String typeLine) {
			// Hack for B.F.M.'s right half.
			if ("Scariest Creature You’ll Ever See".equals(typeLine)) {
				Set<CardType> types = EnumSet.of(CardType.Creature);
				Set<String> subtypes = new HashSet<>();
				subtypes.addAll(Arrays.asList("Scariest", "Creature", "You’ll", "Ever", "See"));
				return new Basic(Collections.emptySet(), types, subtypes);
			}

			// Hack for un-cards with the Summon X type line
			typeLine = typeLine.replace("Summon", "Summon \u2014");

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

			return new Basic(supertypes, cardTypes, subtypes);
		}

		protected final Set<Supertype> supertypes;
		protected final Set<CardType> cardTypes;
		protected final Set<String> subtypes;

		public Basic(Set<Supertype> supertypes, Set<CardType> cardTypes, Set<String> subtypes) {
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
}

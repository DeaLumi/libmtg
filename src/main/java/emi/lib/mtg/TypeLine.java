package emi.lib.mtg;

import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.enums.Color;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.util.CollectionComparator;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

public interface TypeLine {
	/**
	 * Returns the set of supertypes in this typeline. Must not be null.
	 * @return the set of supertypes in this typeline.
	 */
	Set<Supertype> supertypes();

	/**
	 * Returns the set of card types in this typeline. Must not be null, probably shouldn't be empty.
	 * @return the set of card types in this typeline.
	 */
	Set<CardType> cardTypes();

	/**
	 * Returns the set of subtypes in this typeline. Must not be null.
	 * @return the set of subtypes in this typeline.
	 */
	Set<String> subtypes();

	/**
	 * Returns true if this type is the given supertype. By default, checks supertypes().contains()
	 * @param supertype The supertype to check for.
	 * @return true if this type is the given supertype.
	 */
	default boolean is(Supertype supertype) {
		return supertypes().contains(supertype);
	}

	/**
	 * Returns true if this type is the given card type. By default, checks cardTypes().contains()
	 * @param cardType The card type to check for.
	 * @return true if this type is the given card type.
	 */
	default boolean is(CardType cardType) {
		return cardTypes().contains(cardType);
	}

	/**
	 * Returns tue if this type is the given subtype. By default, checks subtypes().contains(). In a game, you might override this for changelings.
	 * @param subtype The subtype to check for.
	 * @return true if this type is the given subtype.
	 */
	default boolean is(String subtype) {
		return subtypes().contains(subtype);
	}

	/**
	 * Returns true if this represents a permanent type. By default, checks all card types' permanent() flag.
	 * @return true if this type is a permanent type.
	 */
	default boolean isPermanent() {
		return cardTypes().stream().anyMatch(CardType::permanent);
	}

	/**
	 * Compares two non-null typelines according to the following algorithm:
	 * - The comparator decides which type groups to compare. It will only compare a type group if both left and right have at least one type in that type group.
	 * - If no type groups are to be compared, the result is {@link CollectionComparator.Result#Disjoint}.
	 * - If only one type group is to be compared, the result {@link CollectionComparator#SET_COMPARATOR} comparison of those two sets.
	 * - If two or more type groups are to be compared, the result depends on the consistency of each of those comparison's results as determined by {@link CollectionComparator#SET_COMPARATOR}:
	 *     - If all results are in agreement, the agreed-upon result is returned.
	 *     - If some results disagree, the result is {@link CollectionComparator.Result#Intersects}.
	 */
	CollectionComparator<TypeLine> COMPARATOR = (left, right) -> {
		Objects.requireNonNull(left);
		Objects.requireNonNull(right);

		boolean compareSupertypes = !left.supertypes().isEmpty() && !right.supertypes().isEmpty();
		boolean compareCardTypes = !left.cardTypes().isEmpty() && !right.cardTypes().isEmpty();
		boolean compareSubtypes = !left.subtypes().isEmpty() && !right.subtypes().isEmpty();

		if (!compareSupertypes && !compareCardTypes && !compareSubtypes) return CollectionComparator.Result.Disjoint;

		CollectionComparator.Result supertypes = compareSupertypes ? CollectionComparator.SET_COMPARATOR.compare(left.supertypes(), right.supertypes()) : CollectionComparator.Result.Disjoint;
		if (!compareCardTypes && !compareSubtypes) return supertypes;

		CollectionComparator.Result cardTypes = compareCardTypes ? CollectionComparator.SET_COMPARATOR.compare(left.cardTypes(), right.cardTypes()) : CollectionComparator.Result.Disjoint;
		if (!compareSupertypes && !compareSubtypes) return cardTypes;

		CollectionComparator.Result subtypes = compareSubtypes ? CollectionComparator.SET_COMPARATOR.compare(left.subtypes(), right.subtypes()) : CollectionComparator.Result.Disjoint;
		if (!compareSupertypes && !compareCardTypes) return subtypes;

		if (!compareSupertypes) return cardTypes == subtypes ? cardTypes : CollectionComparator.Result.Intersects;
		if (!compareCardTypes) return supertypes == subtypes ? supertypes : CollectionComparator.Result.Intersects;
		if (!compareSubtypes) return supertypes == cardTypes ? supertypes : CollectionComparator.Result.Intersects;

		return (supertypes == cardTypes && cardTypes == subtypes) ? supertypes : CollectionComparator.Result.Intersects;
	};

	static Color.Combination landColorIdentity(TypeLine type) {
		if (!type.is(CardType.Land)) return Color.Combination.Empty;

		Color.Combination combo = Color.Combination.Empty;
		if (type.is("Plains")) combo = combo.plus(Color.White);
		if (type.is("Island")) combo = combo.plus(Color.Blue);
		if (type.is("Swamp")) combo = combo.plus(Color.Black);
		if (type.is("Mountain")) combo = combo.plus(Color.Red);
		if (type.is("Forest")) combo = combo.plus(Color.Green);
		if (combo.isEmpty() && type.subtypes().isEmpty()) combo = combo.plus(Color.Colorless);

		return combo;
	}

	/**
	 * A basic implementation of a type line. Supertypes and subtypes are stored in EnumSets, while subtypes are stored
	 * in a HashSet. At present, typelines can't be modified. I might adopt the Mana.Value semantics of copy()/add()
	 * later.
	 */
	class Basic implements TypeLine {

		/**
		 * Attempts to parse a complete type line from a string, by splitting on a unicode mdash (<code>&mdash;</code>
		 * or <code>\u2014</code>) or hyphen (<code>-</code> or <code>\u0097</code>). Strings before the dash (split on
		 * spaces) are card or supertypes, looked up from the enumerations, while those after are subtypes and are added
		 * directly.
		 *
		 * This version is the most complete and correct, handling even weird Un- typelines (like B.F.M.'s right half,
		 * or Unglued's 'Summon X' types). But it's a little more expensive and I might phase it out.
		 *
		 * @param typeLine A complete typeline, including a dash.
		 * @return A TypeLine.Basic object representing the same type line represented by the given string.
		 */
		public static Basic parse(String typeLine) {
			// Hack for B.F.M.'s right half.
			if ("Scariest Creature You'll Ever See".equals(typeLine) || "Scariest Creature You’ll Ever See".equals(typeLine)) {
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

				String capped = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();

				try {
					supertypes.add(Supertype.valueOf(capped));
				} catch (IllegalArgumentException iae) {
					try {
						cardTypes.add(CardType.valueOf(capped));
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

			// TODO: Time Lord is now a single creature type. Thanks, WotC.
			Set<String> subtypes = split >= 0 ? Arrays.stream(typeLine.substring(split + 2).split(" +")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();

			return new Basic(supertypes, cardTypes, subtypes);
		}

		/**
		 * Attempts to parse a complete typeline from any old string. It splits the string on spaces, then checks each
		 * for a corresponding Supertype or CardType, and stashes the rest as subtypes.
		 *
		 * Segments are processed into a reasonable form as follows: First, any leading or trailing hyphens or emdashes
		 * are discarded. Then whitespace is trimmed, and the type is Capitalized.
		 *
		 * This function is intended to provide a highly tolerant type line parser, for use with user-entered data. It's
		 * not perfect, however.
		 *
		 * @param types The list of types to include in this typeline. Words matching super- or card types are taken to
		 *              be those.
		 * @return A typeline representing the given types.
		 */
		// TODO: Have parse() use this?
		public static Basic parseFragment(String types) {
			Set<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
			Set<CardType> cardTypes = EnumSet.noneOf(CardType.class);
			Set<String> subtypes = new HashSet<>();

			boolean dash = false, preDashElemental = false;

			for (String elem : types.split(" +")) {
				if (elem.isEmpty()) continue;

				if (!dash && "-".equals(elem) || "\u2014".equals(elem)) {
					dash = true;

					if (preDashElemental && subtypes.contains("Elemental")) {
						subtypes.remove("Elemental");
						cardTypes.add(CardType.Elemental);
					}

					continue;
				}

				elem = elem.trim();

				if (elem.isEmpty()) continue;

				String capped = elem.substring(0, 1).toUpperCase() + elem.substring(1).toLowerCase();

				if (!dash) {
					try {
						supertypes.add(Supertype.valueOf(capped));
						continue;
					} catch (IllegalArgumentException iae) {
						// ignore
					}

					if (!"Elemental".equals(capped)) {
						try {
							cardTypes.add(CardType.valueOf(capped));
							continue;
						} catch (IllegalArgumentException iae) {
							// ignore
						}
					} else {
						preDashElemental = true;
					}
				}

				subtypes.add(capped);
			}

			return new TypeLine.Basic(supertypes, cardTypes, subtypes);
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

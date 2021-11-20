package emi.lib.mtg.util;

import java.util.*;

/**
 * Represents an object which can be compared with another in a complex way.
 * @param <T> The collection type this comparator compares. Element type is irrelevant.
 */
public interface CollectionComparator<T> {
	enum Result {
		Equal (0.0),
		ContainedIn (-1.0),
		Contains (1.0),
		Intersects (Double.NaN),
		Disjoint (-Double.NaN);

		private final double compareValue;

		Result(double compareValue) {
			this.compareValue = compareValue;
		}

		/**
		 * Returns the comparison value of this result. This is analogous to the integer result of a normal comparison,
		 * and can be used similarly: when greater than or equal to zero, for instance, the left-hand operand is a
		 * superset-or-equal-to the right-hand operand.
		 *
		 * In the event of an intersectional or disjoint comparison result, the returned value is +/-NaN (respectively).
		 * However, this is value still suitable for use with comparisons as suggested above, due to the following:
		 * - Except for {@code NaN != x}, all comparisons involving NaN are false, so intersectional and disjoint
		 *   results will ever indicate one set is greater than, less than, or equal to another.
		 * - Because {@code NaN != x}, intersectional and disjoint results do indicate that one set is not equal to
		 *   another via {@code value() != 0}.
		 *
		 * @return A double value suitable for comparisons (mostly against zero) analogous to the integer result of
		 * 	a Comparator.
		 */
		public double value() {
			return compareValue;
		}
	}

	Result compare(T a, T b);

	/**
	 * Provides a basic comparison of two sets by sorting them into a Venn diagram of A-only, B-only, and both.
	 * - If both A- and B-only groups are empty, the result is Equal.
	 * - If A-only is empty, a is ContainedIn b, and vice-versa for B-only.
	 * - If neither is empty, the result is Disjoint if the intersection is empty or Intersects if it is not.
	 */
	CollectionComparator<Set<?>> SET_COMPARATOR = (a, b) -> {
		Set<Object> aOnly = new HashSet<>();
		Set<Object> intersection = new HashSet<>();
		Set<Object> bOnly = new HashSet<>();

		for (Object aElem : a) {
			(b.contains(aElem) ? intersection : aOnly).add(aElem);
		}

		for (Object bElem : b) {
			if (a.contains(bElem)) continue;
			bOnly.add(bElem);
		}

		if (aOnly.isEmpty() && bOnly.isEmpty()) return Result.Equal;
		if (aOnly.isEmpty()) return Result.ContainedIn;
		if (bOnly.isEmpty()) return Result.Contains;

		return intersection.isEmpty() ? Result.Disjoint : Result.Intersects;
	};
}

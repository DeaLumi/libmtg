package emi.lib.mtg.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an object which can be compared with another in a complex way.
 * @param <T> The collection type this comparator compares. Element type is irrelevant.
 */
public interface CollectionComparator<T extends Collection<?>> {
	enum Result {
		Equal (0),
		ContainedIn (-1),
		Contains (1),
		Intersects (0),
		Disjoint (0);

		private final int compareValue;

		Result(int compareValue) {
			this.compareValue = compareValue;
		}

		public int value() {
			return compareValue;
		}
	}

	Result compare(T a, T b);

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

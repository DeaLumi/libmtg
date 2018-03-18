package emi.lib.mtg.characteristic;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public interface ManaCost {
	Collection<? extends ManaSymbol> symbols();

	default double convertedCost() {
		return symbols().parallelStream()
				.mapToDouble(ManaSymbol::convertedCost)
				.sum();
	}

	default Set<Color> color() {
		return symbols().parallelStream()
				.flatMap(s -> s.color().parallelStream())
				.collect(() -> EnumSet.noneOf(Color.class), EnumSet::add, EnumSet::addAll);
	}

	default boolean varies() {
		return symbols().parallelStream()
				.anyMatch(ManaSymbol::varies);
	}
}

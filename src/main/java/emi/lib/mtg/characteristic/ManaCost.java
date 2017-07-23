package emi.lib.mtg.characteristic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface ManaCost {
	Collection<? extends ManaSymbol> symbols();

	default double convertedCost() {
		double count = 0;

		for(ManaSymbol symbol : symbols()) {
			count += symbol.convertedCost();
		}

		return count;
	}

	default Set<Color> color() {
		Set<Color> union = new HashSet<>();

		for (ManaSymbol symbol : symbols()) {
			union.addAll(symbol.color());
		}

		return union;
	}

	default boolean varies() {
		return symbols().stream().anyMatch(ManaSymbol::varies);
	}
}

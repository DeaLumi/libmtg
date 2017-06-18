package org.whitefoxy.lib.mtg.characteristic;

import org.whitefoxy.lib.mtg.characteristic.impl.BasicManaSymbol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Emi on 5/6/2016.
 */
public interface ManaCost {
	Collection<BasicManaSymbol> symbols();

	default int convertedCost() {
		int count = 0;

		for(ManaSymbol symbol : symbols()) {
			count += symbol.convertedCost();
		}

		return count;
	}

	default Collection<Color> colors() {
		Set<Color> union = new HashSet<>();

		for (ManaSymbol symbol : symbols()) {
			union.addAll(symbol.colors());
		}

		return union;
	}

	default boolean varies() {
		return symbols().stream().anyMatch(ManaSymbol::varies);
	}
}

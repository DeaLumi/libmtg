package org.whitefoxy.lib.mtg.characteristic;

import java.util.Collection;

/**
 * Created by Emi on 5/6/2016.
 */
public interface ManaSymbol {
	int convertedCost();

	Collection<Color> colors();
}

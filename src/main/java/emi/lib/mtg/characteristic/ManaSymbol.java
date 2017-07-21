package emi.lib.mtg.characteristic;

import java.util.Collection;

/**
 * Created by Emi on 5/6/2016.
 */
public interface ManaSymbol {
	double convertedCost();

	boolean varies();

	Collection<Color> color();
}

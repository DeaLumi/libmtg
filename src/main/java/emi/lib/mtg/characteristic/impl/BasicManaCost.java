package emi.lib.mtg.characteristic.impl;

import emi.lib.mtg.characteristic.ManaCost;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Created by Emi on 5/19/2017.
 */
public class BasicManaCost implements ManaCost {
	private final Collection<BasicManaSymbol> symbols;

	public BasicManaCost(String cost) {
		this.symbols = Collections.unmodifiableCollection(BasicManaSymbol.parse(cost));
	}

	@Override
	public Collection<BasicManaSymbol> symbols() {
		return symbols;
	}

	@Override
	public String toString() {
		return symbols().stream().map(BasicManaSymbol::toString).collect(Collectors.joining());
	}
}

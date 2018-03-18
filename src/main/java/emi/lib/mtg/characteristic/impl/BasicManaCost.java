package emi.lib.mtg.characteristic.impl;

import emi.lib.mtg.characteristic.ManaSymbol;
import emi.lib.mtg.characteristic.ManaCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class BasicManaCost implements ManaCost {
	public static BasicManaCost parse(String manaCost) {
		return new BasicManaCost(ManaSymbol.symbolsIn(manaCost));
	}

	private final List<? extends ManaSymbol> symbols;

	public BasicManaCost(List<? extends ManaSymbol> symbols) {
		this.symbols = symbols;
	}

	@Override
	public Collection<? extends ManaSymbol> symbols() {
		return symbols;
	}

	@Override
	public String toString() {
		return symbols().stream().map(ManaSymbol::toString).collect(Collectors.joining());
	}
}

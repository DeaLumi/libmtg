package emi.lib.mtg.characteristic.impl;

import emi.lib.mtg.characteristic.ManaCost;
import emi.lib.mtg.characteristic.ManaSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Created by Emi on 5/19/2017.
 */
public class BasicManaCost implements ManaCost {
	private static List<BasicManaSymbol> parseSymbols(String manaCost) {
		Matcher m = BasicManaSymbol.SYMBOL_PATTERN.matcher(manaCost);

		List<BasicManaSymbol> list = new ArrayList<>();

		int lastEnd = 0;
		while (m.find()) {
			list.add(BasicManaSymbol.fromString(m.group()));
			lastEnd = m.end();
		}

		if (lastEnd != manaCost.length()) {
			throw new IllegalArgumentException(String.format("%s is not a valid mana cost (%d != %d)...", manaCost, lastEnd, manaCost.length()));
		}

		return list;
	}

	public static BasicManaCost parse(String manaCost) {
		return new BasicManaCost(parseSymbols(manaCost));
	}

	private final List<? extends ManaSymbol> symbols;

	public BasicManaCost(List<? extends ManaSymbol> symbols) {
		this.symbols = symbols;
	}

	@Deprecated
	public BasicManaCost(String cost) {
		this.symbols = Collections.unmodifiableList(parseSymbols(cost));
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

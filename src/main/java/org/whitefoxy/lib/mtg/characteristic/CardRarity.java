package org.whitefoxy.lib.mtg.characteristic;

import java.util.NoSuchElementException;

/**
 * Created by Emi on 5/6/2016.
 */
public enum CardRarity {
	BasicLand,
	Common,
	Uncommon,
	Rare,
	MythicRare,
	Special;

	private final String text;

	CardRarity() {
		this.text = name().replaceAll("([a-z])([A-Z])", "$1 $2");
	}

	@Override
	public String toString() {
		return text;
	}

	public static CardRarity forString(String name) {
		for (CardRarity rarity : CardRarity.values()) {
			if (rarity.text.equals(name)) {
				return rarity;
			}
		}

		throw new NoSuchElementException();
	}
}

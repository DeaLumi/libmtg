package emi.lib.mtg.enums;

import java.util.NoSuchElementException;

public enum Rarity {
	Special,
	BasicLand,
	Common,
	Uncommon,
	Rare,
	MythicRare;

	private final String text;

	Rarity() {
		this.text = name().replaceAll("([a-z])([A-Z])", "$1 $2");
	}

	@Override
	public String toString() {
		return text;
	}

	public static Rarity forString(String name) {
		for (Rarity rarity : Rarity.values()) {
			if (rarity.text.equals(name)) {
				return rarity;
			}
		}

		throw new NoSuchElementException();
	}
}

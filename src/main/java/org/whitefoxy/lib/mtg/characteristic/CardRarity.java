package org.whitefoxy.lib.mtg.characteristic;

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
}

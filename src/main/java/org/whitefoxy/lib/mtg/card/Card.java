package org.whitefoxy.lib.mtg.card;

import org.whitefoxy.lib.mtg.characteristic.CardTypeLine;
import org.whitefoxy.lib.mtg.characteristic.Color;
import org.whitefoxy.lib.mtg.characteristic.ManaCost;
import org.whitefoxy.lib.mtg.characteristic.CardRarity;
import org.whitefoxy.lib.mtg.data.CardSet;

import java.net.URL;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Emi on 3/18/2016.
 */
public interface Card {

	CardSet set();

	String name();

	ManaCost manaCost();

	Set<Color> color();

	Set<Color> colorIdentity();

	CardRarity rarity();

	CardTypeLine type();

	String text();

	String flavor();

	String power();

	String toughness();

	String loyalty();

	String collectorNumber();

	int variation();

	default UUID id() {
		return UUID.nameUUIDFromBytes(String.format("%s%s%s", set().code(), name(), collectorNumber()).getBytes());
	}

}

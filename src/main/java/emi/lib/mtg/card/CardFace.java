package emi.lib.mtg.card;

import emi.lib.mtg.characteristic.CardRarity;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.ManaCost;
import emi.lib.mtg.data.CardSet;

import java.util.Set;
import java.util.UUID;

/**
 * Created by Emi on 3/18/2016.
 */
public interface CardFace {

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

package emi.lib.mtg.card;

import emi.lib.mtg.characteristic.CardRarity;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.ManaCost;
import emi.lib.mtg.data.CardSet;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a face of a card. Most cards have only one face, and only one card (Who // What // When // Where // Why)
 * has more than two faces. A face of a card has a set of characteristics as dictated by the comprehensive rules.
 */
public interface CardFace {

	/**
	 * Kinds of card faces. Most cards have only one face, and only one card (Who // What // When // Where // Why) has
	 * more than two faces, but there are differences between two-faced layouts. Card sources should observe these
	 * differences and assign face kinds appropriately.
	 */
	enum Kind {
		/**
		 * The front (i.e. normal) face of a card. Most cards only have this face.
		 */
		Front,

		/**
		 * The transformed back face of a double-faced card. i.e. werewolves, Origins walkers
		 */
		Transformed,

		/**
		 * The left half of a split card
		 */
		Left,

		/**
		 * The right half of a split card
		 */
		Right,

		/**
		 * The primary half of Amonkhet splits & the upright part of Kamigawa flip cards
		 */
		Top,

		/**
		 * The 'aftermath' half of Amonkhet splits & the flipped (upside-down) half of Kamigawa flip cards
		 */
		Bottom,

		/**
		 * Any other card role.
		 */
		Other
	}

	/**
	 * @return The kind of card face this is.
	 */
	Kind kind();

	/**
	 * @return The full English name of this card face.
	 */
	String name();

	/**
	 * @return This card face's mana cost, as a complex object.
	 */
	ManaCost manaCost();

	/**
	 * @return This card face's colors. A colorless card should have an empty set for its color.
	 */
	Set<Color> color();

	/**
	 * @return This card face's color identity. A colorless card with no colored mana symbols should have an empty set
	 * for its color identity.
	 */
	Set<Color> colorIdentity();

	/**
	 * @return This card face's full type line, as a complex object.
	 */
	CardTypeLine type();

	/**
	 * @return The rules text of this card face, in English.
	 */
	String text();

	/**
	 * @return The flavor text of this card face, in English.
	 */
	String flavor();

	/**
	 * @return The power of this card face, if it is a creature or vehicle. Cards without a printed power should return
	 * an empty string. Cards with variable power/toughness should report the value correctly (i.e. "1+*").
	 */
	String power();

	/**
	 * @return The toughness of this card face, if it is a creature or vehicle. Cards without a printed toughness should
	 * return an empty string. Cards with variable power/toughness should report the value correctly (i.e. "1+*").
	 */
	String toughness();

	/**
	 * @return The loyalty of this card face, if it is a planeswalker. Cards without a printed loyalty should return an
	 * empty string. Cards with variable initial loyalty should report this value correct (i.e. "X").
	 */
	String loyalty();

	/**
	 * @return The collector number of this card face. Can be an empty string if the card face wasn't assigned a number.
	 */
	String collectorNumber();

}

package emi.lib.mtg;

import emi.lib.mtg.characteristic.*;
import emi.lib.mtg.characteristic.impl.BasicManaCost;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Magic: the Gathering card. Collects all of a card's playable and fluff characteristics with each unique
 * printing of a card known to the providing data source.
 */
@SuppressWarnings("unused")
public interface Card {
	/**
	 * The face of a card. This is the collection of card characteristics for a given card, based on its state.
	 */
	interface Face {
		/**
		 * Utility function to 'convert' a string value to its nearest number representation.
		 * If the value is an empty string, Double.NaN is returned. Otherwise, the value is
		 * evaluated as though any characteristic-defining abilities gave 0.
		 * @param value The value to convert.
		 * @return The nearest number representation of the given value.
		 */
		static double convertedValue(String value) {
			if (value == null || value.isEmpty()) {
				return Double.NaN;
			}

			// Why is there a card with *^2 in its power? :c
			return Double.parseDouble(value.replaceAll("[-+]?[*]\u00b2?", ""));
		}

		/**
		 * Represents a kind of card face. Almost all cards have Front faces, and the vast majority of cards *only*
		 * have Front faces. Split cards uniquely have only Left and Right faces.
		 *
		 * Only one card (Who // What // When // Where // Why) has more than two faces. Eff that card.
		 */
		enum Kind {
			/**
			 * The front face of this card. These characteristics are usually the correct ones.
			 */
			Front,

			/**
			 * The transformed face of this double-faced card (i.e. Innistrad Werewolves and Origins sparkers).
			 */
			Transformed,

			/**
			 * The flipped face of this Kamigawa-style flip-card.
			 */
			Flipped,

			/**
			 * The left face of this split card, including the 'top' face of Amonkhet splits.
			 */
			Left,

			/**
			 * The right face of this split card, including the 'aftermath' face of Amonkhet splits.
			 */
			Right,

			/**
			 * Other faces I don't know about.
			 */
			Other
		}

		/**
		 * @return The kind of card face this is (i.e. when its characteristics overwrite the card's). Never null.
		 */
		Kind kind();

		/**
		 * @return The name of this card face. An empty string if the card has no name.
		 */
		String name();

		/**
		 * @return This card face's converted mana cost. An empty ManaCost if this card has no mana cost. (Different from {0}.)
		 */
		ManaCost manaCost();

		/**
		 * @return The colors of this card's color indicator. An empty set if this card has no color indicator.
		 */
		Set<Color> colorIndicator();

		/**
		 * @return This card's type line. An empty CardTypeLine if this card has no type. (...it really should have a type.)
		 */
		CardTypeLine type();

		/**
		 * @return This card's rules text. An empty string if a vanilla card.
		 */
		String rules();

		/**
		 * @return This card's power. An empty string if it has no power/toughness box.
		 */
		String power();

		/**
		 * @return This card's toughness. An empty string if it has no power/toughness box.
		 */
		String toughness();

		/**
		 * @return This card's starting loyalty. An empty string if it has no loyalty box.
		 */
		String loyalty();

		/**
		 * @return This card's hand modifier. An empty string if it has no hand/life modifiers.
		 */
		String handModifier();

		/**
		 * @return This card's life modifier. An empty string if it has no hand/life modifiers.
		 */
		String lifeModifier();

		/**
		 * Derived characteristic. Union of the color indicator and mana cost color.
		 *
		 * N.B. Sources SHOULD override this! Consider Ghostflame and Devoid cards. We can't be arsed to
		 * search through the text for characteristic-defining abilities here!
		 *
		 * @return This card's effective color. An empty set if the card is colorless.
		 */
		default Set<Color> color() {
			EnumSet<Color> color = EnumSet.copyOf(this.colorIndicator());
			color.addAll(this.manaCost().color());
			return color;
		}

		/**
		 * Derived characteristic. Union of mana cost color and color indicator and colors of mana
		 * symbols in rules text and characteristic-defining abilities (uuughh).
		 * @return This card's color identity. An empty set if the card is colorless.
		 */
		default Set<Color> colorIdentity() {
			EnumSet<Color> color = EnumSet.copyOf(this.color());
			ManaSymbol.symbolsIn(this.rules()).stream()
					.flatMap(s -> s.color().stream())
					.forEach(color::add);
			return color;
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's power.
		 * For cards with no power, this is NaN. Otherwise, this is the card's power,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's power.
		 */
		default double convertedPower() {
			return convertedValue(this.power());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's toughness.
		 * For cards with no toughness, this is NaN. Otherwise, this is the card's toughness,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's toughness.
		 */
		default double convertedToughness() {
			return convertedValue(this.toughness());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's loyalty.
		 * For cards with no loyalty, this is NaN. Otherwise, this is the card's loyalty,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's loyalty.
		 */
		default double convertedLoyalty() {
			return convertedValue(this.loyalty());
		}
	}

	/**
	 * A unique printing of a card.
	 */
	interface Printing {
		/**
		 * The printed (i.e. not-gameplay-relevant) characteristics of this face. These vary with printing.
		 */
		interface Face {

			/**
			 * @return The printing associated with this face printing.
			 */
			Printing printing();

			/**
			 * @return The face associated with this face printing.
			 */
			Card.Face face();

			/**
			 * @return The flavor text printed on this face in this printing.
			 */
			String flavor();
		}

		/**
		 * @return The card of which this printing is a printing.
		 */
		Card card();

		/**
		 * @return The set of printed face for this card printing.
		 */
		Set<? extends Face> faces();

		/**
		 * Retrieves a particular card face printing by face kind.
		 * @param kind The kind of face of which to get the printed version.
		 * @return The printed version of that face kind.
		 */
		Face face(Card.Face.Kind kind);

		/**
		 * @return The set in which this printing was printed.
		 */
		emi.lib.mtg.Set set();

		/**
		 * @return The rarity of this printing.
		 */
		CardRarity rarity();

		/**
		 * @return The multiverseId of this printing. Null if the card has no multiverseid.
		 */
		Integer multiverseId();

		/**
		 * @return The variation index of this printing (i.e. the 1-indexed number of cards with this name in this set).
		 */
		int variation();

		/**
		 * @return The collector number of this card printing. Null if there is no collecotr number.
		 */
		String collectorNumber();

		/**
		 * @return The MTGO catalog ID of this printing. Null if the card isn't on MTGO, or if the data source doesn't know.
		 */
		Integer mtgoCatalogId();

		/**
		 * N.B. for implementors: This ID must be unique across all printings of all cards, and ideally unique across
		 * all data sources.
		 *
		 * @return A unique ID that can be used to refer to this printing. Must not be null. This is used to reliably
		 * refer to *this* printing of *this* card.
		 */
		UUID id();
	}

	/**
	 * The set of faces of this card. There should be no more than one face per Face.Kind.
	 * @return The set of this card's faces.
	 */
	Set<? extends Face> faces();

	/**
	 * Retrieves a particular face.
	 * @param kind The Face to get.
	 * @return The face of that kind, or null if this card has no such face.
	 */
	Face face(Face.Kind kind);

	/**
	 * The set of this card's printings.
	 * @return The set of this card's printings.
	 */
	Set<? extends Printing> printings();

	/**
	 * Retrieves a particular printing.
	 * @param id The ID of the printing to get.
	 * @return The printing with that ID, or null if this card has no such printing.
	 */
	Printing printing(UUID id);

	/**
	 * Tries to calculate the best name of this card. For most cards including transform and
	 * Kamigawa flip cards, this is the name of the front/upright face. For split cards, this
	 * is the name of the left and right halves concatenated with " // ".
	 * @return The most presentable name of this card.
	 */
	default String name() {
		if (this.faces().size() <= 2 && this.face(Face.Kind.Front) != null) {
			return this.face(Face.Kind.Front).name();
		} else if (this.faces().size() == 2 && this.face(Face.Kind.Left) != null && this.face(Face.Kind.Right) != null) {
			return String.format("%s // %s", this.face(Face.Kind.Left).name(), this.face(Face.Kind.Right).name());
		} else {
			return this.fullName();
		}
	}

	/**
	 * Returns the unabridged name of this card, formed by concatenating the names of its
	 * faces with " // " (i.e. Fire // Ice) in the order of face kinds.
	 * @return The complete name of this card.
	 */
	default String fullName() {
		return this.faces().stream()
				.sorted(Comparator.comparingInt(f -> f.kind().ordinal()))
				.map(Face::name)
				.collect(Collectors.joining(" // "));
	}

	/**
	 * Tries to determine the best mana cost of this card. For most cards including transform and
	 * Kamigawa flip cards, this is the mana cost of the front/upright face. For split cards, this
	 * is the combined mana cost of the left and right halves (as though it were fused).
	 * @return The most presentable mana cost of this card.
	 */
	default ManaCost manaCost() {
		if (this.faces().size() <= 2 && this.face(Face.Kind.Front) != null) {
			return this.face(Face.Kind.Front).manaCost();
		} else if (this.faces().size() == 2 && this.face(Face.Kind.Left) != null && this.face(Face.Kind.Right) != null) {
			return new BasicManaCost(Stream.concat(
							this.face(Face.Kind.Left).manaCost().symbols().stream(),
							this.face(Face.Kind.Right).manaCost().symbols().stream())
					.collect(Collectors.toList()));
		} else {
			return this.fullManaCost();
		}
	}

	/**
	 * Returns the full, combined mana cost of all faces of this card, formed by adding all mana symbols
	 * of each face's mana cost together. Note that this does not reduce generic symbols.
	 * @return The complete mana cost of this card.
	 */
	default ManaCost fullManaCost() {
		return new BasicManaCost(this.faces().stream()
				.flatMap(f -> f.manaCost().symbols().stream())
				.collect(Collectors.toList()));
	}

	/**
	 * Returns the color identity of this card. This is the union of all faces' color identities.
	 * @return The color identity of this card.
	 */
	default Set<Color> colorIdentity() {
		return this.faces().stream()
				.flatMap(f -> f.colorIdentity().stream())
				.collect(() -> EnumSet.noneOf(Color.class), Set::add, Set::addAll);
	}
}

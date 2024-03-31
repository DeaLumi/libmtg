package emi.lib.mtg;

import emi.lib.mtg.enums.*;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.Abilities;

import java.time.LocalDate;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

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

			if (value.contains("\u221e")) {
				return Double.POSITIVE_INFINITY; // TODO: Catch negative infinity? :\
			} else if (value.contains("?") || "X".equals(value)) {
				return 0.0;
			}

			// UGH, DUNGEON MASTER
			value = value.replaceAll("[1-9][0-9]*d[1-9][0-9]*\\+?", "");
			value = value.replaceAll("[-+]?[*]\u00b2?", "");

			try {
				// Why is there a card with *^2 in its power? :c
				return !value.isEmpty() ? Double.parseDouble(value) : 0;
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				return Double.NaN;
			}
		}

		/**
		 * @return The name of this card face. An empty string if the card has no name.
		 */
		String name();

		/**
		 * @return This card face's mana cost. An empty Mana.Value if this card has no mana cost. (Different from {0}.)
		 */
		Mana.Value manaCost();

		/**
		 * Returns this card face's mana value, as defined by the comprehensive rules. Note that this isn't guaranteed
		 * to equal <code>manaCost().value()</code>.
		 *
		 * @return This face's mana value, as defined by the comprehensive rules.
		 */
		double manaValue();

		/**
		 * @return The colors of this card's color indicator. An empty set if this card has no color indicator.
		 */
		Color.Combination colorIndicator();

		/**
		 * @return This card's type line. An empty TypeLine if this card has no type. (...it really should have a type.)
		 */
		TypeLine type();

		/**
		 * @return This card's rules text. An empty string if a vanilla card.
		 */
		String rules();

		/**
		 * @return This card's power. An empty string if it has no power/toughness box.
		 */
		String printedPower();

		/**
		 * @return This card's toughness. An empty string if it has no power/toughness box.
		 */
		String printedToughness();

		/**
		 * @return This card's starting loyalty. An empty string if it has no loyalty box.
		 */
		String printedLoyalty();

		/**
		 * @return This card's starting defense. An empty string if it has no defense box.
		 */
		String printedDefense();

		/**
		 * @return This card's hand modifier. An empty string if it has no hand/life modifiers.
		 */
		String handModifier();

		/**
		 * @return This card's life modifier. An empty string if it has no hand/life modifiers.
		 */
		String lifeModifier();

		/**
		 * @return The set of abilities printed on this card. An empty collection if this card has no text.
		 */
		Abilities abilities();

		/**
		 * Derived characteristic. Union of the color indicator and mana cost color.
		 *
		 * N.B. Sources SHOULD override this! Consider Ghostflame and Devoid cards. We can't be arsed to
		 * search through the text for characteristic-defining abilities here!
		 *
		 * @return This card's effective color. An empty set if the card is colorless.
		 */
		default Color.Combination color() {
			return this.colorIdentity()
					.plus(this.manaCost().color());
		}

		/**
		 * Derived characteristic. Union of mana cost color and color indicator and colors of mana
		 * symbols in rules text and characteristic-defining abilities (uuughh).
		 * @return This card face's color identity. An empty set if the face is colorless.
		 */
		default Color.Combination colorIdentity() {
			return Mana.Symbol.symbolsIn(this.rules())
					.map(Mana.Symbol::color)
					.collect(Color.Combination.COMBO_COLLECTOR)
					.plus(this.colorIndicator())
					.plus(this.manaCost().color());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's power.
		 * For cards with no power, this is NaN. Otherwise, this is the card's power,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's power.
		 */
		default double power() {
			return convertedValue(this.printedPower());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's toughness.
		 * For cards with no toughness, this is NaN. Otherwise, this is the card's toughness,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's toughness.
		 */
		default double toughness() {
			return convertedValue(this.printedToughness());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's loyalty.
		 * For cards with no loyalty, this is NaN. Otherwise, this is the card's loyalty,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's loyalty.
		 */
		default double loyalty() {
			return convertedValue(this.printedLoyalty());
		}

		/**
		 * Derived characteristic. The nearest number representation of the card's defense.
		 * For cards with no defense, this is NaN. Otherwise, this is the card's defense,
		 * taking any characteristic-defining abilities to be 0.
		 * @return Nearest number representation of the card's defense.
		 */
		default double defense() {
			return convertedValue(this.printedDefense());
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
			interface Frame {
				/**
				 * @return The percent of the width of this card from the left edge at which the left edge of this face's frame begins.
				 */
				double left();

				/**
				 * @return The percent of the width of this card from the right edge at which the right edge fo this face's frame begins.
				 */
				double right();

				/**
				 * @return The percent of the height of this card from the top edge at which the top edge of this face's frame begins.
				 */
				double top();

				/**
				 * @return The percent of the height of this card from the bottom edge at which the bottom edge of this face's frame begins.
				 */
				double bottom();

				/**
				 * @return The rotation, in increments of 90 degrees clockwise, to make this face's text right-side up.
				 */
				int rotation();
			}

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

			/**
			 * @return Whether or not this face is printed on the back of the cardboard.
			 */
			boolean onBack();

			/**
			 * @return The geometric location/orientation of this face on the full card.
			 */
			Frame frame();

			/**
			 * Returns true if the other printed face is fully visible in the normal bounds of this printed face.
			 * In practice, this means the other face is an Adventure in the bottom corner of this face.
			 * @param other The other printed face to check bounds on.
			 * @return True if the other printed face is fully visible in the normal bounds of this face, or false otherwise.
			 */
			default boolean contains(Printing.Face other) {
				return other != null &&
						other.printing().equals(this.printing()) &&
						other.onBack() == this.onBack() &&
						other.frame().left() >= frame().left() && other.frame().right() >= frame().right() &&
						other.frame().top() >= frame().top() && other.frame().bottom() >= frame().bottom();
			}
		}

		/**
		 * @return The card of which this printing is a printing.
		 */
		Card card();

		/**
		 * The set of all printed faces of this card. There should be one printed face for each face of
		 * <code>card()</code>.
		 *
		 * This should return printed faces in the same order as <code>card().faces()</code>.
		 *
		 * @return The set of this printing's printed faces.
		 */
		Set<? extends Face> faces();

		/**
		 * The set of printed faces of this card which contribute characteristics to the card's common state. There
		 * should be one printed face for each face in <code>card().mainFaces()</code>
		 *
		 * For example, this would contain only the front side of an MDFC, or only the creature part of an Adventure
		 * card, but would contain both halves of a split card.
		 *
		 * This should return printed faces in the same order as <code>card().mainFaces()</code>.
		 *
		 * @return The set of this card's faces which contribute to the card's common state characteristics.
		 */
		Set<? extends Face> mainFaces();

		/**
		 * Retrieves the face printing associated with the given card face. The given face must be an element of
		 * <code>card().faces()</code>.
		 * @param face The face to find the printing of.
		 * @throws IllegalArgumentException if the given face is not a part of this card.
		 * @return The printed version of that face.
		 */
		default Set<? extends Face> faces(Card.Face face) {
			Set<Face> faces = faces().stream()
					.filter(f -> face.equals(f.face()))
					.collect(Collectors.toSet());

			// TODO: Is this strictly true?
			if (faces.isEmpty()) throw new IllegalArgumentException(String.format("%s is not a face of %s", face.name(), card().name()));
			return faces;
		}

		/**
		 * @return The set in which this printing was printed.
		 */
		emi.lib.mtg.Set set();

		/**
		 * @return The rarity of this printing.
		 */
		Rarity rarity();

		/**
		 * @return The multiverseId of this printing. Null if the card has no multiverseid.
		 */
		Integer multiverseId();

		/**
		 * @return The variation index of this printing (i.e. the 1-indexed number of cards with this name in this set).
		 */
		int variation();

		/**
		 * @return The collector number of this card printing. Null if there is no collector number.
		 */
		String collectorNumber();

		/**
		 * @return The MTGO catalog ID of this printing. Null if the card isn't on MTGO, or if the data source doesn't know.
		 */
		Integer mtgoCatalogId();

		/**
		 * @return True if this is a promo card. These cards include datestamped prerelease promos, promo pack printings, and so on.
		 */
		boolean promo();

		/**
		 * @return The day this printing was first released. This can differ from the printing's set's releaseDate in some cases.
		 */
		LocalDate releaseDate();

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
	 * The set of faces of this card.
	 *
	 * The iteration order of the set returned by this method should be consistent between executions, and
	 * should be in full-card-name order.
	 * - For double-faced cards (including MDFCs), this is front-back.
	 * - For most split cards, this is left-right.
	 * - For Aftermath cards and Kamigawa flip cards, this is top-bottom.
	 * - For adventure cards, this is creature-adventure.
	 * If you're reading this in 2025 and they've added six new multifaced card archetypes, you'll have to figure it
	 * out for your application. Sorry.
	 *
	 * @return The set of this card's faces.
	 */
	Set<? extends Face> faces();

	/**
	 * The set of faces of this card which contribute characteristics to the card's common state. This must be a subset
	 * of faces().
	 *
	 * For example, this would contain only the front side of an MDFC, or only the creature part of an Adventure card,
	 * but would contain both halves of a split card.
	 *
	 * The iteration order of the set returned by this method should be consistent between executions, and
	 * should be in card-name order. See {@link #faces()} for details.
	 *
	 * @return The set of this card's faces which contribute to the card's common state characteristics.
	 */
	Set<? extends Face> mainFaces();

	/**
	 * For a given face,returns the set of all faces it can transform into. Why is this a set and not a single value?
	 * Because the Arena devs decided Specialize was a cool mechanic. :sob:
	 *
	 * @param source The source face for the transformation in question.
	 * @return The set of all faces the source face could transform into. Empty if the face doesn't transform.
	 */
	Set<? extends Face> transformed(Face source);

	/**
	 * For a given face, returns the face it flips into (rotate 180 degrees; this mechanic is as far as I know unique to
	 * Kamigawa flip cards and one Unhinged aura). Null if the given face doesn't flip.
	 *
	 * @param source The source face for the flip.
	 * @return The face the argument would flip into. May be null.
	 */
	Face flipped(Face source);

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
	 * Retrieves a printing by set code and collector number.
	 * @param setCode The set code of the printing to get.
	 * @param collectorNumber The collector number of the printing to get.
	 * @return The printing with that set code and collector number, or null if this card has no such printing.
	 */
	Printing printing(String setCode, String collectorNumber);

	/**
	 * Returns the front face of this card. For most Magic cards, this is the face you see when you open this card
	 * in a pack and read the name at the top. Dual-faced cards (whether transforming, modal, or other) return the front
	 * face. Adventure cards return the creature part. Split cards, which have two main faces, will return null.
	 * @return The front face of this card. Null if the card has no one front face (e.g. split cards).
	 */
	default Face front() {
		Iterator<? extends Face> iter = mainFaces().iterator();
		Face tmp = iter.hasNext() ? iter.next() : null;
		return iter.hasNext() ? null : tmp;
	}

	/**
	 * The set of faces this card's front face could transform into.
	 *
	 * @return The set of possible faces this card could transform into.
	 */
	default Set<? extends Face> transformedFaces() {
		Face front = front();
		return front == null ? Collections.emptySet() : transformed(front);
	}

	/**
	 * Returns the face of this card which would become active if an effect transformed this card, in the sense of
	 * transforming dual-faced cards.
	 * @return The face of this card which would become active if an effect transformed this card. Null if this card is
	 * not a transforming dual-faced card, or if this card has more than one transformed face (e.g. specialize).
	 */
	default Face transformed() {
		Iterator<? extends Face> iter = transformedFaces().iterator();
		Face tmp = iter.hasNext() ? iter.next() : null;
		return iter.hasNext() ? null : tmp;
	}

	/**
	 * Returns the face of this card which would become active if an effect flipped this card, in the sense of Kamigawa
	 * flip cards.
	 * @return The face of this card that would become active if an effect flipped it. Null if this card has no flipped
	 * face.
	 */
	default Face flipped() {
		Face front = front();
		return front == null ? null : flipped(front);
	}

	/**
	 * Returns the ordinary name of this card, formed by concatenating the names of its main faces with " // " (e.g.
	 * "Fire // Ice").
	 * @return The common name of this card.
	 */
	default String name() {
		return this.mainFaces().stream()
				.map(Face::name)
				.collect(Collectors.joining(" // "));
	}

	/**
	 * Returns the unabridged name of this card, formed by concatenating the names of its faces with " // " (e.g.
	 * "Fire // Ice").
	 * @return The complete name of this card.
	 */
	default String fullName() {
		return this.faces().stream()
				.map(Face::name)
				.collect(Collectors.joining(" // "));
	}

	/**
	 * Returns the ordinary mana cost of this card, formed by adding all mana symbols of the main face's mana costs
	 * together. Note that this does not reduce generic mana symbols.
	 * @return The usual mana cost of this card.
	 */
	default Mana.Value manaCost() {
		return this.mainFaces().stream()
				.map(Card.Face::manaCost)
				.collect(Mana.Value.NONCOMBINING_COLLECTOR);
	}

	/**
	 * Returns the full, combined mana cost of all faces of this card, formed by adding all mana symbols
	 * of each face's mana cost together. Note that this does not reduce generic symbols.
	 * @return The complete mana cost of this card.
	 */
	default Mana.Value fullManaCost() {
		return this.faces().stream()
				.map(Face::manaCost)
				.collect(Mana.Value.NONCOMBINING_COLLECTOR);
	}

	/**
	 * Returns the color identity of this card. This is the union of all faces' color identities.
	 * @return The color identity of this card.
	 */
	default Color.Combination colorIdentity() {
		return this.faces().stream()
				.map(Face::colorIdentity)
				.collect(Color.Combination.COMBO_COLLECTOR);
	}

	/**
	 * Returns this card's complete rules text. This is the text of each face, separated by two forward slashes.
	 * @return This card's complete rules text.
	 */
	default String rules() {
		return this.faces().stream()
				.map(Face::rules)
				.collect(Collectors.joining("\n\n//\n\n"));
	}

	/**
	 * State of a card's legality in any given format.
	 */
	enum Legality {
		/**
		 * This card was at one point legal, but has been banned (by English card name, normally).
		 */
		Banned,
		/**
		 * This card is not and has not been legal.
		 */
		NotLegal,
		/**
		 * No more than one of these cards is permitted in a deck.
		 */
		Restricted,
		/**
		 * This card is legal in the format.
		 */
		Legal,
		/**
		 * The data source for this card doesn't know this card's legality.
		 */
		Unknown;
	};

	/**
	 * Returns the card's legality in the given format.
	 * @param format The format to check for legality.
	 * @return The card's legality enum value in that format, or Unknown.
	 */
	Legality legality(Format format);
}

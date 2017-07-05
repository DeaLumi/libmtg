package emi.lib.mtg.card;

import emi.lib.mtg.characteristic.*;
import emi.lib.mtg.data.CardSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a physical Magic: the Gathering playing card.
 *
 * Note that this is distinct from the concept of a Magic card on Gatherer; on Gatherer, Fire is a different card from
 * Ice. Here, they are the left and right faces of the same card. On the other hand, Storm Crow in Eighth Edition is, in
 * this library, a different card from Storm Crow in Tenth Edition.
 */
public interface Card {

	/**
	 * @return The set this card is a part of. Must never be null.
	 */
	CardSet set();

	/**
	 * @return This card's full English name. Cards with multiple faces should have the names of their faces
	 * concatenated with two forward slashes (i.e. "Fire // Ice"). Must never be null.
	 */
	default String name() {
		return Arrays.stream(CardFace.Kind.values())
				.map(this::face)
				.filter(Objects::nonNull)
				.map(CardFace::name)
				.collect(Collectors.joining(" // "));
	}

	/**
	 * @return All of this card's colors. Cards with multiple faces should return the union of all faces' colors. Must
	 * never be null.
	 */
	default Set<Color> color() {
		return Arrays.stream(CardFace.Kind.values())
				.map(this::face)
				.filter(Objects::nonNull)
				.map(CardFace::color)
				.collect(() -> EnumSet.noneOf(Color.class), Set::addAll, Set::addAll);
	}

	/**
	 * @return All of this card's color identities. Cards with multiple faces should return the union of all faces'
	 * color identities. Must never be null.
	 */
	default Set<Color> colorIdentity() {
		return Arrays.stream(CardFace.Kind.values())
				.map(this::face)
				.filter(Objects::nonNull)
				.map(CardFace::colorIdentity)
				.collect(() -> EnumSet.noneOf(Color.class), Set::addAll, Set::addAll);
	}

	/**
	 * @return This card's rarity. Must never be null.
	 */
	CardRarity rarity();

	/**
	 * A convenience function to obtain the front face of this card. All cards have at least a front face.
	 *
	 * @return The front face of this card. Must never be null.
	 */
	default CardFace front() {
		return face(CardFace.Kind.Front);
	}

	/**
	 * @param kind The kind of card face to get.
	 * @return The face of that kind, or null if this card has no face of that kind.
	 */
	CardFace face(CardFace.Kind kind);

	/**
	 * @return The variation of this card, 1-indexed, or -1 if this card has no variations.
	 */
	int variation();

	/**
	 * @return A unique identifier (within lib.mtg) of this card. Must be safe to use as an unambiguous key.
	 */
	default UUID id() {
		return UUID.nameUUIDFromBytes(String.format("%s%s%s", set().code(), name(), variation()).getBytes());
	}

}

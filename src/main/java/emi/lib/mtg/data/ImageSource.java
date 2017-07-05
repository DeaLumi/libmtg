package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.card.CardFace;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a way for a program to obtain an image of a given card/card face.
 */
@Service
@Service.Property.String(name="name")
@Service.Property.Number(name="priority", required=false, value=0.5)
@FunctionalInterface
public interface ImageSource {
	/**
	 * Open an input stream for an image of this entire card. This should attempt to obtain a 'summary' image containing
	 * (ideally) all faces of the given card.
	 *
	 * @param card The card to open an image of.
	 * @return An InputStream to an image of the given card, or null if this provider couldn't find an image.
	 * @throws IOException if an error occurs while attempting to open the image.
	 */
	default InputStream open(Card card) throws IOException {
		return open(card.face(CardFace.Kind.Front));
	}

	/**
	 * Open an input stream for an image of a particular card face. This should attempt to obtain a face-specific image
	 * with only the given face kind displayed.
	 *
	 * Provider note: If your provider can't find an image of the particular given face, it should return null.
	 *
	 * Client note: Just because a provider returns null from this method doesn't mean it can't find any image of the
	 * card you passed. If it isn't detrimental to your purpose, consider querying the same source for its overall card
	 * image.
	 *
	 * @param face The face for which to obtain an image.
	 * @return An InputStream to an image of the specified face of the given card, or null if this provider couldn't
	 * find an image particular to this face of this card.
	 * @throws IOException if an error occurs while attempting to open the image.
	 */
	InputStream open(CardFace face) throws IOException;

	/**
	 * Attempts to open an input stream for an image of the particular card face. If an exception is thrown, this method
	 * catches it and returns null.
	 *
	 * @param face The face for which to obtain an image.
	 * @return An InputStream to an open image for that face of that card, or null if this provider couldn't find it or
	 * an exception occurred while attempting to open it.
	 */
	default InputStream openSafely(CardFace face) {
		try {
			return open(face);
		} catch (IOException ioe) {
			return null;
		}
	}

	/**
	 * Attempts to open an input stream for an image of an overall card. If an exception is thrown, this method catches
	 * it and returns null.
	 *
	 * @param card The card for which to obtain an image.
	 * @return An InputSTream to an open image for that face of that card, or null if this provider couldn't find it or
	 * an exception occurred while attempting to open it.
	 */
	default InputStream openSafely(Card card) {
		try {
			return open(card);
		} catch (IOException ioe) {
			return null;
		}
	}
}

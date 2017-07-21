package emi.lib.mtg.v2;

import emi.lib.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a source of Magic: the Gathering card and related object images.
 */
@Service
@Service.Property.String(name="name")
@Service.Property.Number(name="priority")
public interface ImageSource {

	/**
	 * Attempt to open an InputStream to an image of one face of a card.
	 *
	 * @param printing The printing of the card of which to find an image.
	 * @param face The face of the card of which to find an image.
	 * @return An InputStream that can be used to load a card face image, or null if it couldn't be found.
	 * @throws IOException if an error occurs while opening a card image the source located.
	 */
	InputStream open(Card.Printing printing, Card.Face face) throws IOException;

	/**
	 * Attempt to open an InputStream to an image of a card. Note that this is a fairly generic concept.
	 * For split cards, this should ideally include both halves. For transform cards, this should ideally
	 * only be the front face.
	 *
	 * @param printing The printing of the card of which to find an image.
	 * @return An InputStream that can be used to load a card image, or null if it couldn't be found.
	 * @throws IOException if an error occurs while opening a card image the source located.
	 */
	InputStream open(Card.Printing printing) throws IOException;

}

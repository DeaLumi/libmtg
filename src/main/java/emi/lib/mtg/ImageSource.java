package emi.lib.mtg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

/**
 * Represents a source of Magic: the Gathering card and related object images.
 */
public interface ImageSource {

	/**
	 * Provides a hint to clients as to the ordering of image sources to try.
	 * Disk-based sources may propose a higher priority, for instance, to avoid network usage.
	 * @return A number indicating this source's priority.
	 */
	int priority();

	/**
	 * Attempt to open an an image of the given card printing. Ideally, this means, "show me what I'd see if I opened a
	 * pack and this was on top". Most cards should show just... the card. Transforms and melds should show the front
	 * face. Split cards should show both halves, oriented sideways. So on.
	 *
	 * @param printing The printing of the card of which to find an image.
	 * @return An InputStream that can be used to load a card image, or null if it couldn't be found.
	 * @throws IOException if an error occurs while opening a card image the source located.
	 */
	BufferedImage open(Card.Printing printing) throws IOException;

	/**
	 * Attempt to open an image of a card face. Ideally, this will find an image of that face and only that face,
	 * oriented so that face's name and text are upright.
	 *
	 * @param facePrint The printing of the face for which to find an image.
	 * @return An input stream to an image of that face, or null if this source couldn't find such an image.
	 * @throws IOException if an error occurs while opening a card image the source located.
	 */
	BufferedImage open(Card.Printing.Face facePrint) throws IOException;

	/**
	 * Attempt to open an an image of the given card. Ideally, this means, "show me what I'd see if I opened a pack and
	 * this was on top". Most cards should show just... the card. Transforms and melds should show the front face. Split
	 * cards should show both halves, oriented sideways. So on.
	 *
	 * @param card The card for which to obtain an image.
	 * @return An input stream to an image of this card, or null if no image could be found by this source.
	 * @throws IOException if an error occurs while opening the input stream.
	 */
	default BufferedImage open(Card card) throws IOException {
		Iterator<? extends Card.Printing> printings = card.printings().iterator();

		if (printings.hasNext()) {
			return open(printings.next());
		} else {
			return null;
		}
	}

	/**
	 * Attempt to open an image of a particular card face. Ideally, this will find an image of that face only, oriented
	 * so that the face's name and text are upright.
	 *
	 * @param card The card owning the face for which to find an image.
	 * @param face The face for which to find an image.
	 * @return An input stream to an image of that face, or null if this source couldn't locate such an image.
	 * @throws IOException if an error occurs while opening the input stream.
	 */
	default BufferedImage open(Card card, Card.Face face) throws IOException {
		Iterator<? extends Card.Printing> printings = card.printings().iterator();

		if (printings.hasNext()) {
			return open(printings.next().face(face.kind()));
		} else {
			return null;
		}
	}

	/**
	 * Returns true if images opened through this ImageSource can be cached.
	 *
	 * In general, an ImageSource reads from a URL or other remote source and passes the image in memory to the client
	 * application, which may wish to cache that image on disk for future executions. If an ImageSource's results should
	 * not be used in this way, it must override this method to return false. (Reasons for specifying this might include
	 * an intent that its images be used only temporarily until another ImageSource can find something, or if the source
	 * of the image is already on disk.)
	 *
	 * @return True if images opened through this ImageSource can be cached. False otherwise.
	 */
	default boolean cacheable() {
		return true;
	}

}

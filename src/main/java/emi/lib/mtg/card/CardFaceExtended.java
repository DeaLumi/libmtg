package emi.lib.mtg.card;

import java.net.URL;

/**
 * An extended CardFace definition. Some card sources can provide additional information; implementing this interface
 * allows these sources to report it. These methods are all considered 'optional', so clients looking to use these
 * should be prepared for a CardFace instance to *not* implement this!
 */
public interface CardFaceExtended extends CardFace {
	/**
	 * @return The multiverseid of this card face, or -1 if this card face doesn't have a multiverseid.
	 */
	int multiverseId();

	/**
	 * @return The collector number of this card face. Can be an empty string if the card face wasn't assigned a number.
	 */
	String collectorNumber();

}

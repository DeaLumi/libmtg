package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.CardFace;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Emi on 6/16/2017.
 */
@Service
@Service.Property.String(name="name")
@Service.Property.Number(name="priority", required=false, value=0.5)
@FunctionalInterface
public interface ImageSource {
	InputStream open(CardFace cardFace) throws IOException;

	default InputStream openSafely(CardFace cardFace) {
		try {
			return open(cardFace);
		} catch (IOException ioe) {
			return null;
		}
	}
}

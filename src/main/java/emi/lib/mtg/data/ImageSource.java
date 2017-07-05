package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
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
	default InputStream open(Card card) throws IOException {
		return open(card, CardFace.Kind.Front);
	}

	InputStream open(Card card, CardFace.Kind face) throws IOException;

	default InputStream openSafely(Card card) {
		try {
			return open(card);
		} catch (IOException ioe) {
			return null;
		}
	}
}

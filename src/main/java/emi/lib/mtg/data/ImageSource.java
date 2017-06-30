package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.Card;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Emi on 6/16/2017.
 */
@Service
@Service.Property.String(name="name")
@Service.Property.Number(name="priority", required=false, value=0.5)
@FunctionalInterface
public interface ImageSource {
	InputStream open(Card card) throws IOException;

	default InputStream openSafely(Card card) {
		try {
			return open(card);
		} catch (IOException ioe) {
			return null;
		}
	}
}

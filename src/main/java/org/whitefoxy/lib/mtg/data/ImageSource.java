package org.whitefoxy.lib.mtg.data;

import emi.lib.Service;
import org.whitefoxy.lib.mtg.card.Card;

import java.net.URL;

/**
 * Created by Emi on 6/16/2017.
 */
@Service
@Service.Property.String(name="name")
@FunctionalInterface
public interface ImageSource {
	URL find(Card card);
}

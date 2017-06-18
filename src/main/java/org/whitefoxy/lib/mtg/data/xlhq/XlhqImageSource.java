package org.whitefoxy.lib.mtg.data.xlhq;

import emi.lib.Service;
import org.whitefoxy.lib.mtg.card.Card;
import org.whitefoxy.lib.mtg.data.ImageSource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Emi on 6/16/2017.
 */
@Service.Provider(ImageSource.class)
@Service.Property.String(name="name", value="XLHQ")
public class XlhqImageSource implements ImageSource {
	@Override
	public URL find(Card card) {
		if (card == null) {
			return null;
		}

		File f = new File(new File(String.format("s%s", card.set().code())), String.format("%s%s.xlhq.jpg", card.name(), card.variation() == 0 ? "" : Integer.toString(card.variation())));

		if (!f.exists()) {
			return null;
		}

		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			assert false : "This shouldn't really be possible. The file exists...";
			return null;
		}
	}
}

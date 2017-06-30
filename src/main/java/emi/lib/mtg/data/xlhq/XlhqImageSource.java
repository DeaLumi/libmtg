package emi.lib.mtg.data.xlhq;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.data.ImageSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Emi on 6/16/2017.
 */
@Service.Provider(ImageSource.class)
@Service.Property.String(name="name", value="XLHQ")
@Service.Property.Number(name="priority", value=1.0)
public class XlhqImageSource implements ImageSource {
	private static final File PARENT_FILE = new File(new File("images"), "xlhq");

	static {
		if (!PARENT_FILE.exists() && !PARENT_FILE.mkdirs()) {
			throw new Error("Couldn't create parent directory for XLHQ images...");
		}
	}

	private File file(Card card) {
		File setDir = new File(PARENT_FILE, String.format("s%s", card.set().code()));

		File cardFile;
		if (card.variation() == 0) {
			cardFile = new File(setDir, String.format("%s.xlhq.jpg", card.name()));
		} else {
			cardFile = new File(setDir, String.format("%s%d.xlhq.jpg", card.name(), card.variation()));
		}

		return cardFile;
	}

	@Override
	public InputStream open(Card card) throws IOException {
		if (card == null) {
			return null;
		}

		File f = file(card);
		return f.exists() ? new FileInputStream(f) : null;
	}
}

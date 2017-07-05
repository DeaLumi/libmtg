package emi.lib.mtg.data.xlhq;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.card.CardFace;
import emi.lib.mtg.data.ImageSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

	private File file(Card card, CardFace.Kind face) {
		File setDir = new File(PARENT_FILE, String.format("s%s", card.set().code()));

		File cardFile;
		if (card.variation() == 0) {
			cardFile = new File(setDir, String.format("%s.xlhq.jpg", face.name()));
		} else {
			cardFile = new File(setDir, String.format("%s%d.xlhq.jpg", face.name(), card.variation()));
		}

		return cardFile;
	}

	@Override
	public InputStream open(Card card, CardFace.Kind face) throws IOException {
		if (card == null) {
			return null;
		}

		File f = file(card, face);
		return f.exists() ? new FileInputStream(f) : null;
	}
}

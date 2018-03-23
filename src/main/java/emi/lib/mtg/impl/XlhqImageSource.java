package emi.lib.mtg.impl;

import emi.lib.Service;
import emi.lib.mtg.Card;
import emi.lib.mtg.ImageSource;
import emi.lib.mtg.img.MtgAwtImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service.Provider(ImageSource.class)
@Service.Property.String(name="name", value="XLHQ Images Torrent")
@Service.Property.Number(name="priority", value=0.95)
public class XlhqImageSource implements ImageSource {
	private static final File PARENT = new File(new File("images"), "xlhq");

	private File file(Card.Printing print) {
		File setFile = new File(PARENT, String.format("s%s", print.set().code().toUpperCase()));

		if (!setFile.isDirectory()) {
			return null;
		}

		String name;
		if (print.face(Card.Face.Kind.Right) != null) {
			name = String.format("%s - %s", print.face(Card.Face.Kind.Left).face().name(), print.face(Card.Face.Kind.Right).face().name());
		} else if (print.face(Card.Face.Kind.Flipped) != null) {
			name = String.format("%s_%s", print.face(Card.Face.Kind.Front).face().name(), print.face(Card.Face.Kind.Flipped).face().name());
		} else {
			name = print.card().name();
		}

		File file = new File(setFile, name + ".xlhq.jpg");

		if (!file.exists()) {
			name += print.variation();

			file = new File(setFile, name + ".xlhq.jpg");
		}

		return file;
	}

	@Override
	public BufferedImage open(Card.Printing printing) throws IOException {
		File file = file(printing);

		if (file != null && file.isFile()) {
			return ImageIO.read(file);
		}

		return null;
	}

	@Override
	public BufferedImage open(Card.Printing.Face facePrint) throws IOException {
		BufferedImage source = open(facePrint.printing());

		if (source != null) {
			return MtgAwtImageUtils.faceFromFull(facePrint, source);
		}

		return null;
	}

	@Override
	public boolean cacheable() {
		return false;
	}
}

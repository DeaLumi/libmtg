package emi.lib.mtg.data;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.card.CardFace;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;

@Deprecated
public abstract class DiskBackedImageSource implements ImageSource {
	protected static final File PARENT_FILE = new File("images");

	static {
		if (!PARENT_FILE.exists() && !PARENT_FILE.mkdirs()) {
			throw new Error("Couldn't create 'images' subdirectory.");
		}
	}

	protected abstract String name();

	protected File parentDir() {
		File parent = new File(PARENT_FILE, name());

		if (!parent.exists() && !parent.mkdirs()) {
			throw new Error(String.format("Couldn't create '%s' subdirectory.", name()));
		}

		return parent;
	}

	protected InputStream openInternal(Card card) throws IOException {
		return openInternal(card.face(CardFace.Kind.Front));
	}

	protected abstract InputStream openInternal(CardFace face) throws IOException;

	protected File file(Card card) throws IOException {
		File setDir = new File(parentDir(), String.format("s%s", card.set().code()));

		if (!setDir.exists() && !setDir.mkdirs()) {
			throw new IOException("Couldn't create parent subdirectory for set " + card.set().code());
		}

		return new File(setDir, String.format("%s%s.jpg", card.name(), card.variation() < 0 ? "" : Integer.toString(card.variation() + 1)));
	}

	protected File file(CardFace face) throws IOException {
		File setDir = new File(parentDir(), String.format("s%s", face.card().set().code()));

		if (!setDir.exists() && !setDir.mkdirs()) {
			throw new IOException("Couldn't create parent subdirectory for set " + face.card().set().code());
		}

		return new File(setDir, String.format("%s%s.jpg", face.name(), face.card().variation() < 0 ? "" : Integer.toString(face.card().variation() + 1)));
	}

	@Override
	public InputStream open(Card card) throws IOException {
		File f = file(card);

		if (!f.exists()) {
			InputStream source = openInternal(card);

			if (source == null) {
				return null;
			}

			OutputStream of = new FileOutputStream(f);
			byte[] buffer = new byte[4096];
			int read;
			while ((read = source.read(buffer)) >= 0) {
				of.write(buffer, 0, read);
			}
			of.close();
		}

		return new FileInputStream(f);
	}

	@Override
	public InputStream open(CardFace face) throws IOException {
		File f = file(face);

		if (!f.exists()) {
			InputStream source = openInternal(face);

			if (source == null) {
				return null;
			}

			OutputStream of = new FileOutputStream(f);
			byte[] buffer = new byte[4096];
			int read;
			while ((read = source.read(buffer)) >= 0) {
				of.write(buffer, 0, read);
			}
			of.close();
			source.close();
		}

		return new FileInputStream(f);
	}
}

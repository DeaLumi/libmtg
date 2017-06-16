package org.whitefoxy.lib.mtg.data.mtgjson;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.whitefoxy.lib.mtg.card.Card;
import org.whitefoxy.lib.mtg.characteristic.*;
import org.whitefoxy.lib.mtg.characteristic.impl.BasicCardTypeLine;
import org.whitefoxy.lib.mtg.characteristic.impl.BasicManaCost;
import org.whitefoxy.lib.mtg.data.CardSet;
import org.whitefoxy.lib.mtg.data.CardSource;

import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by Emi on 5/7/2017.
 */
@AutoService(CardSource.class)
public class MtgJsonCardSource implements CardSource {
	public static class WriteOnce<T> {
		private T value;

		public WriteOnce() {
			this.value = null;
		}

		public void value(T newValue) {
			assert this.value == null && newValue != null;

			this.value = newValue;
		}

		public T value() {
			return this.value;
		}
	}

	public static class CardSet implements org.whitefoxy.lib.mtg.data.CardSet {
		public static class Card implements org.whitefoxy.lib.mtg.card.Card {

			private WriteOnce<CardSet> cardSet;
			private String name;
			private BasicManaCost manaCost;
			private Set<Color> colors;
			private BasicCardTypeLine type;
			private String rarity;
			private String text;
			private String flavor;
			private String power;
			private String toughness;
			private String loyalty;
			private String number;
			private String id;
			private String imageName;

			public Card() {
				this.cardSet = new WriteOnce<>();
				this.name = null;
				this.manaCost = null;
				this.colors = null;
				this.type = null;
				this.rarity = null;
				this.text = null;
				this.flavor = null;
				this.power = null;
				this.toughness = null;
				this.loyalty = null;
				this.number = null;
				this.id = null;
				this.imageName = null;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public BasicManaCost manaCost() {
				return manaCost;
			}

			@Override
			public URL illustration() {
				try {
					File f = new File(new File(String.format("s%s", cardSet.value().code())), String.format("%s.xlhq.jpg", this.imageName));
					System.err.println(f.getAbsolutePath());

					if (!f.exists()) {
						f = new File("Back.xlhq.jpg");
					}

					return f.toURI().toURL();
				} catch (MalformedURLException mue) {
					throw new Error(mue);
				}
			}

			@Override
			public Set<Color> color() {
				return colors;
			}

			@Override
			public Set<Color> colorIdentity() {
				return colors; // TODO: fix this!
			}

			@Override
			public CardRarity rarity() {
				return CardRarity.valueOf(rarity);
			}

			@Override
			public CardTypeLine type() {
				return type;
			}

			@Override
			public String text() {
				return text;
			}

			@Override
			public String flavor() {
				return flavor;
			}

			@Override
			public String power() {
				return power;
			}

			@Override
			public String toughness() {
				return toughness;
			}

			@Override
			public String loyalty() {
				return loyalty;
			}

			@Override
			public String collectorNumber() {
				return number;
			}

			@Override
			public org.whitefoxy.lib.mtg.data.CardSet set() {
				return cardSet.value();
			}

			@Override
			public UUID id() {
				return UUID.nameUUIDFromBytes(("mtgjson.com:" + id).getBytes());
			}
		}

		public CardSet() {
			this.name = null;
			this.code = null;
			this.cards = null;
		}

		public final String name, code;
		public final Set<Card> cards;

		@Override
		public String name() {
			return name;
		}

		@Override
		public String code() {
			return code;
		}

		@Override
		public Collection<Card> cards() {
			return cards;
		}

		protected void linkCards() {
			for (Card card : cards()) {
				card.cardSet.value(this);
			}
		}
	}

	private final static TypeAdapter<CardTypeLine> cardTypeLineAdapter = new TypeAdapter<CardTypeLine>() {
		@Override
		public void write(JsonWriter out, CardTypeLine value) throws IOException {
			StringBuilder sb = new StringBuilder();

			for (Supertype st : value.supertypes()) {
				sb.append(st).append(' ');
			}

			for (CardType ct : value.cardTypes()) {
				sb.append(ct).append(' ');
			}

			sb.append('—');

			for (String st : value.subtypes()) {
				sb.append(' ').append(st);
			}

			out.value(sb.toString());
		}

		@Override
		public CardTypeLine read(JsonReader in) throws IOException {
			return BasicCardTypeLine.parse(in.nextString());
		}
	};

	private final static TypeAdapter<ManaCost> manaCostAdapter = new TypeAdapter<ManaCost>() {

		@Override
		public void write(JsonWriter out, ManaCost value) throws IOException {
			StringBuilder sb = new StringBuilder();

			for (ManaSymbol symbol : value.symbols()) {
				sb.append(symbol.toString());
			}

			out.value(sb.toString());
		}

		@Override
		public ManaCost read(JsonReader in) throws IOException {
			return new BasicManaCost(in.nextString());
		}
	};

	private final static URL version;
	private final static URL allSets;

	private final static String[] skipSets = { "UGL", "UNH", "pCEL" };

	static {
		try {
			version = new URL("http://mtgjson.com/json/version.json");
			allSets = new URL("http://mtgjson.com/json/AllSets.json");
		} catch (MalformedURLException mue) {
			throw new Error(mue);
		}
	}

	private final Gson gson;
	private final Map<String, CardSet> sets;
	private final Map<UUID, Card> cards;

	private void downloadFile(URL remote, String local) throws IOException {
		try (InputStream download = remote.openStream(); OutputStream writeOut = new FileOutputStream(local)) {
			byte[] block = new byte[4096];
			int read;
			while ((read = download.read(block)) >= 0) {
				writeOut.write(block, 0, read);
			}
		}
	}

	public MtgJsonCardSource() throws IOException {
		this.gson = new GsonBuilder()
				.registerTypeHierarchyAdapter(CardTypeLine.class, cardTypeLineAdapter)
				.registerTypeHierarchyAdapter(ManaCost.class, manaCostAdapter)
				.create();

		String localVersion = null, remoteVersion = null;
		try (Reader reader = new InputStreamReader(new FileInputStream("version.json"))) {
			localVersion = this.gson.fromJson(reader, String.class);
		} catch (IOException ioe) {
			// do nothing
		}

		try (Reader reader = new InputStreamReader(version.openStream())) {
			remoteVersion = this.gson.fromJson(reader, String.class);
		} catch (IOException ioe) {
			// do nothing
		}

		if (remoteVersion != null && (localVersion == null || !localVersion.equals(remoteVersion))) {
			downloadFile(allSets, "AllSets.json");
			downloadFile(version, "version.json");
		}

		try (Reader reader = new InputStreamReader(new FileInputStream("AllSets.json"))) {
			JsonReader jreader = new JsonReader(reader);

			this.sets = new HashMap<>();
			this.cards = new HashMap<>();
			jreader.beginObject();

			while (jreader.hasNext()) {
				String setKey = jreader.nextName();

				if (Arrays.stream(skipSets).filter(setKey::equals).findAny().isPresent()) {
					jreader.skipValue();
					System.err.println("Skipping " + setKey);
				} else {
					CardSet set = this.gson.fromJson(jreader, CardSet.class);
					this.sets.put(setKey, set);

					for (Card c : set.cards()) {
						cards.put(c.id(), c);
					}
				}
			}

			jreader.endObject();
		}

		sets().forEach(CardSet::linkCards);
	}

	@Override
	public Collection<CardSet> sets() {
		return sets.values();
	}

	@Override
	public Card get(UUID id) {
		return cards.get(id);
	}

	public static void main(String[] args) throws Exception {
		CardSource source = new MtgJsonCardSource();

		for (org.whitefoxy.lib.mtg.data.CardSet set : source.sets()) {
			System.out.println("Set " + set.name() + " (" + set.code() + "; " + set.cards().size() + " cards):");

			for (org.whitefoxy.lib.mtg.card.Card card : set.cards()) {
				System.out.println(" " + card.name() + " (" + (card.manaCost() != null ? card.manaCost().toString() : "<no mana cost>") + ")");
			}
		}

		System.out.println("Total " + source.sets().stream().mapToInt(s -> s.cards().size()).sum() + " cards.");
		System.out.println("Highest CMC: " + source.sets().stream().flatMapToInt(s -> s.cards().stream().mapToInt(c -> c.manaCost() != null ? c.manaCost().convertedCost() : 0)).summaryStatistics().getMax());

		System.out.print('[');
		for (int i = 0; i <= 16; ++i) {
			final int finalI = i;
			long count = source.cards().stream().filter(c -> (c.manaCost() != null ? c.manaCost().convertedCost() : 0) == finalI).count();
			System.out.print(count);
			System.out.print(',');
		}
		System.out.print(']');

	}

}

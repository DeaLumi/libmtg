package emi.lib.mtg.data.scryfall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emi.lib.Service;
import emi.lib.mtg.card.CardFace;
import emi.lib.mtg.card.CardFaceExtended;
import emi.lib.mtg.data.DiskBackedImageSource;
import emi.lib.mtg.data.ImageSource;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service.Provider(ImageSource.class)
@Service.Property.String(name="name", value="Scryfall")
@Service.Property.Number(name="priority", value=0.25)
public class ScryfallImageSource extends DiskBackedImageSource {
	private static final Gson GSON = new GsonBuilder().create();

	private static final long DOWNLOAD_PAUSE = 250; // 0.25 seconds between images

	private final ScheduledExecutorService downloader; // One image at a time.
	private volatile long nextDownload = System.currentTimeMillis();

	public ScryfallImageSource() {
		this.downloader = new ScheduledThreadPoolExecutor(1, r -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("Scryfall Image Downloader");
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	protected String name() {
		return "scryfall";
	}

	@Override
	protected InputStream openInternal(CardFace face) throws IOException {
		if (face == null) {
			return null;
		}

		File f = file(face);

		if (f.exists()) {
			return new FileInputStream(f);
		}

		final long targetTime;
		synchronized (this) {
			targetTime = nextDownload = Math.max(nextDownload, System.currentTimeMillis()) + DOWNLOAD_PAUSE;
		}

		ScheduledFuture<InputStream> result = this.downloader.schedule(() -> {
			try {
				URL url;
				if ((face instanceof CardFaceExtended) && ((CardFaceExtended) face).multiverseId() >= 0) {
					CardFaceExtended ex = (CardFaceExtended) face;

					url = new URL(String.format("https://api.scryfall.com/cards/multiverse/%d?format=image", ex.multiverseId()));
				} else {
					// Guh. This is messy. I may decide to scratch this and just return...

					String searchString = String.format("++%s set:%s", face.card().name(), face.card().set().code());
					URL search = new URL(String.format("https://api.scryfall.com/search?q=%s", URLEncoder.encode(searchString, "UTF-8")));

					HttpsURLConnection searchConn = (HttpsURLConnection) search.openConnection();
					searchConn.connect();

					if (searchConn.getResponseCode() != 200) {
						System.err.println("Couldn't load search results for " + searchString);
						return null;
					}

					Map results = GSON.fromJson(new InputStreamReader(searchConn.getInputStream()), Map.class);

					if (!results.containsKey("object") || !"list".equals(results.get("object"))) {
						System.err.println("Search results weren't a list.");
						return null;
					}

					if (face.card().variation() >= 0 && (int) results.get("total_cards") <= 1) {
						System.err.println("Search results don't include variations of cards.");
						return null;
					}

					if (!results.containsKey("data") || !(results.get("data") instanceof List)) {
						System.err.println("Search result data wasn't a List.");
						return null;
					}

					Map cardObject = (Map) ((List) results.get("data")).get(face.card().variation() > 0 ? face.card().variation() : 0);

					if (!cardObject.containsKey("image_uri") || !(cardObject.get("image_uri") instanceof String)) {
						System.err.println("Card object didn't contain image_uri.");
						return null;
					}

					url = new URL((String) cardObject.get("image_uri"));
				}

				HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
				conn.connect();

				if (conn.getResponseCode() != 200) {
					System.err.println(String.format("Couldn't download image of %s/%s from Scryfall: %s", face.name(), face.card().set().code(), conn.getResponseMessage()));
					return null;
				}

				return url.openStream();
			} catch (MalformedURLException e) {
				throw new AssertionError(e); // Shouldn't happen
			}
		}, targetTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

		try {
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
}

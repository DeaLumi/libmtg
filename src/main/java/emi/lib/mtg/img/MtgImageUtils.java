package emi.lib.mtg.img;

import emi.lib.mtg.Card;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MtgImageUtils {
	public static final double ROUND_RADIUS_FRACTION = 3.0 / 63.0; // 3mm out of 63 x 88

	public static Image clearCorners(Image source) {
		PixelReader reader = source.getPixelReader();

		if (reader.getColor(0, 0).getOpacity() <= 0.05) {
			return source;
		} else {
			WritableImage dst = new WritableImage((int) source.getWidth(), (int) source.getHeight());
			PixelWriter writer = dst.getPixelWriter();

			final double radius = Math.min(source.getWidth(), source.getHeight()) * ROUND_RADIUS_FRACTION;
			final int radUp = (int) Math.ceil(radius);
			final int radDn = (int) Math.floor(radius);

			for (int y = 0; y < source.getHeight(); ++y) {
				for (int x = 0; x < source.getWidth(); ++x) {
					// possibly modify color if near corner
					double ux = -1, uy = -1;
					if (x <= radius) {
						ux = x;
					} else if (x >= dst.getWidth() - radius) {
						ux = dst.getWidth() - x;
					}

					if (y <= radius) {
						uy = y;
					} else if (y >= dst.getHeight() - radius) {
						uy = dst.getHeight() - y;
					}

					if (ux >= 0 && uy >= 0) {
						double dx = ux - radius, dy = uy - radius;
						double d = Math.sqrt(dx * dx + dy * dy);
						double dd = Math.max(radius - 0.5, Math.min(d, radius + 0.5));
						Color c = reader.getColor(x, y);
						c = c.interpolate(Color.TRANSPARENT, dd - radius + 0.5);
						dst.getPixelWriter().setColor(x, y, c);
					}
				}
			}

			writer.setPixels(radUp, 0, (int) dst.getWidth() - 2*radUp, (int) dst.getHeight(), reader, radUp, 0);
			writer.setPixels(0, radDn, radUp, (int) dst.getHeight() - 2*radDn, reader, 0, radDn);
			writer.setPixels((int) dst.getWidth() - radUp, radDn, radUp, (int) dst.getHeight() - 2*radDn, reader, (int) dst.getWidth() - radUp, radDn);

			return dst;
		}
	}

	private static final ExecutorService IMAGE_RESIZE_POOL = Executors.newCachedThreadPool(r -> {
		Thread th = Executors.defaultThreadFactory().newThread(r);
		th.setName("LibMtg-ImageResize-" + th.getId());
		th.setDaemon(true);
		return th;
	});

	// TODO: I hate this. Write my own blur/Lanczos downsampler?
	public static Image scaled(Image source, double w, double h, boolean smooth) {
		try {
			PipedOutputStream output = new PipedOutputStream();
			PipedInputStream input = new PipedInputStream(output);

			IMAGE_RESIZE_POOL.submit(() -> ImageIO.write(SwingFXUtils.fromFXImage(source, null), "png", output));
			return new Image(input, w, h, true, smooth);
		} catch (IOException ioe) {
			return null;
		}
	}

	public static Image subsection(Image source, int x, int y, int w, int h) {
		return new WritableImage(source.getPixelReader(), x, y, w, h);
	}

	public static Image rotated(Image source, double rotation) {
		// TODO: Do this off the JavaFX application thread, please.
		ImageView view = new ImageView(source);
		view.setRotate(rotation);

		AtomicReference<Image> image = new AtomicReference<>(null);

		Platform.runLater(() -> image.set(view.snapshot(null, null)));

		while (image.get() == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
				break;
			}
		}

		return image.get();
	}

	public static Image faceFromFull(Image source, Card.Face.Kind kind) {
		switch (kind) {
			case Front:
				return source;
			case Transformed:
				return source;
			case Flipped:
				return rotated(source, 180.0);
			case Left:
				return scaled(rotated(subsection(source, 0, 0, (int) source.getWidth(), (int) (source.getHeight() / 2)), 90.0), source.getWidth(), source.getHeight(), true);
			case Right:
				return scaled(rotated(subsection(source, 0, (int) (source.getHeight() / 2), (int) source.getWidth(), (int) (source.getHeight() / 2)), 90.0), source.getWidth(), source.getHeight(), true);
			case Other:
			default:
				throw new IllegalArgumentException();
		}
	}
}

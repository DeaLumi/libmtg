package emi.lib.mtg.img;

import emi.lib.mtg.Card;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

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

	private static ExecutorService daemonPool(String prefix) {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1, r -> {
			Thread th = Executors.defaultThreadFactory().newThread(r);
			th.setName(prefix + th.getId());
			th.setDaemon(true);
			return th;
		});
	}

	private static final ExecutorService IMAGE_RESIZE_POOL = daemonPool("LibMtg-ImageResize-");

	private static final ExecutorService IMAGE_OP_POOL = daemonPool("LibMtg-ImageOp-");

	public static DoubleBinaryOperator gaussian(double sigma) {
		return (x, y) -> {
			final double sigmasq2 = sigma*sigma*2;

			return 1/(Math.PI*sigmasq2)*Math.exp(-(x*x + y*y)/sigmasq2);
		};
	}

	public static DoubleBinaryOperator simplifiedGaussian(double sigma) {
		final double[] precomp = new double[(int) (2*Math.ceil(sigma) + 1)];

		for (int i = 0; i < precomp.length; ++i) {
			precomp[i] = 1/Math.sqrt(Math.PI*sigma*sigma)*Math.exp(-i/(2*sigma));
		}

		return (x, y) -> {
			final int r = (int) Math.round(Math.sqrt(x*x + y*y));

			if (r >= precomp.length) {
				return 0.0;
			} else {
				return precomp[r];
			}
		};
	}

	public static DoubleBinaryOperator lanczos(int a) {
		return (x, y) -> {
			final double r = Math.sqrt(x*x + y*y);
			if (r < -a || r > a) {
				return 0.0;
			} else if (r == 0.0) {
				return 1.0;
			} else {
				final double pir = Math.PI*r;

				return a*Math.sin(pir)*Math.sin(pir/a)/(pir*pir);
			}
		};
	}

	public static Image convolve(Image source, DoubleBinaryOperator kernel, int a) {
		return resample(source, (int) source.getWidth(), (int) source.getHeight(), kernel, a);
	}

	public static WritableImage resample(Image source, int w, int h, DoubleBinaryOperator kernel, int a) {
		WritableImage destination = new WritableImage(w, h);

		PixelReader reader = source.getPixelReader();
		PixelWriter writer = destination.getPixelWriter();
		WritablePixelFormat<IntBuffer> pixelFormat = WritablePixelFormat.getIntArgbPreInstance();

		Set<Future<?>> completions = new HashSet<>(w*h);
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				final int destX = x;
				final int destY = y;

				final double sourceX = destX * (source.getWidth() / w);
				final double sourceY = destY * (source.getHeight() / h);

				final int windowX = Math.max(0, (int) Math.floor(sourceX) - a + 1);
				final int windowY = Math.max(0, (int) Math.floor(sourceY) - a + 1);

				final int windowW = Math.min((int) Math.floor(sourceX) + a, (int) source.getWidth()) - windowX;
				final int windowH = Math.min((int) Math.floor(sourceY) + a, (int) source.getHeight()) - windowY;

				final double cx = sourceX - windowX;
				final double cy = sourceY - windowY;

				completions.add(IMAGE_OP_POOL.submit(() -> {
					int[] buffer = new int[windowW*windowH];
					reader.getPixels(windowX, windowY, windowW, windowH, pixelFormat, buffer, 0, windowW);

					double accumR = 0, accumG = 0, accumB = 0, accumA = 0;
					double accumF = 0.0;
					for (int ay = 0; ay < windowH; ++ay) {
						for (int ax = 0; ax < windowW; ++ax) {
							int argb = buffer[ay*windowW + ax];
							int alpha = (argb >> 24) & 0xFF;
							int r = (argb >> 16) & 0xFF;
							int g = (argb >> 8) & 0xFF;
							int b = argb & 0xFF;

							double f = kernel.applyAsDouble(cx - ax, cy - ay);

							accumF += f;
							accumA += alpha * f;
							accumR += r * f;
							accumG += g * f;
							accumB += b * f;
						}
					}

					accumA = Math.max(0, Math.min((int) (accumA / accumF), 0xFF));
					accumR = Math.max(0, Math.min((int) (accumR / accumF), 0xFF));
					accumG = Math.max(0, Math.min((int) (accumG / accumF), 0xFF));
					accumB = Math.max(0, Math.min((int) (accumB / accumF), 0xFF));

					int packed = 0;
					packed |= ((int) accumA << 24) & 0xFF000000;
					packed |= ((int) accumR << 16) & 0x00FF0000;
					packed |= ((int) accumG << 8) & 0x0000FF00;
					packed |= ((int) accumB) & 0x000000FF;

					writer.setArgb(destX, destY, packed);
				}));
			}
		}

		try {
			for (Future<?> future : completions) {
				future.get();
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			return destination;
		} catch (ExecutionException ee) {
			ee.getCause().printStackTrace();
			throw new Error(ee.getCause());
		}

		return destination;
	}

	public static int lanczosCount = 0, imageCount = 0;
	public static double lanczosTime = 0.0, imageTime = 0.0;
	private static boolean lanczos = false;

	// TODO: I hate this. Write my own blur/Lanczos downsampler?
	public static Image scaled(Image source, double w, double h, boolean smooth) {
		lanczos = !lanczos;

		Image out = source;
		long start = System.nanoTime();
		if (lanczos) {
			++lanczosCount;
			if (smooth) {
				out = convolve(out, simplifiedGaussian(1.0), 3);
			}
			out = resample(out, (int) w, (int) h, lanczos(3), 3);
			lanczosTime += (System.nanoTime() - start) / 1e9;
		} else {
			++imageCount;
			try {
				PipedOutputStream output = new PipedOutputStream();
				PipedInputStream input = new PipedInputStream(output);

				IMAGE_RESIZE_POOL.submit(() -> ImageIO.write(SwingFXUtils.fromFXImage(source, null), "png", output));
				out = new Image(input, w, h, true, smooth);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.err.println("(Ignoring.)");
				System.err.flush();
			}
			imageTime += (System.nanoTime() - start) / 1e9;
		}

		System.err.println(String.format("Average times: %.2f lanczos, %.2f image", lanczosTime / lanczosCount, imageTime / imageCount));

		return out;
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

	public static class Test extends Application {
		public static void main(String[] args) {
			Application.launch(args);
		}

		@Override
		public void start(Stage primaryStage) throws Exception {
			Image comparison = new Image("file:front.png");
			Image source = new Image("file:front.png");
			source = convolve(source, gaussian(1), 3);
			source = scaled(source, 220, 308, true);
			ImageView comparisonImage = new ImageView(comparison);
			ImageView image = new ImageView(source);
			Scene scene = new Scene(new HBox(comparisonImage, image));
			primaryStage.setScene(scene);
			primaryStage.show();
		}
	}
}

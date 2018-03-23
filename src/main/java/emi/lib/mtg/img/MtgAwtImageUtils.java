package emi.lib.mtg.img;

import emi.lib.mtg.Card;
import emi.lib.mtg.impl.BasicCard;
import emi.lib.mtg.impl.BasicPrinting;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.DoubleBinaryOperator;

public class MtgAwtImageUtils {
	public static final double ROUND_RADIUS_FRACTION = 3.0 / 63.0; // 3mm out of 63 x 88
	public static final double BORDER_WIDTH = 2.5 / 63.0;

	public static BufferedImage clearCorners(BufferedImage source, double forceRadius) {
		if (source.getColorModel().hasAlpha() && source.getAlphaRaster().getSampleDouble(0, 0, 0) <= 0.05) {
			return source;
		}

		BufferedImage dst = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		dst.createGraphics().drawImage(source, 0, 0, null);
		int[] dstBuffer = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();

		final double radius;

		if (forceRadius < 0) {
			// Heuristic to determine the border radius...
			double tmp = -1;
			int borderC = 0x00;
			for (int x = source.getWidth() / 64; x < source.getWidth(); ++x) {
				int rgb = source.getRGB(x, source.getHeight() / 4);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = (rgb) & 0xff;
				int chrominance = Math.max(Math.max(r, g), b) - Math.min(Math.min(r, g), b);

				if (borderC == 0x00 && Math.max(Math.max(r, g), b) >= 0x10) {
					borderC = chrominance;
				} else if (Math.abs(borderC - chrominance) >= 0x10) {
					tmp = x;
					break;
				}
			}

			radius = tmp >= 0 ? tmp : source.getWidth() * ROUND_RADIUS_FRACTION;
		} else {
			radius = forceRadius;
		}

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

					int alpha = (dstBuffer[dst.getWidth()*y + x] >> 24) & 0xff;
					alpha = (int) Math.max(0, Math.min(alpha * (1 - 2 * (dd - radius)), 0xff));
					dstBuffer[dst.getWidth()*y + x] = (dstBuffer[dst.getWidth()*y + x] & 0x00ffffff) | ((alpha << 24) & 0xff000000);
				}
			}
		}

		return dst;
	}

	public static BufferedImage clearCorners(BufferedImage source) {
		return clearCorners(source, source.getWidth() * ROUND_RADIUS_FRACTION);
	}

	private static ExecutorService daemonPool(String prefix) {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1, r -> {
			Thread th = Executors.defaultThreadFactory().newThread(r);
			th.setName(prefix + th.getId());
			th.setDaemon(true);
			return th;
		});
	}

	private static final ExecutorService IMAGE_OP_POOL = daemonPool("LibMtg-ImageOp-");

	public static DoubleBinaryOperator gaussian(double sigma) {
		return (x, y) -> {
			final double sigmasq2 = sigma*sigma*2;

			return 1/(Math.PI*sigmasq2)*Math.exp(-(x*x + y*y)/sigmasq2);
		};
	}

	public static DoubleBinaryOperator simplifiedGaussian(double sigma) {
		final double[] precomp = new double[(int) Math.ceil(6*sigma)];

		for (int i = 0; i < precomp.length; ++i) {
			precomp[i] = 1/Math.sqrt(2*Math.PI*sigma*sigma)*Math.exp(-(i*i)/(2*sigma*sigma));
		}

		return (x, y) -> {
			final int ix = (int) Math.round(Math.abs(x)), iy = (int) Math.round(Math.abs(y));

			if (ix >= precomp.length || iy >= precomp.length) {
				return 0.0;
			} else {
				return precomp[ix]*precomp[iy];
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

	// TODO: Rewrite all this to use ordinary byte arrays?

	public static BufferedImage convolve(BufferedImage source, DoubleBinaryOperator kernel, int a) {
		return resample(source, source.getWidth(), source.getHeight(), kernel, a);
	}

	public static BufferedImage resample(BufferedImage source, int w, int h, DoubleBinaryOperator kernel, int a) {
		final int sourceW = source.getWidth();
		final int sourceH = source.getHeight();

		int[] srcBuffer = source.getRaster().getPixels(0, 0, sourceW, sourceH, (int[]) null);
		int srcStride = source.getRaster().getNumBands();

		BufferedImage destination = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] destBuffer = ((DataBufferInt) destination.getRaster().getDataBuffer()).getData();

		try {
			IMAGE_OP_POOL.submit(() -> {
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final double sourceX = x * ((double) source.getWidth() / w);
						final double sourceY = y * ((double) source.getHeight() / h);

						final int windowX1 = Math.max(0, (int) Math.floor(sourceX) - a + 1);
						final int windowY1 = Math.max(0, (int) Math.floor(sourceY) - a + 1);

						final int windowX2 = Math.min((int) Math.floor(sourceX) + a - 1, sourceW - 1);
						final int windowY2 = Math.min((int) Math.floor(sourceY) + a - 1, sourceH - 1);

						double accumR = 0, accumG = 0, accumB = 0, accumA = 0;
						double accumF = 0.0;
						for (int ay = windowY1; ay <= windowY2; ++ay) {
							for (int ax = windowX1; ax <= windowX2; ++ax) {
								final int xyi = (ay*sourceW + ax)*srcStride;

								int alpha = 0xFF;
								if (srcStride >= 4) {
									alpha = srcBuffer[xyi + 3] & 0xFF;
								}
								int r = srcBuffer[xyi + 2] & 0xFF;
								int g = srcBuffer[xyi + 1] & 0xFF;
								int b = srcBuffer[xyi] & 0xFF;

								double f = kernel.applyAsDouble(sourceX - ax, sourceY - ay);

								accumF += f;
								if (srcStride >= 4) {
									accumA += alpha * f;
								} else {
									accumA += 255.0;
								}
								accumR += r * f;
								accumG += g * f;
								accumB += b * f;
							}
						}

						byte alpha = (byte) 0xFF;
						if (srcStride >= 4) {
							alpha = (byte) Math.max(0, Math.min((int) (accumA / accumF), 0xFF));
						}
						byte red = (byte) Math.max(0, Math.min((int) (accumR / accumF), 0xFF));
						byte green = (byte) Math.max(0, Math.min((int) (accumG / accumF), 0xFF));
						byte blue = (byte) Math.max(0, Math.min((int) (accumB / accumF), 0xFF));

						int packed = Byte.toUnsignedInt(alpha) << 24 |
								Byte.toUnsignedInt(red) |
								Byte.toUnsignedInt(green) << 8 |
								Byte.toUnsignedInt(blue) << 16;

						destBuffer[y * w + x] = packed;
					}
				}
			}).get();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			throw new Error(ie);
		} catch (ExecutionException ee) {
			ee.getCause().printStackTrace();
			throw new Error(ee.getCause());
		}

		return destination;
	}

	public static BufferedImage scaled(BufferedImage source, double w, double h, boolean smooth) {
		BufferedImage out = source;
		if (smooth) {
			out = convolve(out, simplifiedGaussian(1.0), 3);
		}
		out = resample(out, (int) w, (int) h, lanczos(3), 3);

		return out;
	}

	public static BufferedImage subsection(BufferedImage source, int x1, int y1, int x2, int y2) {
		BufferedImage output = new BufferedImage(x2 - x1, y2 - y1, source.getType());
		output.createGraphics().drawImage(source.getSubimage(x1, y1, x2 - x1, y2 - y1), 0, 0, null);
		return output;
	}

	public static BufferedImage rotated(BufferedImage source, double rotation) {
		double radians = rotation * Math.PI / 180.0;
		int w = (int) (source.getWidth() * Math.abs(Math.cos(radians)) + source.getHeight() * Math.abs(Math.sin(radians)));
		int h = (int) (source.getWidth() * Math.abs(Math.sin(radians)) + source.getHeight() * Math.abs(Math.cos(radians)));

		AffineTransform rotate = AffineTransform.getRotateInstance(-radians, source.getWidth() / 2, source.getHeight() / 2);
		BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		dest.createGraphics().drawImage(source, new AffineTransformOp(rotate, AffineTransformOp.TYPE_BICUBIC), (w - source.getWidth()) / 2, (h - source.getHeight()) / 2);
		return dest;
	}

	public static BufferedImage faceFromFull(Card.Printing.Face printedFace, BufferedImage source) {
		final double borderRadius = source.getWidth() * ROUND_RADIUS_FRACTION;

		Card card = printedFace.printing().card();

		if (card.face(Card.Face.Kind.Left) != null) {
			// split card
			if (card.face(Card.Face.Kind.Right) == null) {
				throw new IllegalStateException();
			}

			final int startY, endY;
			final double rotation;

			if (card.face(Card.Face.Kind.Right).rules().startsWith("Aftermath")) {
				int division = (int) (source.getHeight() * 0.5425);
				switch (printedFace.face().kind()) {
					case Left:
						startY = 0;
						endY = division;
						rotation = 0.0;
						break;
					case Right:
						startY = division;
						endY = source.getHeight();
						rotation = 90.0;
						break;
					default:
						throw new IllegalStateException();
				}
			} else {
				int division = (int) (source.getHeight() * 0.5);
				switch (printedFace.face().kind()) {
					case Left:
						startY = division;
						endY = source.getHeight();
						break;
					case Right:
						startY = 0;
						endY = division;
						break;
					default:
						throw new IllegalStateException();
				}
				rotation = -90.0;
			}

			return rotated(clearCorners(subsection(source, 0, startY, source.getWidth(), endY), borderRadius), rotation);
		} else if (card.face(Card.Face.Kind.Flipped) != null) {
			// Kamigawa flip card - probably.
			int divisionTop, divisionBottom;

			if (card.face(Card.Face.Kind.Front).name().equals("Curse of the Fire Penguin")) {
				divisionTop = (int) (source.getHeight() * 0.675);
				divisionBottom = divisionTop;
			} else {
				divisionTop = (int) (source.getHeight() * 0.307);
				divisionBottom = (int) (source.getHeight() * 0.667);
			}

			switch (printedFace.face().kind()) {
				case Front:
					return clearCorners(subsection(source, 0, 0, source.getWidth(), divisionBottom), borderRadius);
				case Flipped:
					return rotated(clearCorners(subsection(source, 0, divisionTop, source.getWidth(), source.getHeight()), borderRadius), 180.0);
				default:
					throw new IllegalStateException();
			}
		} else {
			return clearCorners(source);
		}
	}

	public static class Test extends Application {
		static BufferedImage source;
		static BufferedImage example;

		public static void main(String[] args) throws IOException {
			BasicCard card = new BasicCard()
					.face(new BasicCard.Face(Card.Face.Kind.Front).name("Curse of the Fire Penguin").type("Sorcery").rules(""))
					.face(new BasicCard.Face(Card.Face.Kind.Flipped).type("Sorcery").rules("Aftermath"));

			BasicPrinting printing = new BasicPrinting();
			printing.card(card)
					.face(printing.new Face(card.face(Card.Face.Kind.Front)))
					.face(printing.new Face(card.face(Card.Face.Kind.Flipped)));

			card.printing(printing);

			source = ImageIO.read(new File("fire-penguin.png"));
			source = scaled(source, source.getWidth() * 0.75, source.getHeight() * 0.75, true);
			example = faceFromFull(printing.face(Card.Face.Kind.Front), source);

			Application.launch(args);
		}

		@Override
		public void start(Stage primaryStage) throws Exception {
			WritableImage fxComparison = SwingFXUtils.toFXImage(source, null);
			WritableImage fxExample = SwingFXUtils.toFXImage(example, null);

			ImageView comparisonImage = new ImageView(fxComparison);
			BorderPane comparisonBorder = new BorderPane(comparisonImage);
			comparisonBorder.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT)));

			ImageView image = new ImageView(fxExample);
			BorderPane border = new BorderPane(image);
			border.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT)));

			Scene scene = new Scene(new HBox(16, new Group(comparisonBorder), new Group(border)));
			scene.setFill(javafx.scene.paint.Color.MAGENTA);
			primaryStage.setScene(scene);
			primaryStage.show();
		}
	}

}

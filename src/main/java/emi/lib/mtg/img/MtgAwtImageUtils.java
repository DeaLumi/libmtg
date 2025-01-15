package emi.lib.mtg.img;

import emi.lib.mtg.Card;
import emi.lib.mtg.enums.StandardFrame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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

	public static BufferedImage combined(BufferedImage source1, int x11, int y11, int x12, int y12, BufferedImage source2, int x21, int y21, int x22, int y22) {
		BufferedImage output = new BufferedImage(Math.max(x12, x22) - Math.min(x11, x21), Math.max(y12, y22) - Math.min(y11, y21), source1.getType());

		Graphics2D graphics = output.createGraphics();
		graphics.drawImage(source1, x11, y11, x12 - x11, y12 - y11, null);
		graphics.drawImage(source2, x21, y21, x22 - x21, y22 - y21, null);
		return output;
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

	private static double rgbValue(int pixel) {
		return ((pixel & 0xFF) + ((pixel & 0xFF00) >> 8)  + ((pixel & 0xFF0000) >> 16)) / 3.0;
	}

	// TODO: This is a naive implementation.
	private static double stdDev(int[] rgb, int start, int count, int stride) {
		double sum = 0, sumOfSquares = 0;

		for (int pixel = start; pixel < start + count; pixel += stride) {
			double v = rgbValue(rgb[pixel]);
			sum += v;
			sumOfSquares += v * v;
		}

		return Math.sqrt(count * sumOfSquares - sum * sum) / count;
	}

	private static final double BORDER_DEVIATION = 0.020 * 0xFF; // Split dividers (border colored) are extremely even in color, at least if they have black borders.
	private static final double FRAME_DEVIATION = 0.050 * 0xFF; // Card frames are a bit rougher,

	private static int findHLine(BufferedImage source, int left, int yStart, int right, int yStop, double maxDeviation) {
		final int width = Math.abs(right - left);
		final int x0 = Math.min(left, right);
		final int height = Math.abs(yStop - yStart);
		final int[] rgb = new int[right * height];
		source.getRGB(x0, Math.min(yStart, yStop), width, height, rgb, 0, width);

		if (yStart < yStop) {
			for (int y = yStart; y < yStop; ++y) {
				if (stdDev(rgb, width * (y - yStart), width, 1) < maxDeviation) {
					return y;
				}
			}
		} else if (yStart > yStop) {
			for (int y = yStart - 1; y >= yStop; --y) {
				if (stdDev(rgb, width * (y - yStop), width, 1) < maxDeviation) {
					return y;
				}
			}
		}

		return -1;
	}

	private static int findHDivider(BufferedImage source, int xStart, int yStart, int xStop, int yStop, double sigma) {
		final int x0 = Math.min(xStart, xStop);
		final int x1 = Math.max(xStart, xStop);
		final int width = x1 - x0;
		final int y0 = Math.min(yStart, yStop);
		final int y1 = Math.max(yStart, yStop);
		final int height = y1 - y0;

		final int[] rgb = new int[width * height];
		source.getRGB(x0, y0, width, height, rgb, 0, width);

		int top = -1;
		for (int y = y0; y < y1; ++y) {
			double dev = stdDev(rgb, width * (y - y0), width, 1);
			if (dev < sigma) {
				top = y;
				break;
			}
		}

		if (top < 0) return -1;

		int bottom = -1;
		for (int y = y1 - 1; y >= y0; --y) {
			double dev = stdDev(rgb, width * (y - y0), width, 1);
			if (dev < sigma) {
				bottom = y;
				break;
			}
		}

		if (bottom < 0) return top;

		return (top + bottom) / 2;
	}

	public static BufferedImage faceFromFull(Card.Print.Face printedFace, BufferedImage full) {
		BufferedImage tmp = full;

		Card.Print.Face.Frame frame = printedFace.frame();
		if (frame.left() > 0 || frame.right() > 0 || frame.top() > 0 || frame.bottom() > 0)
			tmp = subsection(tmp, (int) (frame.left() * full.getWidth()), (int) (frame.top() * full.getHeight()),
					(int) ((1.0 - frame.right()) * full.getWidth()), (int) ((1.0 - frame.bottom()) * full.getHeight()));

		if (frame.rotation() != 0)
			tmp = rotated(tmp, -90.0 * frame.rotation());

		return clearCorners(tmp);
	}

	public static BufferedImage meldedFace(Card.Print.Face printedFace, BufferedImage top, BufferedImage bottom) {
		return clearCorners(rotated(combined(bottom, 0, 0, bottom.getWidth(), bottom.getHeight(),
				top, bottom.getWidth(), 0, bottom.getWidth() + top.getWidth(), top.getHeight()), -90.0 * StandardFrame.Meld.rotation()));
	}
}

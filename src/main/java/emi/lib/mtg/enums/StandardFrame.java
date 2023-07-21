package emi.lib.mtg.enums;

import emi.lib.mtg.Card;

import java.util.Comparator;

public enum StandardFrame implements Card.Printing.Face.Frame {
	FullFace(0.0, 0.0, 0.0, 0.0, 0),

	/* Kamigawa-style flip cards */
	FlipTopFull(0.0, 0.0, 0.0, 0.3397, 0),
	FlipBottomFull(0.0, 0.0, 0.3120, 0.0, 2),
	FlipTopModern(0.0, 0.0, 0.0, 0.3750, 0),
	FlipBottomModern(0.0, 0.0, 0.2959, 0.0, 2),

	/* Split and/or fuse cards */
	SplitLeftFull(0.0, 0.0, 0.5, 0.0, 1),
	SplitRightFull(0.0, 0.0, 0.0, 0.5, 1),
	SplitLeftModern(0.0, 0.0, 0.4786, 0.0, 1),
	SplitRightModern(0.0, 0.0, 0.0, 0.5214, 1),

	/* Aftermath */
	AftermathTop(0.0, 0.0, 0.0, 0.4584, 0),
	AftermathBottom(0.0, 0.0, 0.5416, 0.0, -1),

	Adventure(0.0, 0.4985, 0.6239, 0.0, 0),
	Battle(0.0, 0.0, 0.0, 0.0, 1),
	Meld(0.0, -1.0, 0.0, 0.0, -1),

	/* Silly Stuff */
	FirePenguinTop(0.0, 0.0, 0.0, 0.3269, 0),
	FirePenguinBottom(0.0, 0.0, 0.6699, 0.0, 2),
	/* No, I'm still not doing who/what/when/where/why. */
	;

	private final double left, right, top, bottom;
	private final int rotation;

	StandardFrame(double left, double right, double top, double bottom, int rotation) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		this.rotation = rotation;
	}

	@Override
	public double left() {
		return left;
	}

	@Override
	public double right() {
		return right;
	}

	@Override
	public double top() {
		return top;
	}

	@Override
	public double bottom() {
		return bottom;
	}

	@Override
	public int rotation() {
		return rotation;
	}

	/**
	 * Attempts to establish a sensible ordering on frames. In essence:
	 * - If the frames have different rotations, the rotation closer to 0 comes first (aftermath, flip cards).
	 * - Else, the frame closer to the top of the reoriented card comes first (adventures).
	 * - Else, the frame closer to the left of the reoriented card comes first (ordinary split cards).
	 * - Else, the vertically-shorter frame on the reoriented card comes first (???).
	 * - Else, the horizontally-skinnier frame on the reoriented card comes first (???).
	 * - Else, the frames are identical.
	 */
	public static Comparator<Card.Printing.Face.Frame> FRAME_SORT = (a, b) -> {
		if (a.rotation() != b.rotation()) return Math.abs(a.rotation()) - Math.abs(b.rotation());
		double l1, t1, r1, b1, l2, t2, r2, b2;

		switch (a.rotation()) {
			case -1:
				l1 = a.top(); t1 = a.right(); r1 = a.bottom(); b1 = a.left();
				l2 = b.top(); t2 = b.right(); r2 = b.bottom(); b2 = a.left();
				break;
			case 0:
				l1 = a.left(); t1 = a.top(); r1 = a.right(); b1 = a.bottom();
				l2 = b.left(); t2 = b.top(); r2 = b.right(); b2 = a.bottom();
				break;
			case 1:
				l1 = a.bottom(); t1 = a.left(); r1 = a.top(); b1 = a.right();
				l2 = b.bottom(); t2 = b.left(); r2 = b.top(); b2 = a.right();
				break;
			case -2:
			case 2:
				l1 = a.right(); t1 = a.bottom(); r1 = a.left(); b1 = a.top();
				l2 = b.right(); t2 = b.bottom(); r2 = b.left(); b2 = a.top();
				break;
			default:
				throw new IllegalArgumentException("What rotation is " + a.rotation() + "???");
		}

		if (t1 != t2) return (int) Math.signum(t1 - t2);
		if (l1 != l2) return (int) Math.signum(l1 - l2);
		if (b1 != b2) return (int) Math.signum(b2 - b1);
		if (r1 != r2) return (int) Math.signum(r2 - r1);
		return 0;
	};
}

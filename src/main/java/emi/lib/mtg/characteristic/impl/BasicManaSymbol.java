package emi.lib.mtg.characteristic.impl;

import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.ManaSymbol;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Emi on 5/6/2016.
 */
public enum BasicManaSymbol implements ManaSymbol {
	WHITE ("{W}", 1, Color.WHITE),
	BLUE ("{U}", 1, Color.BLUE),
	BLACK ("{B}", 1, Color.BLACK),
	RED ("{R}", 1, Color.RED),
	GREEN ("{G}", 1, Color.GREEN),

	HYBRID_WHITE_BLUE ("{W/U}", 1, Color.WHITE, Color.BLUE),
	HYBRID_WHITE_BLACK ("{W/B}", 1, Color.WHITE, Color.BLACK),
	HYBRID_BLUE_BLACK ("{U/B}", 1, Color.BLUE, Color.BLACK),
	HYBRID_BLUE_RED ("{U/R}", 1, Color.BLUE, Color.RED),
	HYBRID_BLACK_RED ("{B/R}", 1, Color.BLACK, Color.RED),
	HYBRID_BLACK_GREEN ("{B/G}", 1, Color.BLACK, Color.GREEN),
	HYBRID_RED_GREEN ("{R/G}", 1, Color.RED, Color.GREEN),
	HYBRID_RED_WHITE ("{R/W}", 1, Color.RED, Color.WHITE),
	HYBRID_GREEN_WHITE ("{G/W}", 1, Color.GREEN, Color.WHITE),
	HYBRID_GREEN_BLUE ("{G/U}", 1, Color.GREEN, Color.BLUE),

	TWOBRID_WHITE_2 ("{2/W}", 2, Color.WHITE),
	TWOBRID_BLUE_2 ("{2/U}", 2, Color.BLUE),
	TWOBRID_BLACK_2 ("{2/B}", 2, Color.BLACK),
	TWOBRID_RED_2 ("{2/R}", 2, Color.RED),
	TWOBRID_GREEN_2 ("{2/G}", 2, Color.GREEN),

	PHYREXIAN_WHITE ("{W/P}", 1, Color.WHITE),
	PHYREXIAN_BLUE ("{U/P}", 1, Color.BLUE),
	PHYREXIAN_BLACK ("{B/P}", 1, Color.BLACK),
	PHYREXIAN_RED ("{R/P}", 1, Color.RED),
	PHYREXIAN_GREEN ("{G/P}", 1, Color.GREEN),
	PHYREXIAN_GENERIC ("{P}", 1),

	SNOW ("{S}", 1),
	COLORLESS ("{C}", 1, Color.COLORLESS),

	GENERIC_0 ("{0}", 0),
	GENERIC_1 ("{1}", 1),
	GENERIC_2 ("{2}", 2),
	GENERIC_3 ("{3}", 3),
	GENERIC_4 ("{4}", 4),
	GENERIC_5 ("{5}", 5),
	GENERIC_6 ("{6}", 6),
	GENERIC_7 ("{7}", 7),
	GENERIC_8 ("{8}", 8),
	GENERIC_9 ("{9}", 9),
	GENERIC_10 ("{10}", 10),
	GENERIC_11 ("{11}", 11),
	GENERIC_12 ("{12}", 12),
	GENERIC_13 ("{13}", 13),
	GENERIC_15 ("{15}", 15),
	GENERIC_16 ("{16}", 16),
	GENERIC_20 ("{20}", 20),
	GENERIC_MILLION ("{1000000}", 1000000),
	GENERIC_INFINITE ("{\u221e}", Integer.MAX_VALUE),

	GENERIC_X ("{X}", true),
	GENERIC_Y ("{Y}", true),
	GENERIC_Z ("{Z}", true),

	TAP ("{T}", 0),
	UNTAP ("{Q}", 0)
	;

	public static final Pattern SYMBOL = Pattern.compile("\\{(?:[WUBRG]|W/[UB]|U/[BR]|B/[RG]|R/[GW]|G/[WU]|2/[WUBRG]|(?:[WUBRG]/)?P|S|C|[0-9]+|\u221e|[XYZ]|[TQ])\\}");

	public static Collection<BasicManaSymbol> parse(String manaCost) {
		Matcher m = SYMBOL.matcher(manaCost);

		List<BasicManaSymbol> list = new ArrayList<>();

		int lastEnd = 0;
		while (m.find()) {
			list.add(BasicManaSymbol.fromString(m.group()));
			lastEnd = m.end();
		}

		if (lastEnd != manaCost.length()) {
			throw new IllegalArgumentException(String.format("%s is not a valid mana cost (%d != %d)...", manaCost, lastEnd, manaCost.length()));
		}

		return list;
	}

	public static BasicManaSymbol fromString(String unparsing) {
		if (!reverse.containsKey(unparsing)) {
			for (BasicManaSymbol symbol : BasicManaSymbol.values()) {
				if (symbol.toString().equals(unparsing)) {
					reverse.put(symbol.toString(), symbol);
					return symbol;
				}
			}

			throw new IllegalArgumentException(String.format("%s is not a valid mana symbol...", unparsing));
		}

		return reverse.get(unparsing);
	}

	private final static Map<String, BasicManaSymbol> reverse = new HashMap<>();

	private final String unparsing;
	private final Set<Color> colors;
	private final int convertedCost;
	private final boolean varies;

	BasicManaSymbol(String unparsing, int convertedCost) {
		this.unparsing = unparsing;
		this.convertedCost = convertedCost;
		this.varies = false;
		this.colors = Collections.unmodifiableSet(EnumSet.noneOf(Color.class));
	}

	BasicManaSymbol(String unparsing, boolean varies) {
		this.unparsing = unparsing;
		this.convertedCost = 0;
		this.varies = varies;
		this.colors = Collections.unmodifiableSet(EnumSet.noneOf(Color.class));
	}

	BasicManaSymbol(String unparsing, int convertedCost, Color firstColor, Color... otherColors) {
		this.unparsing = unparsing;
		this.convertedCost = convertedCost;
		this.varies = false;
		this.colors = Collections.unmodifiableSet(EnumSet.of(firstColor, otherColors));
	}

	BasicManaSymbol(String unparsing, boolean varies, Color firstColor, Color... otherColors) {
		this.unparsing = unparsing;
		this.convertedCost = 0;
		this.varies = varies;
		this.colors = Collections.unmodifiableSet(EnumSet.of(firstColor, otherColors));
	}

	@Override
	public String toString() {
		return unparsing;
	}

	@Override
	public int convertedCost() {
		return convertedCost;
	}

	@Override
	public boolean varies() {
		return varies;
	}

	@Override
	public Set<Color> colors() {
		return colors;
	}
}

package emi.lib.mtg;

import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.util.Multiset;

import java.util.*;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.*;

public interface Mana {
	double value();
	Color.Combination color();

	interface Symbol extends Mana {
		static Symbol parse(String str) {
			if (str.isEmpty()) throw new IllegalArgumentException("Mana symbol can't be empty!");

			if (str.charAt(0) == '{') {
				if (str.charAt(str.length() - 1) != '}') throw new IllegalArgumentException("Malformed mana symbol " + str);
				str = str.substring(1, str.length() - 1);
			}

			if (str.length() > 2 && str.contains("/")) {
				return Symbol.Hybrid.of(Arrays.stream(str.split("/")).map(Symbol::parsePure).collect(Collectors.toList()));
			} else {
				return Symbol.parsePure(str);
			}
		}

		static Symbol.Pure parsePure(String str) {
			try {
				return Atom.forString(str);
			} catch (IllegalArgumentException nsme) {
				// do nothing
			}

			try {
				return Variable.valueOf(str);
			} catch (IllegalArgumentException nsme) {
				// do nothing
			}

			try {
				return Generic.parse(str);
			} catch (NumberFormatException nfe) {
				// do nothing
			}

			throw new IllegalArgumentException("Unrecognized mana symbol " + str);
		}

		static Stream<Symbol> symbolsIn(String str) {
			if (str == null || str.isEmpty()) return Stream.empty();

			Iterator<Symbol> symbolIterator = new Iterator<Symbol>() {
				private Symbol next = null;
				private int nextStart = 0;

				private void findNext() {
					for (; nextStart >= 0 && nextStart < str.length(); ++nextStart) {
						switch (str.charAt(nextStart)) {
							case '(':
								nextStart = str.indexOf(')', nextStart);
								break;
							case '{':
								int end = str.indexOf('}', nextStart);
								int start = nextStart + 1;
								nextStart = end + 1;
								try {
									next = Symbol.parse(str.substring(start, end));
									return;
								} catch (IllegalArgumentException iae) {
									continue; // There's a lot of non-mana symbols in Magic. Be tolerant.
								}
						}
					}

					if (nextStart < 0) nextStart = str.length();
					next = null;
				}

				@Override
				public boolean hasNext() {
					if (next == null) findNext();
					return next != null;
				}

				@Override
				public Symbol next() {
					if (next == null) findNext();

					Symbol tmp = next;
					findNext();
					return tmp;
				}
			};

			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(symbolIterator, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
		}

		interface Pure extends Symbol {
		}

		enum Atom implements Pure {
			Colorless("C", 1.0, Color.Combination.Colorless),
			White("W", 1.0, Color.Combination.White),
			Blue("U", 1.0, Color.Combination.Blue),
			Black("B", 1.0, Color.Combination.Black),
			Red("R", 1.0, Color.Combination.Red),
			Green("G", 1.0, Color.Combination.Green),

			HalfColorless("HC", Atom.Colorless),
			HalfWhite("HW", Atom.White),
			HalfBlue("HU", Atom.Blue),
			HalfBlack("HB", Atom.Black),
			HalfRed("HR", Atom.Red),
			HalfGreen("HG", Atom.Green),

			Snow("S", 1.0, Color.Combination.Empty),
			Phyrexian("P", 0.0, Color.Combination.Empty),
			;

			private final String symbol;
			private final double value;
			private final Color.Combination color;
			private final Atom whole;
			private Atom half;

			static {
				for (Atom atom : Atom.values()) if (atom.whole != null) atom.whole.half = atom;
			}

			Atom(String symbol, double value, Color.Combination color) {
				this.symbol = symbol;
				this.value = value;
				this.color = color;
				this.whole = null;
			}

			Atom(String symbol, Atom whole) {
				this.symbol = symbol;
				this.value = whole.value / 2.0;
				this.color = whole.color;
				this.whole = whole;
			}

			@Override
			public double value() {
				return value;
			}

			@Override
			public Color.Combination color() {
				return color;
			}

			@Override
			public String toString() {
				return symbol;
			}

			private static final Map<String, Atom> bySymbolMap = bySymbolMap();

			private static Map<String, Atom> bySymbolMap() {
				Map<String, Atom> tmp = new HashMap<>();

				for (Atom atom : Atom.values()) {
					tmp.put(atom.symbol, atom);
				}

				return Collections.unmodifiableMap(tmp);
			}

			public static Atom forString(String str) {
				Atom atom = bySymbolMap.get(str);
				if (atom == null) throw new IllegalArgumentException("No such atom " + str);
				return atom;
			}
		}

		class Generic implements Pure {
			public static final Generic ZERO = new Generic(0.0),
					HALF = new Generic(0.5),
					ONE = new Generic(1.0),
					TWO = new Generic(2.0),
					INFINITY = new Generic(Double.POSITIVE_INFINITY);

			public static Generic of(double value) {
				if (value < 0) throw new IllegalArgumentException("Generic values can't be negative!");
				if (value == 0.0) return ZERO;
				if (value == 0.5) return HALF;
				if (value == 1.0) return ONE;
				if (value == 2.0) return TWO;
				if (value == Double.POSITIVE_INFINITY) return INFINITY;

				return new Generic(value);
			}

			public static Generic parse(String str) {
				if ("\u221e".equals(str)) return INFINITY;

				double val = 0.0;
				if (str.endsWith("\u00bd")) {
					str = str.substring(0, str.length() - 1);
					val += 0.5;
				}

				val += Double.parseDouble(str);

				return of(val);
			}

			private final double value;

			protected Generic(double value) {
				this.value = value;
			}

			@Override
			public int hashCode() {
				return Double.hashCode(value);
			}

			@Override
			public boolean equals(Object obj) {
				return obj instanceof Generic && Double.compare(((Generic) obj).value, value) == 0;
			}

			@Override
			public String toString() {
				if (value == 0.5) return "\u00bd";
				if (value == Double.POSITIVE_INFINITY) return "\u221e";
				double rem = value % 1.0;
				if (rem == 0.5) return ((int) value) + "\u00bd";
				if (rem == 0.0) return Integer.toString((int) value);
				return Double.toString(value);
			}

			@Override
			public double value() {
				return value;
			}

			@Override
			public Color.Combination color() {
				return Color.Combination.Empty;
			}

		}

		enum Variable implements Pure {
			X,
			Y,
			Z;

			@Override
			public String toString() {
				return name();
			}

			@Override
			public double value() {
				return 0;
			}

			@Override
			public Color.Combination color() {
				return Color.Combination.Empty;
			}
		}

		class Hybrid implements Symbol {
			public static final Hybrid
					WHITE_BLUE = new Hybrid(Atom.White, Atom.Blue),
					WHITE_BLACK = new Hybrid(Atom.White, Atom.Black),
					TWO_WHITE = new Hybrid(Generic.TWO, Atom.White),
					WHITE_PHYREXIAN = new Hybrid(Atom.White, Atom.Phyrexian),

					BLUE_BLACK = new Hybrid(Atom.Blue, Atom.Black),
					BLUE_RED = new Hybrid(Atom.Blue, Atom.Red),
					TWO_BLUE = new Hybrid(Generic.TWO, Atom.Blue),
					BLUE_PHYREXIAN = new Hybrid(Atom.Blue, Atom.Phyrexian),

					BLACK_RED = new Hybrid(Atom.Black, Atom.Red),
					BLACK_GREEN = new Hybrid(Atom.Black, Atom.Green),
					TWO_BLACK = new Hybrid(Generic.TWO, Atom.Black),
					BLACK_PHYREXIAN = new Hybrid(Atom.Black, Atom.Phyrexian),

					RED_GREEN = new Hybrid(Atom.Red, Atom.Green),
					RED_WHITE = new Hybrid(Atom.Red, Atom.White),
					TWO_RED = new Hybrid(Generic.TWO, Atom.Red),
					RED_PHYREXIAN = new Hybrid(Atom.Red, Atom.Phyrexian),

					GREEN_WHITE = new Hybrid(Atom.Green, Atom.White),
					GREEN_BLUE = new Hybrid(Atom.Green, Atom.Blue),
					TWO_GREEN = new Hybrid(Generic.TWO, Atom.Green),
					GREEN_PHYREXIAN = new Hybrid(Atom.Green, Atom.Phyrexian);

			public static Hybrid of(Symbol.Pure... options) {
				return new Hybrid(Arrays.asList(options));
			}

			public static Hybrid of(Collection<? extends Symbol.Pure> options) {
				return new Hybrid(options);
			}

			private final SortedSet<Symbol.Pure> options;
			private final Color.Combination color;

			protected Hybrid(Symbol.Pure a, Symbol.Pure b) {
				this(Arrays.asList(a, b));
			}

			protected Hybrid(Collection<? extends Symbol.Pure> options) {
				this.color = options.stream().map(Symbol::color).collect(Color.Combination.COMBO_COLLECTOR);
				TreeSet<Symbol.Pure> tmp = new TreeSet<>(Comparator.comparing(Symbol::color, this.color.symbolOrder())
						.thenComparing(Symbol::value)
						.thenComparing((s1, s2) -> s1.equals(s2) ? 0 : 1));
				tmp.addAll(options);
				this.options = Collections.unmodifiableSortedSet(tmp);
			}

			public java.util.Set<Pure> options() {
				return options;
			}

			@Override
			public int hashCode() {
				return options.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				return obj instanceof Hybrid && ((Hybrid) obj).options.equals(options);
			}

			@Override
			public String toString() {
				return options.stream().map(Symbol::toString).collect(Collectors.joining("/"));
			}

			@Override
			public double value() {
				return options.stream().mapToDouble(Symbol::value).max().orElse(0.0);
			}

			@Override
			public Color.Combination color() {
				return color;
			}

			protected Color.Combination firstColor() {
				return options.stream().map(Symbol::color).min(Color.Combination.EMPTY_LAST_COMPARATOR).orElse(Color.Combination.Empty);
			}
		}
	}

	class Value implements Mana {
		public static class Pure extends Value {
			public static class Family implements Mana {
				public static class NoClosedFormException extends Exception {
					public NoClosedFormException(String message) {
						super(message);
					}
				}

				protected final java.util.Set<Pure> options;

				protected Family(Collection<? extends Pure> options) {
					Color.Combination color = options.stream().map(Pure::color).collect(Color.Combination.COMBO_COLLECTOR);
					this.options = new TreeSet<>(Value.COMPLETE_COMPARATOR
							.thenComparing((v1, v2) -> v1.equals(v2) ? 0 : 1));
					this.options.addAll(options);
				}

				public Set<Pure> options() {
					return options;
				}

				public Value factor() throws NoClosedFormException {
					throw new UnsupportedOperationException("Nope, haven't implemented this either.");
				}

				@Override
				public double value() {
					return options.stream().mapToDouble(Value::value).max().orElse(0.0);
				}

				@Override
				public Color.Combination color() {
					return options.stream().map(Value::color).collect(Color.Combination.COMBO_COLLECTOR);
				}

				@Override
				public int hashCode() {
					return options.hashCode();
				}

				@Override
				public boolean equals(Object obj) {
					return (obj instanceof Family) && ((Family) obj).options.equals(options);
				}

				@Override
				public String toString() {
					return options.toString();
				}
			}

			public static Comparator<Pure> comparator(Comparator<Color.Combination> colorComparator) {
				return null; // TODO
			}

			protected <T extends Symbol.Pure> Pure(Collection<T> symbols) {
				super(symbols);
			}

			protected <T extends Symbol.Pure> Pure(Collection<? extends T> symbolsA, Collection<? extends T> symbolsB) {
				super(symbolsA, symbolsB);
			}

			protected Multiset<Symbol.Pure> pureSymbols() {
				return (Multiset<Symbol.Pure>) symbols;
			}

			@Override
			public Collection<Symbol.Pure> symbols() {
				return Collections.unmodifiableCollection(pureSymbols());
			}

			@Override
			public boolean pure() {
				return true;
			}

			@Override
			public Pure copy() {
				return new Pure(symbols());
			}

			@Override
			public Value add(Value other) {
				if (other instanceof Pure) return add((Pure) other);
				return new Value(this.symbols, other.symbols); // We can't guarantee other contains only pure symbols.
			}

			public Pure add(Pure other) {
				this.pureSymbols().addAll(other.pureSymbols());
				polishSymbols();
				return this;
			}

			public Pure reduce(Pure other, boolean colorPaysGeneric, boolean failOnExcess) {
				double ourGeneric = 0, theirGeneric = 0;
				Multiset<Symbol.Pure>.UniqueIterator iter;

				// Step one: Sum up our generic symbols.
				for (iter = this.pureSymbols().uniqueIterator(); iter.hasNext(); ) {
					Symbol.Pure sym = iter.next();
					if (sym instanceof Symbol.Generic) {
						ourGeneric += sym.value() * iter.count();
					}
				}

				// Step two: Sum up the reduction's generic value, substituting generic for color as appropriate.
				// If we fail on excess, bail out before modifying our symbols if we can't support the reduction.
				// TODO: Handle half-symbols. Ugh.
				for (iter = other.pureSymbols().uniqueIterator(); iter.hasNext(); ) {
					Symbol.Pure sym = iter.next();
					if (sym instanceof Symbol.Generic) {
						theirGeneric += sym.value() * iter.count();
					} else {
						int diff = iter.count() - this.symbols.count(sym);
						if (diff > 0) {
							if (colorPaysGeneric) {
								theirGeneric += sym.value() * diff;
							} else if (failOnExcess) {
								return null;
							}
						}
					}

					if (failOnExcess && theirGeneric > ourGeneric) {
						return null;
					}
				}

				// Step three: Remove any common symbols. Any overflow is already stored in ourGeneric - theirGeneric.
				for (iter = other.pureSymbols().uniqueIterator(); iter.hasNext(); ) {
					Symbol.Pure sym = iter.next();
					if (sym instanceof Symbol.Generic) continue;
					this.symbols.remove(sym, iter.count());
				}

				// Step four: If ourGeneric is not zero, remove all generic symbols and add a single symbol with value ourGeneric - theirGeneric.
				if (ourGeneric > 0) {
					for (iter = this.pureSymbols().uniqueIterator(); iter.hasNext(); ) {
						Symbol.Pure sym = iter.next();
						if (sym instanceof Symbol.Generic) {
							iter.remove();
						}
					}

					this.pureSymbols().add(Symbol.Generic.of(ourGeneric - theirGeneric));
					polishSymbols();
				}

				// Step five: If our symbols is now empty, we become {0}. (Nonexistent mana costs can't be paid. Reduction always leaves a residual.)
				if (this.symbols.isEmpty()) {
					this.pureSymbols().add(Symbol.Generic.ZERO);
				}

				return this;
			}
		}

		public static Value of(Symbol... symbols) {
			return new Value(Arrays.asList(symbols));
		}

		public static Value.Pure of(Symbol.Pure... symbols) {
			return new Value.Pure(Arrays.asList(symbols));
		}

		public static Value of(Collection<? extends Symbol> symbols) {
			if (symbols.stream().allMatch(s -> s instanceof Symbol.Pure)) {
				// Cast is safe due to above check.
				return new Value.Pure((Collection<? extends Symbol.Pure>) symbols);
			} else {
				return new Value(symbols);
			}
		}

		public static Value parse(String str) {
			int start = -1;
			List<Symbol> symbols = new ArrayList<>();
			for (int i = 0; i < str.length(); ++i) {
				if (start < 0) {
					if (str.charAt(i) != '{') throw new IllegalArgumentException("Malformed mana cost " + str + " at character " + i);
					start = ++i;
				} else {
					if (str.charAt(i) == '}') {
						symbols.add(Symbol.parse(str.substring(start, i)));
						start = -1;
					}
				}
			}

			return Value.of(symbols);
		}

		protected final Multiset<? extends Symbol> symbols;

		protected <T extends Symbol> Value(Collection<T> symbols) {
			this(symbols, null);
		}

		protected <T extends Symbol> Value(Collection<? extends T> symbolsA, Collection<? extends T> symbolsB) {
			this.symbols = new Multiset<T>();
			if (symbolsA != null) symbolsInternal().addAll(symbolsA);
			if (symbolsB != null) symbolsInternal().addAll(symbolsB);
			polishSymbols();
		}

		protected void polishSymbols() {
			double generic = 0.0;

			for(Multiset<Symbol>.UniqueIterator iter = symbolsInternal().uniqueIterator(); iter.hasNext();) {
				Symbol sym = iter.next();

				if (sym instanceof Symbol.Generic) {
					generic += sym.value() * iter.count();
					iter.remove();
				}
			}

			if (generic > 0) symbolsInternal().add(Symbol.Generic.of(generic));

			for (Symbol.Atom atom : Symbol.Atom.values()) {
				if (atom.whole != null) {
					int count = symbolsInternal().count(atom);
					if (count > 1) {
						symbolsInternal().add(atom.whole, count / 2);
						symbolsInternal().remove(atom, count - count % 2);
					}
				}
			}

			this.symbols.sort(Comparator.comparing(Symbol::color, this.color().symbolOrder()));
		}

		@Override
		public double value() {
			return symbols.stream().mapToDouble(Symbol::value).sum();
		}

		@Override
		public Color.Combination color() {
			return symbols.stream().map(Symbol::color).collect(Color.Combination.COMBO_COLLECTOR);
		}

		protected Color.Combination minimalColor() {
			return symbols.stream().map(s -> s instanceof Symbol.Hybrid ? ((Symbol.Hybrid) s).firstColor() : s.color()).collect(Color.Combination.COMBO_COLLECTOR);
		}

		@Override
		public int hashCode() {
			return symbols.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			return (other instanceof Value) && ((Value) other).symbols.equals(symbols);
		}

		@Override
		public String toString() {
			return "{" + symbols.stream().map(Symbol::toString).collect(Collectors.joining("}{")) + "}";
		}

		public Collection<? extends Symbol> symbols() {
			return Collections.unmodifiableCollection(symbols);
		}

		protected Multiset<Symbol> symbolsInternal() {
			// I'm like 90% sure this cast is always safe. Multiset<? extends Symbol> should always be a subclass of Multiset<Symbol> for our purposes.
			return (Multiset<Symbol>) symbols;
		}

		protected static class Venn {
			public final Value aOnly, both, bOnly;

			public static Venn of(Value a, Value b) {
				double genericA = 0.0, genericB = 0.0;

				Multiset<Symbol> aOnly = new Multiset<>(a.symbolsInternal());
				Multiset<Symbol> both = new Multiset<>();
				Multiset<Symbol> bOnly = new Multiset<>(b.symbolsInternal());

				for (Multiset<Symbol>.UniqueIterator iter = aOnly.uniqueIterator(); iter.hasNext(); ) {
					Symbol sym = iter.next();
					if (sym instanceof Symbol.Generic) {
						genericA += sym.value() * iter.count();
						iter.remove();
					}
				}

				for (Multiset<Symbol>.UniqueIterator iter = bOnly.uniqueIterator(); iter.hasNext(); ) {
					Symbol sym = iter.next();
					if (sym instanceof Symbol.Generic) {
						genericB += sym.value() * iter.count();
						iter.remove();
					}
				}

				for (Multiset<Symbol>.UniqueIterator iter = a.symbolsInternal().uniqueIterator(); iter.hasNext(); ) {
					Symbol sym = iter.next();
					if (sym instanceof Symbol.Generic) continue;

					int shared = Math.min(aOnly.count(sym), bOnly.count(sym));
					if (shared > 0) {
						aOnly.remove(sym, shared);
						bOnly.remove(sym, shared);
						both.add(sym, shared);
					}
				}

				double sharedGeneric = Math.min(genericA, genericB);
				if (sharedGeneric > 0) {
					genericA -= sharedGeneric;
					genericB -= sharedGeneric;
					both.add(Symbol.Generic.of(sharedGeneric));
				}

				if (genericA > 0) {
					aOnly.add(Symbol.Generic.of(genericA));
				}

				if (genericB > 0) {
					bOnly.add(Symbol.Generic.of(genericB));
				}

				return new Venn(new Value(aOnly), new Value(both), new Value(bOnly));
			}

			public Venn(Value aOnly, Value both, Value bOnly) {
				this.aOnly = aOnly;
				this.both = both;
				this.bOnly = bOnly;
			}

			public int asComparison() {
				if (aOnly.symbols.isEmpty() && bOnly.symbols.isEmpty()) return 0;
				if (aOnly.symbols.isEmpty()) return -1;
				if (bOnly.symbols.isEmpty()) return 1;
				if (aOnly.color() != bOnly.color()) return Color.Combination.EMPTY_FIRST_COMPARATOR.compare(aOnly.color(), bOnly.color());
				return 0;
			}
		}

		public int devotion(Color color) {
			return (int) symbolsInternal().stream().filter(s -> s.color().contains(color)).count();
		}

		public boolean pure() {
			return false;
		}

		public boolean varies() {
			return symbolsInternal().stream().anyMatch(s -> s instanceof Symbol.Variable);
		}

		public Value copy() {
			return new Value(symbols);
		}

		public Value add(Value other) {
			symbolsInternal().addAll(other.symbolsInternal());
			polishSymbols();
			return this;
		}

		public Value substitute(Symbol.Variable variable, Value.Pure substitution) {
			int limit = symbolsInternal().count(variable);
			for (int i = 0; i < limit; ++i) {
				symbolsInternal().addAll(substitution.symbolsInternal());
			}
			symbolsInternal().remove(variable, limit);
			polishSymbols();
			return this;
		}

		public Pure.Family expand() {
			throw new UnsupportedOperationException("This hasn't been implemented yet.");
		}

		public String debugStr() {
			return String.format("%s (value %.1f, color %s, min color %s, %s)", this, value(), color(), minimalColor(), this instanceof Pure ? "pure" : "hybrid");
		}

		// TODO: I hate the names of these comparators.
		public static final Comparator<Value> SEARCH_COMPARATOR = (a, b) -> {
			Venn venn = Venn.of(a, b);
			return venn.asComparison();
		};

		public static final Comparator<Value> DEVOTION_COMPARATOR = (a, b) -> {
			for (Color color : Color.values()) {
				int ad = a.devotion(color), bd = b.devotion(color);
				if (ad != bd) return ad - bd;
			}

			return 0;
		};

		public static final Comparator<Value> SYMBOL_COMPARATOR = Comparator.comparing(Value::pure)
				.thenComparing(DEVOTION_COMPARATOR);

		public static final Comparator<Value> COMPLETE_COMPARATOR = Comparator.comparing(Value::color, Color.Combination.EMPTY_LAST_COMPARATOR)
				.thenComparing(Value::value)
				.thenComparing(SYMBOL_COMPARATOR);

		public static final Collector<Value, Value, Value> COLLECTOR = Collector.of(Value::of, Value::add, Value::add);
	}

	static void listsEqual(List<Symbol> symbolsIn, List<Symbol> expected) {
		assert symbolsIn.equals(expected);
	}

	static void main(String[] args) {
		listsEqual(
				Mana.Symbol.symbolsIn("{2/W}{2/U}{2/B}{2/R}{2/G}").collect(Collectors.toList()),
				Arrays.asList(Mana.Symbol.Hybrid.TWO_WHITE, Mana.Symbol.Hybrid.TWO_BLUE, Mana.Symbol.Hybrid.TWO_BLACK, Mana.Symbol.Hybrid.TWO_RED, Mana.Symbol.Hybrid.TWO_GREEN)
		);

		listsEqual(
				Mana.Symbol.symbolsIn("Flashback {2}{R}").collect(Collectors.toList()),
				Arrays.asList(Mana.Symbol.Generic.TWO, Mana.Symbol.Atom.Red)
		);

		listsEqual(
				Mana.Symbol.symbolsIn("Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and you gain that much life.)").collect(Collectors.toList()),
				Collections.emptyList()
		);
	}
}

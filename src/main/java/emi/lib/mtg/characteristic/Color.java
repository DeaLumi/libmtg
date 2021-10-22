package emi.lib.mtg.characteristic;

import emi.lib.mtg.util.CollectionComparator;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by Emi on 5/6/2016.
 */
public enum Color {
	White("W", "White"),
	Blue("U", "Blue"),
	Black("B", "Black"),
	Red("R", "Red"),
	Green("G", "Green"),
	Colorless("C", "Colorless"); // SPECIFICALLY colorless. This should NOT be used for generic costs or colorless creatures.

	public final String letter, name;

	Color(String letter, String name) {
		this.letter = letter;
		this.name = name;
	}

	public static Color fromString(String from) {
		for (Color c : Color.values()) {
			if (from.length() == 1 && c.letter.equals(from) || c.name.equals(from)) {
				return c;
			}
		}

		throw new NoSuchElementException(String.format("No MTG color associated with %s", from));
	}

	public enum Combination implements Set<Color> {
		Empty("Empty"),

		White("White", Color.White),
		WhiteBlue("Azorius", Color.White, Color.Blue),
		WhiteBlack("Orzhov", Color.White, Color.Black),

		Blue("Blue", Color.Blue),
		BlueBlack("Dimir", Color.Blue, Color.Black),
		BlueRed("Izzet", Color.Blue, Color.Red),

		Black("Black", Color.Black),
		BlackRed("Rakdos", Color.Black, Color.Red),
		BlackGreen("Golgari", Color.Black, Color.Green),

		Red("Red", Color.Red),
		RedGreen("Gruul", Color.Red, Color.Green),
		RedWhite("Boros", Color.Red, Color.White),

		Green("Green", Color.Green),
		GreenWhite("Selesnya", Color.Green, Color.White),
		GreenBlue("Simic", Color.Green, Color.Blue),

		WUG("Bant", Color.Green, Color.White, Color.Blue),
		WUB("Esper", Color.White, Color.Blue, Color.Black),
		UBR("Grixis", Color.Blue, Color.Black, Color.Red),
		BRG("Jund", Color.Black, Color.Red, Color.Green),
		WRG("Naya", Color.Red, Color.Green, Color.White),
		WBR("Mardu", "Savai", "Dega", Color.Red, Color.White, Color.Black),
		URG("Temur", "Ketria", "Ceta", Color.Green, Color.Blue, Color.Red),
		WBG("Abzan", "Indatha", "Necra", Color.White, Color.Black, Color.Green),
		WUR("Jeskai", "Raugrin", "Raka", Color.Blue, Color.Red, Color.White),
		UBG("Sultai", "Zagoth", "Ana", Color.Black, Color.Green, Color.Blue),

		NotWhite("Chaos", Color.Blue, Color.Black, Color.Red, Color.Green),
		NotBlue("Aggression", Color.Black, Color.Red, Color.Green, Color.White),
		NotBlack("Altruism", Color.Red, Color.Green, Color.White, Color.Blue),
		NotRed("Growth", Color.Green, Color.White, Color.Blue, Color.Black),
		NotGreen("Artifice", Color.White, Color.Blue, Color.Black, Color.Red),

		FiveColor("Five-Color", "5C", Color.White, Color.Blue, Color.Black, Color.Red, Color.Green),

		Colorless("Colorless", Color.Colorless),

		WhiteC("White+Colorless", Color.Colorless, Color.White),
		WhiteBlueC("Azorius+Colorless", Color.Colorless, Color.White, Color.Blue),
		WhiteBlackC("Orzhov+Colorless", Color.Colorless, Color.White, Color.Black),

		BlueC("Blue", Color.Colorless, Color.Blue),
		BlueBlackC("Dimir+Colorless", Color.Colorless, Color.Blue, Color.Black),
		BlueRedC("Izzet+Colorless", Color.Colorless, Color.Blue, Color.Red),

		BlackC("Black+Colorless", Color.Colorless, Color.Black),
		BlackRedC("Rakdos+Colorless", Color.Colorless, Color.Black, Color.Red),
		BlackGreenC("Golgari+Colorless", Color.Colorless, Color.Black, Color.Green),

		RedC("Red+Colorless", Color.Colorless, Color.Red),
		RedGreenC("Gruul+Colorless", Color.Colorless, Color.Red, Color.Green),
		RedWhiteC("Boros+Colorless", Color.Colorless, Color.Red, Color.White),

		GreenC("Green+Colorless", Color.Colorless, Color.Green),
		GreenWhiteC("Selesnya+Colorless", Color.Colorless, Color.Green, Color.White),
		GreenBlueC("Simic+Colorless", Color.Colorless, Color.Green, Color.Blue),

		WUGC("Bant+Colorless", Color.Colorless, Color.Green, Color.White, Color.Blue),
		WUBC("Esper+Colorless", Color.Colorless, Color.White, Color.Blue, Color.Black),
		UBRC("Grixis+Colorless", Color.Colorless, Color.Blue, Color.Black, Color.Red),
		BRGC("Jund+Colorless", Color.Colorless, Color.Black, Color.Red, Color.Green),
		WRGC("Naya+Colorless", Color.Colorless, Color.Red, Color.Green, Color.White),
		WBRC("Mardu+Colorless", "Savai+Colorless", "Dega+Colorless", Color.Colorless, Color.Red, Color.White, Color.Black),
		URGC("Temur+Colorless", "Ketria+Colorless", "Ceta+Colorless", Color.Colorless, Color.Green, Color.Blue, Color.Red),
		WBGC("Abzan+Colorless", "Indatha+Colorless", "Necra+Colorless", Color.Colorless, Color.White, Color.Black, Color.Green),
		WURC("Jeskai+Colorless", "Raugrin+Colorless", "Raka+Colorless", Color.Colorless, Color.Blue, Color.Red, Color.White),
		UBGC("Sultai+Colorless", "Zagoth+Colorless", "Ana+Colorless", Color.Colorless, Color.Black, Color.Green, Color.Blue),

		NotWhiteC("Chaos+Colorless", Color.Colorless, Color.Blue, Color.Black, Color.Red, Color.Green),
		NotBlueC("Aggression+Colorless", Color.Colorless, Color.Black, Color.Red, Color.Green, Color.White),
		NotBlackC("Altruism+Colorless", Color.Colorless, Color.Red, Color.Green, Color.White, Color.Blue),
		NotRedC("Growth+Colorless", Color.Colorless, Color.Green, Color.White, Color.Blue, Color.Black),
		NotGreenC("Artifice+Colorless", Color.Colorless, Color.White, Color.Blue, Color.Black, Color.Red),

		FiveColorC("Five-Color+Colorless", "5C+C", Color.Colorless, Color.White, Color.Blue, Color.Black, Color.Red, Color.Green),
		;

		public static final Collector<Color, AtomicInteger, Combination> COLOR_COLLECTOR = Collector.of(
				() -> new AtomicInteger(0),
				(i, c) -> i.accumulateAndGet(1 << c.ordinal(), (i1, i2) -> i1 | i2),
				(a, b) -> { a.accumulateAndGet(b.get(), (i1, i2) -> i1 | i2); return a; },
				i -> Combination.byMask(i.get())
		);

		public static final Collector<Combination, AtomicInteger, Combination> COMBO_COLLECTOR = Collector.of(
				() -> new AtomicInteger(0),
				(i, c) -> i.accumulateAndGet(c.mask, (i1, i2) -> i1 | i2),
				(a, b) -> { a.accumulateAndGet(b.get(), (i1, i2) -> i1 | i2); return a; },
				i -> Combination.byMask(i.get())
		);

		public static final CollectionComparator<Color.Combination> COMPARATOR = (a, b) -> {
			if (a.mask == b.mask) return CollectionComparator.Result.Equal;

			int intersection = a.mask & b.mask;
			if (intersection == a.mask) return CollectionComparator.Result.Contains;
			if (intersection == b.mask) return CollectionComparator.Result.ContainedIn;

			return intersection == 0 ? CollectionComparator.Result.Disjoint : CollectionComparator.Result.Intersects;
		};

		public static final Comparator<Color.Combination> EMPTY_FIRST_COMPARATOR = (a, b) -> {
			if (a == b) return 0;
			if (a.size() != b.size()) return a.size() - b.size();
			return a.ordinal() - b.ordinal();
		};

		public static final Comparator<Color.Combination> EMPTY_LAST_COMPARATOR = (a, b) -> {
			if (a == b) return 0;
			if (a == Color.Combination.Empty) return 1;
			if (b == Color.Combination.Empty) return -1;
			if (a.size() != b.size()) return a.size() - b.size();
			return a.ordinal() - b.ordinal();
		};

		private static final List<Combination> BY_MASK = makeByMask();

		private static List<Combination> makeByMask() {
			Combination[] array = new Combination[1 << Color.values().length];

			for (Combination combo : Combination.values())
				array[combo.mask] = combo;

			return Collections.unmodifiableList(Arrays.asList(array));
		}

		public static int mask(Color... colors) {
			int mask = 0;
			for (Color color : colors) mask |= (1 << color.ordinal());
			return mask;
		}

		public static int mask(Iterable<Color> colors) {
			int mask = 0;
			for (Color color : colors) mask |= (1 << color.ordinal());
			return mask;
		}

		public static Combination byMask(int mask) {
			if (mask < 0 || mask > BY_MASK.size()) throw new NoSuchElementException();
			return BY_MASK.get(mask);
		}

		public static Combination byColors(Iterable<Color> colors) {
			if (colors instanceof Combination) return (Combination) colors;

			return byMask(mask(colors));
		}

		public static Combination byColors(Color... colors) {
			return byColors(Arrays.asList(colors));
		}

		private final int mask;
		private final Color[] colorsArray;
		public final List<Color> colors;
		public final List<String> aliases;

		Combination(String alias, Color... colors) {
			this(new String[] { alias }, colors);
		}

		Combination(String aliasA, String aliasB, Color... colors) {
			this(new String[] { aliasA, aliasB }, colors);
		}

		Combination(String aliasA, String aliasB, String aliasC, Color... colors) {
			this(new String[] { aliasA, aliasB, aliasC }, colors);
		}

		Combination(String[] aliases, Color... colorsArray) {
			this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
			this.colorsArray = colorsArray;
			this.colors = Collections.unmodifiableList(Arrays.asList(colorsArray));

			int mask = 0;
			for (Color color : colorsArray) {
				mask |= (1 << color.ordinal());
			}
			this.mask = mask;
		}

		// TODO Might use this later, but my IDE gives a nice enum value summary when referring to elements created as above...
		Combination(Combination basis, String aliasModifier, Color... addins) {
			List<Color> list = new ArrayList<>(basis.colors);
			list.addAll(Arrays.asList(addins));
			this.aliases = Collections.unmodifiableList(basis.aliases.stream().map(s -> s + aliasModifier).collect(Collectors.toList()));
			this.colorsArray = list.toArray(new Color[0]);
			this.colors = Collections.unmodifiableList(list);

			int mask = 0;
			for (Color color : colorsArray) {
				mask |= (1 << color.ordinal());
			}
			this.mask = mask;
		}

		@Override
		public int size() {
			return colorsArray.length;
		}

		@Override
		public boolean isEmpty() {
			return mask == 0;
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Color)) return false;
			return (mask & (1 << ((Color) o).ordinal())) != 0;
		}

		@Override
		public ListIterator<Color> iterator() {
			return colors.listIterator();
		}

		@Override
		public Object[] toArray() {
			return Arrays.copyOf(colorsArray, colorsArray.length);
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return Arrays.copyOf(colorsArray, colorsArray.length, (Class<T[]>) a.getClass());
		}

		@Override
		public boolean add(Color color) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			if (!(c instanceof Combination)) {
				for (Object o : c) if (!contains(o)) return false;
				return true;
			}

			return (mask & ((Combination) c).mask) == ((Combination) c).mask;
		}

		@Override
		public boolean addAll(Collection<? extends Color> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		public Combination plus(Color other) {
			return Combination.byMask(mask | (1 << other.ordinal()));
		}

		public Combination plus(Combination other) {
			return Combination.byMask(mask | other.mask);
		}

		public Combination minus(Color other) {
			return Combination.byMask(mask & ~(1 << other.ordinal()));
		}

		public Combination minus(Combination other) {
			return Combination.byMask(mask & ~other.mask);
		}

		public Combination plus(Iterable<Color> other) {
			return plus(byColors(other));
		}

		public Combination minus(Iterable<Color> other) {
			return minus(byColors(other));
		}

		public Combination plus(Color... others) {
			return plus(byColors(others));
		}

		public Combination minus(Color... others) {
			return minus(byColors(others));
		}

		public Comparator<Combination> symbolOrder() {
			return (a, b) -> {
				if (a == b) return 0;
				if ((a.size() + b.size() != 3) && a.size() != b.size()) return a.size() - b.size(); // The != 3 is so hybrids settle in between their colors.
				if (a.colorsArray[0] != b.colorsArray[0]) return colors.indexOf(a.colorsArray[0]) - colors.indexOf(b.colorsArray[0]);
				if (a.size() != b.size()) return a.size() - b.size();
				return 0;
			};
		}

		@Override
		public String toString() {
			return aliases.get(0);
		}
	}
}

package emi.lib.mtg.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Multiset<T> implements Collection<T> {
	public class ImmutableIterator implements java.util.Iterator<T> {
		protected final ListIterator<T> backing;
		protected T last;
		protected int remaining;

		public ImmutableIterator() {
			this.backing = Multiset.this.list.listIterator();
		}

		private boolean advance() {
			if (!backing.hasNext()) return false;
			last = backing.next();
			remaining = histogram.get(last).get();
			return true;
		}

		@Override
		public boolean hasNext() {
			while (last == null || remaining <= 0) {
				if (!advance()) return false;
			}

			return true;
		}

		@Override
		public T next() {
			while (last == null || remaining <= 0) {
				if (!advance()) throw new NoSuchElementException();
			}

			--remaining;
			return last;
		}
	}

	public class MutableIterator extends ImmutableIterator {
		@Override
		public void remove() {
			if (last == null || remaining <= 0) throw new IllegalStateException();

			Multiset.this.remove(last);
			--remaining;
		}
	}

	public class UniqueIterator implements java.util.Iterator<T> {
		private final ListIterator<T> backing;
		private T last;

		public UniqueIterator() {
			this.backing = Multiset.this.list.listIterator();
		}

		@Override
		public boolean hasNext() {
			return backing.hasNext();
		}

		@Override
		public T next() {
			last = backing.next();
			return last;
		}

		@Override
		public void remove() {
			if (last == null) throw new IllegalStateException();

			backing.remove();
			Multiset.this.removeAll(last);
			last = null;
		}

		public int count() {
			return Multiset.this.count(last);
		}
	}

	private final Map<T, AtomicInteger> histogram = new HashMap<>();
	private final List<T> list = new ArrayList<>();
	private final AtomicInteger size = new AtomicInteger(0);
	private int hashCode = 0;

	public Multiset() {
	}

	public Multiset(Iterable<? extends T> source) {
		if (source instanceof Multiset) {
			addAll((Multiset<? extends T>) source);
		} else {
			for (T elem : source) {
				size.incrementAndGet();
				if (!histogram.containsKey(elem)) {
					histogram.put(elem, new AtomicInteger(0));
					list.add(elem);
				}
				histogram.get(elem).incrementAndGet();
			}
		}
	}

	public Multiset(T... source) {
		this(Arrays.asList(source));
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public boolean isEmpty() {
		return size.get() == 0 || histogram.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return histogram.containsKey(o) && histogram.get(o).get() > 0;
	}

	@Override
	public MutableIterator iterator() {
		return new MutableIterator();
	}

	public ImmutableIterator immutableIterator() {
		return new ImmutableIterator();
	}

	public UniqueIterator uniqueIterator() {
		return new UniqueIterator();
	}

	public Stream<T> uniqueStream() {
		return list.stream();
	}

	@Override
	public Object[] toArray() {
		Object[] array = new Object[size.get()];

		Iterator iter = iterator();
		for (int i = 0; i < array.length; ++i) {
			array[i] = iter.next();
		}

		return array;
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		T1[] array = Arrays.copyOf(a, size.get());

		Iterator iter = iterator();
		for (int i = 0; i < array.length; ++i) {
			array[i] = (T1) iter.next();
		}

		return array;
	}

	@Override
	public boolean add(T t) {
		return add(t, 1) != 1;
	}

	public int add(T t, int count) {
		size.addAndGet(count);
		if (!histogram.containsKey(t)) {
			histogram.put(t, new AtomicInteger(count));
			list.add(t);
		} else {
			count = histogram.get(t).addAndGet(count);
		}
		hashCode = 0;
		return count;
	}

	@Override
	public boolean remove(Object o) {
		return remove(o, 1) != Integer.MIN_VALUE;
	}

	public int removeAll(Object o) {
		if (!histogram.containsKey(o)) return 0;

		int count = histogram.get(o).get();
		size.addAndGet(-count);
		histogram.remove(o);
		list.remove(o);
		hashCode = 0;

		return count;
	}

	/**
	 * Removes the given number of instances of the specified object.
	 * @param o     The object to remove.
	 * @param count The number of instances of that object to remove.
	 * @return Integer.MIN_VALUE if there were no instances of that object, or the difference between the original count of that object and the requested removal count. If this value is zero or negative, the set now contains no instances of the object.
	 */
	public int remove(Object o, int count) {
		if (!histogram.containsKey(o)) return Integer.MIN_VALUE;

		int remainder = histogram.get(o).addAndGet(-count);
		if (remainder <= 0) {
			histogram.remove(o);
			list.remove(o);
			size.addAndGet(remainder - count);
		} else {
			size.addAndGet(-count);
		}
		hashCode = 0;

		return remainder;
	}

	public boolean containsAll(Iterable<?> c) {
		// We can work faster if it's another multiset, by directly comparing counts.
		if (c instanceof Multiset) {
			for (Map.Entry<?, AtomicInteger> entry : ((Multiset<?>) c).histogram.entrySet()) {
				if (!histogram.containsKey(entry.getKey())) return false;
				if (histogram.get(entry.getKey()).get() < entry.getValue().get()) return false;
			}

			return true;
		}

		Map<Object, AtomicInteger> tmp = new HashMap<>();
		for (Object elem : c) {
			if (!histogram.containsKey(elem)) return false;
			if (histogram.get(elem).get() < tmp.computeIfAbsent(elem, k -> new AtomicInteger(0)).incrementAndGet()) return false;
		}

		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c.size() > size()) return false;

		return containsAll((Iterable<?>) c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (c.isEmpty()) return false;

		if (c instanceof Multiset) {
			// Cast is safe -- `c` is `? extends T`, and is a `Multiset`.
			Multiset<? extends T> other = (Multiset<? extends T>) c;
			for (Map.Entry<? extends T, AtomicInteger> entry : other.histogram.entrySet()) {
				if (histogram.containsKey(entry.getKey())) {
					histogram.get(entry.getKey()).addAndGet(entry.getValue().get());
				} else {
					histogram.put(entry.getKey(), new AtomicInteger(entry.getValue().get()));
					list.add(entry.getKey());
				}
			}

			size.addAndGet(other.size.get());
		} else {
			for (T elem : c) add(elem);
		}
		hashCode = 0;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c.isEmpty()) return false;

		boolean modified = false;

		if (c instanceof Multiset) {
			Multiset<?>.UniqueIterator iter = ((Multiset<?>) c).uniqueIterator();
			while (iter.hasNext()) {
				Object o = iter.next();
				if (remove(o, iter.count()) != Integer.MIN_VALUE) modified = true;
			}
			if (modified) hashCode = 0;
			return modified;
		}

		for (Object elem : c) if (remove(elem)) modified = true;
		if (modified) hashCode = 0;
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;

		if (c.isEmpty()) {
			modified = size() > 0;
			clear();
			return modified;
		}

		Map<Object, AtomicInteger> otherHisto;

		if (c instanceof Multiset) {
			otherHisto = ((Multiset) c).histogram;
		} else {
			otherHisto = new HashMap<>();
			for (Object elem : c) {
				if (!histogram.containsKey(elem)) continue;
				otherHisto.computeIfAbsent(elem, k -> new AtomicInteger(0)).incrementAndGet();
			}
		}

		size.set(0);
		java.util.Iterator<Map.Entry<T, AtomicInteger>> iter = histogram.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<T, AtomicInteger> next = iter.next();
			AtomicInteger peer = otherHisto.get(next.getKey());
			if (peer == null || peer.get() <= 0) {
				iter.remove();
				list.remove(next.getKey());
			} else {
				if (modified = next.getValue().get() > peer.get()) next.getValue().set(peer.get());
				size.addAndGet(next.getValue().get());
			}
		}

		if (modified) hashCode = 0;

		return modified;
	}

	@Override
	public void clear() {
		histogram.clear();
		list.clear();
		size.set(0);
		hashCode = 0;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = 1;
			for (T elem : this) hashCode *= elem.hashCode();
		}

		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Collection)) return false;
		if (obj instanceof List || obj instanceof Set) return false;
		if (obj instanceof Multiset && obj.hashCode() != hashCode()) return false;
		return ((Collection<?>) obj).size() == size() && containsAll((Collection<?>) obj);
	}

	@Override
	public String toString() {
		ImmutableIterator it = immutableIterator();
		if (! it.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			T e = it.next();
			sb.append(e == this ? "(this Collection)" : e);
			if (! it.hasNext())
				return sb.append(']').toString();
			sb.append(',').append(' ');
		}
	}

	public void sort(Comparator<? super T> comparator) {
		list.sort(comparator);
	}

	public int count(Object o) {
		return histogram.containsKey(o) ? histogram.get(o).get() : 0;
	}

	public static class Venn<T> {
		public final Collection<T> onlyInA, inBoth, onlyInB;

		public Venn(Collection<T> onlyInA, Collection<T> inBoth, Collection<T> onlyInB) {
			this.onlyInA = onlyInA;
			this.inBoth = inBoth;
			this.onlyInB = onlyInB;
		}

		public CollectionComparator.Result asComparison() {
			if (onlyInA.isEmpty() && onlyInB.isEmpty()) return CollectionComparator.Result.Equal;
			if (onlyInA.isEmpty()) return CollectionComparator.Result.ContainedIn;
			if (onlyInB.isEmpty()) return CollectionComparator.Result.Contains;
			return inBoth.isEmpty() ? CollectionComparator.Result.Disjoint : CollectionComparator.Result.Intersects;
		}
	}

	public Venn<T> venn(Collection<T> b) {
		Multiset<T> aOnly = new Multiset<>(this);
		Multiset<T> both = new Multiset<>();
		Multiset<T> bOnly = new Multiset<>(b);

		for (T k : list) {
			int shared = Math.min(aOnly.count(k), bOnly.count(k));
			if (shared > 0) {
				aOnly.remove(k, shared);
				bOnly.remove(k, shared);
				both.add(k, shared);
			}
		}

		return new Venn<>(aOnly, both, bOnly);
	}
}

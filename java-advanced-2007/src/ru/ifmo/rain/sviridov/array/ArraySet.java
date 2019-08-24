package ru.ifmo.rain.sviridov.array;

import java.lang.reflect.Array;
import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> elements;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        elements = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<? extends T> collection) {
        elements = List.copyOf(new TreeSet<>(collection));
        comparator = null;
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        TreeSet<T> temp = new TreeSet<>(comparator);
        temp.addAll(collection);
        elements = List.copyOf(temp);
        this.comparator = comparator;
    }

    private ArraySet(List<T> elements, Comparator<? super T> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    private int positionIndex(T t) {
        return Collections.binarySearch(elements, Objects.requireNonNull(t), comparator);
    }

    private int lowerIndex(T t, boolean isEnlusive) {
        int i = positionIndex(t);
        if (i < 0) {
            return -i - 2;
        }
        return isEnlusive ? i : i - 1;
    }

    private int higherIndex(T t, boolean isEnxlusive) {
        int i = positionIndex(t);
        if (i < 0) {
            return -i - 1;
        }
        return isEnxlusive ? i : i + 1;
    }

    private T get(int index) {
        if (0 <= index && index < elements.size()) {
            return elements.get(index);
        } else {
            return null;
        }
    }

    @Override
    public T lower(T t) {
        return get(lowerIndex(t, false));
    }

    @Override
    public T floor(T t) {
        return get(lowerIndex(t, true));
    }

    @Override
    public T ceiling(T t) {
        return get(higherIndex(t, true));
    }

    @Override
    public T higher(T t) {
        return get(higherIndex(t, false));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("Poll first is not supported");

    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("Poll last is not supported");

    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean contains(Object o) {
        try {
            return positionIndex((T) Objects.requireNonNull(o)) >= 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }


    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Iterator<?> i = collection.iterator(); i.hasNext(); ) {
            if (!contains(i.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(elements), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    private ArraySet<T> sSet(T from, boolean fInclusive, T to, boolean toInclusive) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Arguments should be non-null");
        }
        int start = higherIndex(from, fInclusive);
        int end = lowerIndex(to, toInclusive);
        return end < start ? new ArraySet<>(Collections.emptyList(), comparator) :
                new ArraySet<>(elements.subList(start, end + 1), comparator);
    }

    @Override
    public ArraySet<T> subSet(T t, boolean b, T e1, boolean b1) {
        if (comparator != null && comparator.compare(t, e1) > 0 ||
                comparator == null && t instanceof Comparable && ((Comparable) t).compareTo(e1) > 0) {
            throw new IllegalArgumentException("Left bound is greated than right one");
        } else {
            return sSet(t, b, e1, b1);
        }
    }

    @Override
    public NavigableSet<T> headSet(T t, boolean b) {
        if (isEmpty()) {
            return this;
        }
        return sSet(first(), true, t, b);
    }

    @Override
    public NavigableSet<T> tailSet(T t, boolean b) {
        if (isEmpty()) {
            return this;
        }
        return sSet(t, b, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T t, T e1) {
        return subSet(t, true, e1, false);
    }

    @Override
    public SortedSet<T> headSet(T t) {
        return headSet(t, false);
    }

    @Override
    public SortedSet<T> tailSet(T t) {
        return tailSet(t, true);
    }

    @Override
    public T first() {
        if (get(0) == null) {
            throw new NoSuchElementException("attempted to access an element which doesn't exist");
        } else return get(0);
    }

    @Override
    public T last() {
        if (get(elements.size() - 1) == null) {
            throw new NoSuchElementException("attempted to access an element which doesn't exist");
        } else return get(elements.size() - 1);
    }

    @Override
    public boolean isEmpty() {
        return (elements.size() == 0); // ?
    }
}

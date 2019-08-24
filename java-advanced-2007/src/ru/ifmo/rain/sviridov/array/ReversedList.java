package ru.ifmo.rain.sviridov.array;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class ReversedList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> list;

    public ReversedList(List<T> list) {
        this.list = list;
    }
    @Override
    public T get(int i) {
        return list.get(list.size() - i - 1);
    }

    @Override
    public int size() {
        return list.size();
    }
}

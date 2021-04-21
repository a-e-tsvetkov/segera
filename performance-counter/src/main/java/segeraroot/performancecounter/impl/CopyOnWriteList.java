package segeraroot.performancecounter.impl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

public class CopyOnWriteList<T> {
    private final AtomicReference<T[]> values;
    private final IntFunction<T[]> newArray;

    public CopyOnWriteList(IntFunction<T[]> newArray) {
        this.newArray = newArray;
        values = new AtomicReference<>(newArray.apply(0));
    }

    public void add(T counter) {
        T[] tmp;
        T[] newValue;
        do {
            tmp = values.get();
            newValue = newArray.apply(tmp.length + 1);
            System.arraycopy(tmp, 0, newValue, 0, tmp.length);
            newValue[tmp.length] = counter;
        } while (!values.compareAndSet(tmp, newValue));
    }


    public void remove(T counter) {
        T[] tmp;
        T[] newValue;
        do {
            tmp = values.get();
            newValue = newArray.apply(tmp.length - 1);
            ArrayUtil.copyExcept(tmp, newValue, counter);
        } while (!values.compareAndSet(tmp, newValue));
    }

    public T[] get() {
        return values.get();
    }
}

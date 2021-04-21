package segeraroot.performancecounter;

public interface Aggregator extends ValueConsumer<Void> {
    void add(int value);

    <T> T reset(ValueConsumer<T> consumer);

    long getSum();

    long getCount();


}


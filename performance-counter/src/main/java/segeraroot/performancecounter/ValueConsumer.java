package segeraroot.performancecounter;

public interface ValueConsumer<T> {
    ValueConsumer<?> NULL = (sum, count) -> null;

    T consume(long sum, long count);
}

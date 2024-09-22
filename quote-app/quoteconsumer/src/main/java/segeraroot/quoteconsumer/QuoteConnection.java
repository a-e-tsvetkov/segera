package segeraroot.quoteconsumer;

public interface QuoteConnection<T> {
    void write(T messageWrapper);

    String getName();
}

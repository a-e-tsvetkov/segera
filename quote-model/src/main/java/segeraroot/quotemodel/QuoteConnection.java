package segeraroot.quotemodel;

public interface QuoteConnection<T> {
    void write(T messageWrapper);

    Object get();

    void set(Object context);

    String getName();
}

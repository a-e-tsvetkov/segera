package segeraroot.quotemodel;

public interface QuoteConnection {
    void write(MessageWrapper<Message> messageWrapper);

    Object get();

    void set(Object context);

    String getName();
}

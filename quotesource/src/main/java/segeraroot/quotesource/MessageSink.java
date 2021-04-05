package segeraroot.quotesource;

public interface MessageSink<T> {
    void send(T message);
}

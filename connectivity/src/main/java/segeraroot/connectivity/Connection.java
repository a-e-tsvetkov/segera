package segeraroot.connectivity;

public interface Connection<T> {
    void startWriting();

    Object get();

    void set(Object context);

    String getName();
}

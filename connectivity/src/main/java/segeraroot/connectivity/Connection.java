package segeraroot.connectivity;

public interface Connection {
    void startWriting();

    Object get();

    void set(Object context);

    String getName();
}

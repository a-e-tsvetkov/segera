package segeraroot.connectivity;

public interface Connection {
    void startWriting();

    <C> C get();

    String getName();
}

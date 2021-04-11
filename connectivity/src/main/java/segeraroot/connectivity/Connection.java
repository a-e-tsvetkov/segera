package segeraroot.connectivity;

public interface Connection<T> {
    void write(T messageWrapper);

    Object get();

    void set(Object context);

    String getName();
}

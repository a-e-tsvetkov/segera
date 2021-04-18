package segeraroot.connectivity.impl;


import segeraroot.connectivity.Connection;

public abstract class ConnectionBase<T> implements Connection {
    private Object context;

    public Object get() {
        return context;
    }

    public void set(Object context) {
        this.context = context;
    }
}

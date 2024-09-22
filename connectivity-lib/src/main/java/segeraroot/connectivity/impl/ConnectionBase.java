package segeraroot.connectivity.impl;


import segeraroot.connectivity.Connection;

public abstract class ConnectionBase implements Connection {
    private Object context;

    @SuppressWarnings("unchecked")
    public <C> C get() {
        return (C) context;
    }

    public void set(Object context) {
        this.context = context;
    }
}

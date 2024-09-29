package segeraroot.connectivity.impl;


import lombok.Setter;
import segeraroot.connectivity.Connection;

public abstract class ConnectionBase implements Connection {
    @Setter
    private Object context;

    @SuppressWarnings("unchecked")
    public <C> C get() {
        return (C) context;
    }
}

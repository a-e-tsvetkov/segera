package segeraroot.connectivity.impl;


import lombok.Setter;
import segeraroot.connectivity.Connection;

@Setter
public abstract class ConnectionWithContext implements Connection {
    private Object context;

    @SuppressWarnings("unchecked")
    public <C> C get() {
        return (C) context;
    }
}

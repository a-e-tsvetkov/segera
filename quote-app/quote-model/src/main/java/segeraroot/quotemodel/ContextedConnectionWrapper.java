package segeraroot.quotemodel;

import lombok.experimental.Delegate;
import lombok.Setter;
import segeraroot.connectivity.Connection;

public class ContextedConnectionWrapper implements Connection {
    @Delegate
    private final Connection connection;
    @Setter
    private Object context;

    public ContextedConnectionWrapper(Connection connection) {
        this.connection = connection;
    }

    @SuppressWarnings("unchecked")
    public <C> C get() {
        return (C) context;
    }
}

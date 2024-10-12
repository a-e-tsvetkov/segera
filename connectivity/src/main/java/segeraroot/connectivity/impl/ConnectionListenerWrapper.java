package segeraroot.connectivity.impl;

import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ConnectionListener;

import java.util.function.Function;

@RequiredArgsConstructor
public class ConnectionListenerWrapper<C extends ContextWrapper> implements ConnectionListener {
    private final ConnectionListener connectionListener;
    private final Function<Connection, C> contextFactory;

    @Override
    public void handleCloseConnection(Connection connection) {
        ContextWrapper context = connection.get();
        connectionListener.handleCloseConnection(context.getInnerConnection());
    }

    @Override
    public C handleNewConnection(Connection connection) {
        ConnectionDelegate wrapper = new ConnectionDelegate(connection);
        Object innerContext = connectionListener.handleNewConnection(wrapper);
        wrapper.setContext(innerContext);
        return contextFactory.apply(wrapper);
    }
}

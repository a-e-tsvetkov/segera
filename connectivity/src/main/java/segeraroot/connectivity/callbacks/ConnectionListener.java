package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

public interface ConnectionListener {
    ConnectionListener NOOP = new ConnectionListener() {
        @Override
        public void handleCloseConnection(Connection connection) {

        }

        @Override
        public Object handleNewConnection(Connection connection) {
            return null;
        }
    };

    void handleCloseConnection(Connection connection);

    Object handleNewConnection(Connection connection);
}

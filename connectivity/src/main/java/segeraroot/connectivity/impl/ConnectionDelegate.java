package segeraroot.connectivity.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;


@RequiredArgsConstructor
public class ConnectionDelegate extends ConnectionWithContext {
    @Delegate
    private final Connection connection;

    //Do not delegate this
    @SuppressWarnings("EmptyMethod")
    @Override
    public <C> C get() {
        return super.get();
    }
}

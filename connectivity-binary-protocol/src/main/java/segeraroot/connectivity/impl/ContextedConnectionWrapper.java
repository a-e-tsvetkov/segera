package segeraroot.connectivity.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;


@RequiredArgsConstructor
public class ContextedConnectionWrapper extends ConnectionBase {
    @Delegate
    private final Connection connection;

    //Do not delegate this
    @Override
    public <C> C get() {
        return super.get();
    }
}

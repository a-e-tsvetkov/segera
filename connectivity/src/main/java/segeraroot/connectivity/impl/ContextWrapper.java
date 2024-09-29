package segeraroot.connectivity.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ContextWrapper {
    private final ContextedConnectionWrapper innerConnection;
}

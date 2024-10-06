package segeraroot.connectivity.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.Connection;

@Getter
@RequiredArgsConstructor
public class ContextWrapper {
    private final Connection innerConnection;
}

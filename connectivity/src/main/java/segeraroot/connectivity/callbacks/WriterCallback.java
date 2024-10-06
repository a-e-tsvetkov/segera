package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

public interface WriterCallback<W> {
    OperationResult handleWriting(Connection connection, W writer);
}

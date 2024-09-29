package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

public interface WriterCallback<W> {
    WritingResult handleWriting(Connection connection, W writer);
}

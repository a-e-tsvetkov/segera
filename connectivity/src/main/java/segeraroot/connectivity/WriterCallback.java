package segeraroot.connectivity;

public interface WriterCallback<W> {
    WritingResult handleWriting(Connection connection, W writer);
}

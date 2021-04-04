package segeraroot.quotemodel;

public interface QuoteConnectionCallback<T> {
    void handleCloseConnection(QuoteConnection<T> connection);

    void handleNewConnection(QuoteConnection<T> connection);

    void handleMessageConnection(QuoteConnection<T> connection, T messageWrapper);
}

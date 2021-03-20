package segeraroot.quotemodel;

public interface QuoteConnectionCallback {
    void handleCloseConnection(QuoteConnection connection);

    void handleNewConnection(QuoteConnection connection);

    void handleMessageConnection(QuoteConnection connection, MessageWrapper<?> messageWrapper);
}

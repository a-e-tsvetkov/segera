package segeraroot.quotesource;

import segeraroot.quotemodel.QuoteConnection;

public abstract class QuoteConnectionBase<T> implements QuoteConnection<T> {
    private Object context;

    public Object get() {
        return context;
    }

    public void set(Object context) {
        this.context = context;
    }
}

package segeraroot.quoteconsumer;

import lombok.Getter;

@Getter
public class QuoteProtocolWrongVersionException extends Exception {
    private final int expectedVersion;
    private final int actualVersion;

    public QuoteProtocolWrongVersionException(int expectedVersion, int actualVersion) {
        super("Protocol version doesn't match: " + expectedVersion + " " + actualVersion);
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
}

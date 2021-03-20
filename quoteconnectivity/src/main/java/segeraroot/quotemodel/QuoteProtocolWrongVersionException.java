package segeraroot.quotemodel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class QuoteProtocolWrongVersionException extends Exception {
    private final int expectedVersion;
    private final int actualVersion;
}

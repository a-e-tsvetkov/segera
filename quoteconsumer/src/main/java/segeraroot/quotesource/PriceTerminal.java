package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.Quote;

@Slf4j
public class PriceTerminal {
    public void acceptQuote(Quote quote) {
        log.info("Quote received: {}", quote);
    }
}

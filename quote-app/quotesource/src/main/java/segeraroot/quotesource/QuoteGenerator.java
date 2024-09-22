package segeraroot.quotesource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.QuoteSupport;
import segeraroot.quotemodel.messages.Quote;

import java.time.Instant;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class QuoteGenerator {
    private final String symbol;
    private final Consumer<Quote> quoteConsumer;
    private final ThreadFactory threadFactory;
    private volatile boolean running;

    public void start() {
        running = true;
        Thread thread = threadFactory.newThread(this::generatorLoop);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    private void generatorLoop() {
        log.info("Start: {}", symbol);
        while (running) {
            Quote quote = Quote.builder()
                    .date(Instant.now().toEpochMilli())
                    .symbol(QuoteSupport.convert(symbol))
                    .volume(100)
                    .price(6_00100)
                    .build();
            log.debug("Send quote {}", quote);
            try {
                quoteConsumer.accept(quote);
            } catch (RuntimeException e) {
                log.error("Unable to send quote: " + quote, e);
            } catch (Throwable e) {
                log.error("ERROR DURING PROCESSING: " + quote, e);
                stop();
            }

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Stop: {}", symbol);
    }
}

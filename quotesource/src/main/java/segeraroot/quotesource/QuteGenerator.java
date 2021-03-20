package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.Quote;

import java.time.Instant;
import java.util.function.Consumer;

@Slf4j
public class QuteGenerator {
    private final String symbol;
    private final Consumer<Quote> quoteConsumer;
    private volatile boolean running;

    public QuteGenerator(String symbol, Consumer<Quote> quoteConsumer) {
        this.symbol = symbol;
        this.quoteConsumer = quoteConsumer;
    }

    public void start() {
        running = true;
        Thread thread = new Thread(this::generatorLoop);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    private void generatorLoop() {
        while (running) {
            Quote quote = Quote.builder()
                    .date(Instant.now())
                    .symbol(symbol)
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
    }
}

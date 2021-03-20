package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.*;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class PriceTerminal {
    private final Map<MessageType, Consumer<Message>> map = new HashMap<>();
    private String[] symbols;

    public PriceTerminal(String... symbols) {
        this.symbols = symbols;
        register(MessageType.QUOTE, this::acceptQuote);
    }

    private <T> void register(MessageType messageType, Consumer<T> consumer) {
        //noinspection unchecked
        map.put(messageType, message -> consumer.accept((T) message));
    }


    private void acceptMessage(QuoteConnection connection, MessageWrapper<?> messageWrapper) {
        Consumer<Message> consumer = map.get(messageWrapper.getType());
        if (consumer == null) {
            acceptDefault(messageWrapper.getValue());
        } else {
            consumer.accept(messageWrapper.getValue());
        }
    }

    private void acceptDefault(Message value) {
        log.error("Unexpected message {}", value);
    }

    private void acceptQuote(Quote quote) {
        log.info("Quote received: {}", quote);
    }

    public QuoteConnectionCallback getCallback() {
        return SimpleQuoteConnectionCallback.builder()
                .newHandler(this::onNewConnection)
                .closeHandler(connection -> {
                })
                .messageHandler(this::acceptMessage)
                .build();
    }

    private void onNewConnection(QuoteConnection quoteConnection) {
        for (String symbol : symbols) {
            subscribe(quoteConnection, symbol);
        }
    }

    private void subscribe(QuoteConnection quoteConnection, String symbol) {
        quoteConnection.write(MessageWrapper.builder()
                .type(MessageType.SUBSCRIBE)
                .value(Subscribe.builder()
                        .symbol(symbol)
                        .build())
                .build());
    }
}

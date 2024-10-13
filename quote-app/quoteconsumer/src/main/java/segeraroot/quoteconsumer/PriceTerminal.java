package segeraroot.quoteconsumer;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quoteconsumer.messages.Quote;
import segeraroot.quoteconsumer.messages.Subscribe;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class PriceTerminal {
    private final Map<MessageType, Consumer<Message>> map;
    private final String[] symbols;

    public PriceTerminal(String... symbols) {
        this.symbols = symbols;
        map = Map.ofEntries(entry(MessageType.QUOTE, this::acceptQuote));
    }

    @SuppressWarnings("unchecked")
    private <T> Map.Entry<MessageType, Consumer<Message>> entry(MessageType messageType, Consumer<T> consumer) {
        return Map.entry(messageType, message -> consumer.accept((T) message));
    }


    private void acceptMessage(QuoteConnection<MessageWrapper> connection, MessageWrapper messageWrapper) {
        var consumer = map.get(messageWrapper.getType());
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

    public QuoteConnectionCallback<MessageWrapper> getCallback() {
        return SimpleQuoteConnectionCallback.<MessageWrapper>builder()
            .newHandler(this::onNewConnection)
            .closeHandler(connection -> {
            })
            .messageHandler(this::acceptMessage)
            .build();
    }

    private void onNewConnection(QuoteConnection<MessageWrapper> quoteConnection) {
        for (String symbol : symbols) {
            subscribe(quoteConnection, symbol);
        }
    }

    private void subscribe(QuoteConnection<MessageWrapper> quoteConnection, String symbol) {
        log.debug("subscribe: {} {}", quoteConnection.getName(), symbol);
        quoteConnection.write(MessageWrapper.builder()
            .type(MessageType.SUBSCRIBE)
            .value(Subscribe.builder()
                .symbol(symbol)
                .build())
            .build());
    }
}

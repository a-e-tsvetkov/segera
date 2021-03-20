package segeraroot.quotemodel;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Slf4j
public abstract class ConnectionHandlerBase implements QuoteConnection {
    private final Socket socket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final Object writeLock = new Object();
    private volatile boolean running;
    private Object context;

    public ConnectionHandlerBase(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void start() {
        Thread thread = new Thread(this::readQuotesLoop);
        running = true;
        thread.start();
    }

    private void stop() {
        running = false;
        handleCloseConnection();
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Error while closing socket: {}", e.getMessage());
        }
    }

    protected abstract void handleCloseConnection();

    @Override
    public final void write(MessageWrapper<Message> messageWrapper) {
        synchronized (writeLock) {
            try {
                getSerialization().writeMessage(messageWrapper, this.outputStream);
                this.outputStream.flush();
            } catch (IOException e) {
                log.error("Unable to write to socket: {}", e.getMessage());
                stop();
            }
        }
    }

    protected abstract void onMessage(MessageWrapper<?> messageWrapper);

    protected abstract Serialization getSerialization();

    protected final void readQuotesLoop() {
        while (running) {
            MessageWrapper<?> messageWrapper = null;
            try {
                messageWrapper = getSerialization().readMessage(inputStream);
                onMessage(messageWrapper);
            } catch (IOException e) {
                log.error("Exception in read loop: {}", e.getMessage());
                stop();
            } catch (RuntimeException e) {
                log.error("Failed to process quote: " + messageWrapper, e);
            } catch (Throwable e) {
                log.error("ERROR DURING PROCESSING: " + messageWrapper, e);
                stop();
            }
        }
    }

    protected final void readHeader() {
        try {
            int version = getSerialization().readHeader(inputStream);
            log.info("Protocol version {}", version);
        } catch (IOException e) {
            log.error("Exception while read header", e);
            stop();
        } catch (QuoteProtocolWrongVersionException e) {
            log.error("Unexpected header version: expected={} actual={}",
                    e.getExpectedVersion(),
                    e.getActualVersion());
            stop();
        }
    }

    protected final void writeHeader() {
        try {
            getSerialization().writeHeader(outputStream);
        } catch (IOException e) {
            log.error("Exception while read header", e);
            stop();
        }
    }

    @Override
    public final String getName() {
        return socket.getRemoteSocketAddress().toString();
    }

    @Override
    public final Object get() {
        return context;
    }

    @Override
    public final void set(Object context) {
        assert this.context == null;
        this.context = context;
    }
}

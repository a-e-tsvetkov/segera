package segeraroot.quotemodel;

import lombok.Getter;

@Getter
public enum MessageType {
    SUBSCRIBE(1),
    UNSUBSCRIBE(2),
    QUOTE(3),
    SYMBOLS_REQ(4),
    SYMBOLS_RES(5);

    private final byte code;
    private static final MessageType[] values;

    MessageType(int code) {
        this.code = (byte) code;
    }

    static {
        values = new MessageType[MessageType.values().length];
        for (MessageType messageType : MessageType.values()) {
            assert messageType.getCode() > 0;
            assert messageType.getCode() <= values.length;
            assert values[messageType.getCode() - 1] == null;
            values[messageType.getCode() - 1] = messageType;
        }
    }

    public static MessageType valueOf(byte typeByte) {
        return values[typeByte - 1];
    }
}

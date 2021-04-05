package segeraroot.quoteconsumer;

import lombok.Getter;

@Getter
public enum MessageType {
    SUBSCRIBE(1, 3),
    UNSUBSCRIBE(2, 3),
    QUOTE(3, 15),
    SYMBOLS_REQ(4, 0),
    SYMBOLS_RES(5, 0);

    private final byte code;
    private final int size;
    private static final MessageType[] values;

    MessageType(int code, int size) {
        this.code = (byte) code;
        this.size = size;
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

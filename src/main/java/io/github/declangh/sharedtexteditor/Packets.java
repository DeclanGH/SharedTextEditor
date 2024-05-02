package io.github.declangh.sharedtexteditor;

import java.nio.ByteBuffer;

public class Packets {

    public enum Operation {
        INSERT,
        DELETE
    }

    private static final int INT_PARAMS_SIZE = Integer.BYTES * 2;
    private static final int OPERATION_SIZE = Integer.BYTES;
    private static final int OPNUM_SIZE = Integer.BYTES;

    public static byte [] createInsertPacket(int offset, int opNum, int length, String characters) {
        if (characters == null) throw new NullPointerException("characters cannot be null");
        int charactersLength = characters.length() * 2;

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + OPNUM_SIZE + INT_PARAMS_SIZE + charactersLength);

        // put the ordinal value of the Operation
        packetBuffer.putInt(Operation.INSERT.ordinal());
        packetBuffer.putInt(opNum);
        packetBuffer.putInt(offset);
        packetBuffer.putInt(length);

        for (char c : characters.toCharArray())
            packetBuffer.putChar(c);

        // Return the byte array
        return packetBuffer.array();
    }

    public static byte[] createDeletePacket(int offset, int opNum, int length) {

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + OPNUM_SIZE + INT_PARAMS_SIZE);

        // put the ordinal value of the Operation, offset and length
        packetBuffer.putInt(Operation.DELETE.ordinal());
        packetBuffer.putInt(opNum);
        packetBuffer.putInt(offset);
        packetBuffer.putInt(length);

        return packetBuffer.array();
    }

    public static Operation parseOperation(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        int operationOrdinal = packetBuffer.getInt();

        // return int at the first position of packet
        return Operation.values()[operationOrdinal];
    }

    public static int parseOperationNum(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        packetBuffer.getInt();

        // return int at the second position of packet
        return packetBuffer.getInt();
    }

    public static int parseOffset(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        packetBuffer.getInt();
        packetBuffer.getInt();

        return packetBuffer.getInt();
    }

    public static int parseLength(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        // skip until we get to position of length
        packetBuffer.getInt();
        packetBuffer.getInt();
        packetBuffer.getInt();

        return packetBuffer.getInt();
    }

    public static String parseString(byte[] packet) {
        Operation operation = parseOperation(packet);

        // we won't make this mistake, but incase
        if (operation != Operation.INSERT)
            throw new IllegalArgumentException("Attempt to parse characters from a non-insert packet");

        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.getInt(); // Skip operation
        packetBuffer.getInt(); // Skip operation number
        packetBuffer.getInt(); // Skip offset
        packetBuffer.getInt(); // Skip length

        // iterate the remaining bytes (expected to be chars) and append them to form a string
        StringBuilder builder = new StringBuilder();
        while (packetBuffer.hasRemaining()) {
            builder.append(packetBuffer.getChar());
        }

        return builder.toString();
    }
}
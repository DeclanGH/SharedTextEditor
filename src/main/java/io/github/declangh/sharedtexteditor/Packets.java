package io.github.declangh.sharedtexteditor;

import java.nio.ByteBuffer;

public class Packets {

    public enum Operation {
        INSERT,
        DELETE
    }

    private static final int INT_PARAMS_SIZE = Integer.BYTES * 2;
    private static final int OPERATION_SIZE = Integer.BYTES;

    public static byte[] createInsertPacket(int offset, int length, String characters) {
        int charactersLength = characters.getBytes().length;

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + INT_PARAMS_SIZE + charactersLength);

        // put the ordinal value of the Operation
        packetBuffer.putInt(Operation.INSERT.ordinal());

        packetBuffer.putInt(offset);
        packetBuffer.putInt(length);

        for (char c : characters.toCharArray())
            packetBuffer.putChar(c);

        // Return the byte array
        return packetBuffer.array();
    }

    public static byte[] createDeletePacket(int offset, int length) {

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + INT_PARAMS_SIZE);

        // put the ordinal value of the Operation, offset and length
        packetBuffer.putInt(Operation.DELETE.ordinal());
        packetBuffer.putInt(offset);
        packetBuffer.putInt(length);

        return packetBuffer.array();
    }

    public static Operation parseOperation(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        int operationOrdinal = packetBuffer.getInt();

        return Operation.values()[operationOrdinal];
    }

    public static int parseOffset(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.getInt();

        return packetBuffer.getInt();
    }

    public static int parseLength(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        // skip until we get to position of length
        packetBuffer.getInt();
        packetBuffer.getInt();

        return packetBuffer.getInt();
    }

    public static int parseString(byte[] packet) {
        Operation operation = parseOperation(packet);

        if (operation != Operation.INSERT)
            throw new IllegalArgumentException("Attempt to parse characters from a non-insert packet");

        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.getInt(); // Skip operation
        packetBuffer.getInt(); // Skip offset
        packetBuffer.getInt(); // Skip length

        return packetBuffer.getInt();
    }
}
package io.github.declangh.sharedtexteditor;

import java.nio.ByteBuffer;

public class Packets {

    public enum Operation {
        INSERT,
        DELETE,
        REQUEST,
        UPDATE
    }

    private static final int INT_PARAMS_SIZE = Integer.BYTES * 2;
    private static final int OPERATION_SIZE  = Integer.BYTES;
    private static final int OPNUM_SIZE      = Integer.BYTES;

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

    public static byte[] createTextAreaRequestPacket(String requesterID) {

        int uidLength = requesterID.length() * 2;

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + 4 + uidLength);

        packetBuffer.putInt(Operation.REQUEST.ordinal());

        // put the ID length, necessary for parsing ID
        packetBuffer.putInt(requesterID.length()/2);

        // put the ID
        for (char c : requesterID.toCharArray()) packetBuffer.putChar(c);

        return packetBuffer.array();
    }

    public static byte[] createUpdatePacket(String requesterID, String textArea) {
        int charactersLength = requesterID.length() * 2;

        ByteBuffer packetBuffer = ByteBuffer.allocate(OPERATION_SIZE + 4 + charactersLength);

        // put the operation
        packetBuffer.putInt(Operation.UPDATE.ordinal());

        // put the ID length, necessary for parsing ID
        packetBuffer.putInt(requesterID.length()/2);

        // put the ID
        for (char c : requesterID.toCharArray()) packetBuffer.putChar(c);

        // put the textArea
        for (char c : textArea.toCharArray()) packetBuffer.putChar(c);

        return packetBuffer.array();
    }

    public static Operation parseOperation(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        int operationOrdinal = packetBuffer.getInt();

        // return int at the first position of packet
        return Operation.values()[operationOrdinal];
    }

    public static String parseID(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        packetBuffer.getInt();

        int limit = packetBuffer.getInt();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < limit; i++) {
            if (packetBuffer.hasRemaining()) builder.append(packetBuffer.getChar());
            else break;
        }



        return builder.toString();
    }

    public static String parseTextArea(byte[] packet) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);

        // skip operation
        packetBuffer.getInt();

        // skip length
        int limit = packetBuffer.getInt();

        // skip id
        for (int i = 0; i < limit; i++)
            if (packetBuffer.hasRemaining()) packetBuffer.getChar();
            else break;

        StringBuilder builder = new StringBuilder();

        // get text area
        while (packetBuffer.hasRemaining()) {
            builder.append(packetBuffer.getChar());
        }

        return builder.toString();
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
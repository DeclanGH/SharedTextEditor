package io.github.declangh.sharedtexteditor;

import java.nio.ByteBuffer;

public class Packets {

    public byte[] insertPacket(int numOps, short offset, short length, String characters){
        //The header telling the receipient to insert
        String INSERT = "INSERT";

        //Byte buffer that will hold packet allocate (INSERT.bytes.length + characters.bytes.length + short.bytes*3) 
        ByteBuffer packetBuffer = ByteBuffer.allocate(INSERT.getBytes().length + (Short.BYTES*3) + characters.getBytes().length);

        //Insert the operations into the buffer
        //First the operation
        packetBuffer.put(INSERT.getBytes());

        //Then the number of operations
        packetBuffer.putInt(numOps);

        //Then the offset
        packetBuffer.putShort(offset);

        //Then the length
        packetBuffer.putShort(length);

        //Then the characters (so you know how many bytes to grab)
        packetBuffer.put(characters.getBytes());

        //Now turn it into an array of bytes
        byte[] packet = packetBuffer.array();

        return packet;
    }

    public byte[] deletePacket(int numOps, short offset, short length){
        //The header telling the receipient to insert
        String DELETE = "DELETE";

        //Byte buffer that will hold packet allocate (INSERT.bytes.length + characters.bytes.length + short.bytes*3) 
        ByteBuffer packetBuffer = ByteBuffer.allocate(DELETE.getBytes().length + (Short.BYTES*3));

        //Insert the operations into the buffer
        //First the number of operations
        packetBuffer.putInt(numOps);

        //Then the offset
        packetBuffer.putShort(offset);

        //Then the length
        packetBuffer.putShort(length);

        //Now turn it into an array of bytes
        byte[] packet = packetBuffer.array();

        return packet;
    }
}

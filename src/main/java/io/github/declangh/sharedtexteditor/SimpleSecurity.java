package io.github.declangh.sharedtexteditor;

public class SimpleSecurity {

    // simple rng using xor shift
    public static long generateRandomNumber(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    // this would be the starting iteration of encryption. We want to ignore the Operation Ordinal
    private static final int START_ITR = Integer.BYTES;

    public static byte[] encrypt(byte[] byteArray, long key) {

        System.out.println("Encrypting with: " + key);

        if (byteArray.length <= START_ITR) {
            System.out.println("First four bytes are assumed to be Operation Ordinal.");
            throw new IllegalArgumentException("Byte array must be at least 5 bytes long!");
        }

        for (int i=START_ITR; i<byteArray.length; i++) {
            byteArray[i] = (byte) (byteArray[i] ^ (key & 0xFF));
        }

        return byteArray;
    }

    public static byte[] decrypt(byte[] byteArray, long key) {

        System.out.println("Decrypting with: " + key);

        if (byteArray.length <= START_ITR) {
            System.out.println("First four bytes are assumed to be Operation Ordinal.");
            throw new IllegalArgumentException("Byte array must be at least 5 bytes long!");
        }

        for (int i=START_ITR; i<byteArray.length; i++) {
            byteArray[i] = (byte) (byteArray[i] ^ (key & 0xFF));
        }

        return byteArray;
    }
}

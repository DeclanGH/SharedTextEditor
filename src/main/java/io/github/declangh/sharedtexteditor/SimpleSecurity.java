package io.github.declangh.sharedtexteditor;

public class SimpleSecurity {

    // simple rng using xor shift
    public static long generateRandomNumber(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    public static byte[] encrypt(byte[] byteArray, long key) {

        for (int i=0; i<byteArray.length; i++) {
            byteArray[i] = (byte) (byteArray[i] ^ (key & 0xFF));
        }

        return byteArray;
    }

    public static byte[] decrypt(byte[] byteArray, long key) {

        for (int i=0; i<byteArray.length; i++) {
            byteArray[i] = (byte) (byteArray[i] ^ (key & 0xFF));
        }

        return byteArray;
    }
}

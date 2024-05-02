package io.github.declangh.sharedtexteditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;

public class AEADEncryption 
{
    public static KeysetHandle createKey() throws GeneralSecurityException 
    {
        AeadConfig.register();

        KeyTemplate keyTemplate = null;
        KeysetHandle keysetHandle = null;

        // Create the encrypting key
        keyTemplate = KeyTemplates.get("AES256_GCM");
        keysetHandle = KeysetHandle.generateNew(keyTemplate);

        return keysetHandle;
    }

    public static byte[] keyToByteArray(KeysetHandle keysetHandle) throws IOException, GeneralSecurityException 
    {
        AeadConfig.register();

        ByteArrayOutputStream keyBAOS = null;
        ObjectOutputStream keyOOS = null;
        byte[] keysetArr = null;

        // Turn the key into a byte array
        keyBAOS = new ByteArrayOutputStream();
        keyOOS = new ObjectOutputStream(keyBAOS);

        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(keyOOS));
        keyOOS.close();

        keysetArr = keyBAOS.toByteArray();

        return keysetArr;
    }

    public static KeysetHandle byteArrayToKey(byte[] keyset) throws GeneralSecurityException, IOException 
    {
        AeadConfig.register();

        ByteArrayInputStream keyBAIS = null;
        ObjectInputStream keyOIS     = null;
        KeysetHandle keysetHandle    = null;

        keyBAIS = new ByteArrayInputStream(keyset);
        keyOIS = new ObjectInputStream(keyBAIS);

        keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(keyOIS));
        keyOIS.close();

        return keysetHandle;
    }

    public static byte[] encrypt(byte[] plaintext, String associatedData, KeysetHandle keysetHandle) throws UnsupportedEncodingException, GeneralSecurityException 
    {
        AeadConfig.register();

        Aead aead = null;
        byte[] ciphertext;

        // Create an AEAD Interface out of the encrypting key, which will perform the actual encrypting/decrypting
        aead = keysetHandle.getPrimitive(Aead.class);

        // Encrypt some message
        ciphertext = aead.encrypt(plaintext, associatedData.getBytes("ISO-8859-1"));

        return ciphertext;
    }

    public static byte[] decrypt(byte[] ciphertext, String associatedData, KeysetHandle keysetHandle) throws GeneralSecurityException, UnsupportedEncodingException 
    {
        AeadConfig.register();

        Aead aead = null;
        byte[] plaintext;

        // Create an AEAD Interface out of the encrypting key, which will perform the actual encrypting/decrypting
        aead = keysetHandle.getPrimitive(Aead.class);

        // Encrypt some message
        plaintext = aead.decrypt(ciphertext, associatedData.getBytes("ISO-8859-1"));

        return plaintext;
    }
}

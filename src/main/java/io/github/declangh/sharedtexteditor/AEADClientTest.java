package io.github.declangh.sharedtexteditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.GeneralSecurityException;
import java.util.Scanner;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;

public class AEADClientTest 
{
    public static void main(String[] args) throws IOException, GeneralSecurityException
    {
        //////////////////////////////// Local Variables ////////////////////////////////

        Scanner scan = new Scanner(System.in);

        String host        = "localhost";  // The name of the Server
        int port           = 10000;        // The port that will be accessed on the Server
        String keyTemplate = "AES256_GCM"; // The encryption algorithm that will be used

        // Variables used when extracting data from ByteBuffers
        int kBLimits          = 0;
        byte[] keyset         = null;
        int cBLimits          = 0;
        byte[] ciphertext     = null;
        int aDBLimits         = 0;
        byte[] associatedData = null;

        String associatedDataStr   = ""; // The associated data that is a part of the encryption/decryption process
        String decryptedCiphertext = ""; // The Server's decrypted message
        String clientMessage       = ""; // The message the Client wants to send over

        // ByteBuffers used to send and receive encrypted data
        ByteBuffer keyTemplateBuff    = null;
        ByteBuffer keysetBuff         = null;
        ByteBuffer ciphertextBuff     = null;
        ByteBuffer associatedDataBuff = null;

        // Various Objects needed to handle the encryption keys
        ByteArrayInputStream keyBAIS = null;
        ObjectInputStream keyOIS     = null;
        KeysetHandle keysetHandle    = null;
        Aead aead                    = null;

        //////////////////////////////// Setting up the Client ////////////////////////////////

        DatagramChannel client = DatagramChannel.open();
        InetSocketAddress serverAddress = new InetSocketAddress(host, port);
        client.bind(null);

        System.out.println("Connecting to the Server...\n");

        //////////////////////////////// Creating the Encryption/Decryption Key ////////////////////////////////

        // Send the chosen encryption algorithm to the Server
        keyTemplateBuff = ByteBuffer.wrap(keyTemplate.getBytes("ISO-8859-1"));
        client.send(keyTemplateBuff, serverAddress);

        // Receive the encrypting key created using the chosen algorithm from the Server
        keysetBuff = ByteBuffer.allocate(2048);
        client.receive(keysetBuff);

        System.out.println("You've connected to the Server and received the key!\n");

        // Extract the key
        keysetBuff.flip();
        kBLimits = keysetBuff.limit();
        keyset = new byte[kBLimits];
        keysetBuff.get(keyset, 0, kBLimits);
        
        keyBAIS = new ByteArrayInputStream(keyset);
        keyOIS = new ObjectInputStream(keyBAIS);

        keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withInputStream(keyOIS));
        keyOIS.close();

        //////////////////////////////// Receiving and Sending Data with the Key ////////////////////////////////

        // Create an AEAD Interface out of the encrypting key, which will perform the actual encrypting/decrypting
        AeadConfig.register();
        aead = keysetHandle.getPrimitive(Aead.class);

        // Receive ciphertext from the Server
        ciphertextBuff = ByteBuffer.allocate(2048);
        client.receive(ciphertextBuff);

        // Receive data associated with the ciphertext from the Server
        associatedDataBuff = ByteBuffer.allocate(2048);
        client.receive(associatedDataBuff);

        // Extract the ciphertext
        ciphertextBuff.flip();
        cBLimits = ciphertextBuff.limit();
        ciphertext = new byte[cBLimits];
        ciphertextBuff.get(ciphertext, 0, cBLimits);

        // Extract the associated data
        associatedDataBuff.flip();
        aDBLimits = associatedDataBuff.limit();
        associatedData = new byte[aDBLimits];
        associatedDataBuff.get(associatedData, 0, aDBLimits);
        associatedDataStr = new String(associatedData, "ISO-8859-1");

        // Decrypt and display the message from the Server
        decryptedCiphertext = new String(aead.decrypt(ciphertext, associatedDataStr.getBytes("ISO-8859-1")));
        System.out.println(decryptedCiphertext);

        while(true)
        {
            System.out.print("Type a message to send: ");
            clientMessage = scan.nextLine();

            // Encrypt some message for the Server to receive
            ciphertext = aead.encrypt(clientMessage.getBytes("ISO-8859-1"), associatedDataStr.getBytes("ISO-8859-1"));

            // Send the Server the ciphertext
            ciphertextBuff = ByteBuffer.wrap(ciphertext);
            client.send(ciphertextBuff, serverAddress);

            // Receive ciphertext from the Server
            ciphertextBuff = ByteBuffer.allocate(2048);
            client.receive(ciphertextBuff);

            // Extract the ciphertext
            ciphertextBuff.flip();
            cBLimits = ciphertextBuff.limit();
            ciphertext = new byte[cBLimits];
            ciphertextBuff.get(ciphertext, 0, cBLimits);

            // Decrypt and display the message from the Server
            decryptedCiphertext = new String(aead.decrypt(ciphertext, associatedDataStr.getBytes("ISO-8859-1")));
            System.out.println(decryptedCiphertext);
        }
    }
}


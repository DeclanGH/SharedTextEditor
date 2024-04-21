package io.github.declangh.sharedtexteditor;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;

public class AEADServerTest 
{
    public static void main(String[] args) throws Exception, GeneralSecurityException 
    {
        //////////////////////////////// Local Variables ////////////////////////////////

        String host            = "localhost"; // The name of the Server
        int port               = 10000;       // The port that will be accessed on the Server
        String keyTemplateName = "";          // The encryption algorithm that is used by the encryption key

        String plaintext      = "Server: If you're seeing this, that means we can securely communicate with each other! :)\n" +
                                "Server: Why don't you send me a message to try it out?\n";
        String associatedData = "Google Tink AEAD";

        byte[] ciphertext          = null; // The encrypted message the Server sends to a Client
        String decryptedCiphertext = "";   // The encrypted message the Server receives from a Client

        // Variables used when extracting data from ByteBuffers
        int kTNBLimits   = 0;
        byte[] kTNBBytes = null;
        int cBLimits     = 0;

        // ByteBuffers used to send and receive encrypted data
        ByteBuffer keyTemplateNameBuff = null;
        ByteBuffer keysetBuff = null;
        ByteBuffer ciphertextBuff = null;
        ByteBuffer associatedDataBuff = null;

        // Various Objects needed to handle the encryption keys
        ByteArrayOutputStream keyBAOS = null;
        ObjectOutputStream keyOOS = null;
        byte[] keysetArr = null;
        KeyTemplate keyTemplate = null;
        KeysetHandle keysetHandle = null;
        Aead aead = null;

        //////////////////////////////// Setting up the Server ////////////////////////////////

        DatagramChannel server = DatagramChannel.open();
        InetSocketAddress iAdd = new InetSocketAddress(host, port);
        server.bind(iAdd);

        SocketAddress remoteAdd = null;

        System.out.println("Waiting for a Client to connect to...\n");

        //////////////////////////////// Creating the Encryption/Decryption Key ////////////////////////////////

        // Receive the chosen encryption algorithm from the Client
        keyTemplateNameBuff = ByteBuffer.allocate(2048);
        remoteAdd = server.receive(keyTemplateNameBuff);

        keyTemplateNameBuff.flip();
        kTNBLimits = keyTemplateNameBuff.limit();
        kTNBBytes = new byte[kTNBLimits];
        keyTemplateNameBuff.get(kTNBBytes, 0, kTNBLimits);
        keyTemplateName = new String(kTNBBytes, "ISO-8859-1");

        // Assuming an encryption algorithm was sent
        if(!(keyTemplateName.equals(null)))
        {
            System.out.println("You've connected to a Client, time to send over the key...");

            AeadConfig.register();

            // Create the encrypting key
            keyTemplate = KeyTemplates.get(keyTemplateName);
            keysetHandle = KeysetHandle.generateNew(keyTemplate);

            // Turn the key into a byte array so that it may be sent to the Client
            keyBAOS = new ByteArrayOutputStream();
            keyOOS = new ObjectOutputStream(keyBAOS);

            CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(keyOOS));
            keyOOS.close();

            keysetArr = keyBAOS.toByteArray();

            // Send the key to the Client
            keysetBuff = ByteBuffer.wrap(keysetArr);
            server.send(keysetBuff, remoteAdd);

            System.out.println("Key sent successfully!\n");

            // Create an AEAD Interface out of the encrypting key, which will perform the actual encrypting/decrypting
            aead = keysetHandle.getPrimitive(Aead.class);

            //////////////////////////////// Sending and Receiving Data with the Key ////////////////////////////////

            // Encrypt some message for the Client to receive
            ciphertext = aead.encrypt(plaintext.getBytes("ISO-8859-1"), associatedData.getBytes("ISO-8859-1"));

            // Send the Client the ciphertext
            ciphertextBuff = ByteBuffer.wrap(ciphertext);
            server.send(ciphertextBuff, remoteAdd);

            // Send the Client the data associated with the ciphertext
            associatedDataBuff = ByteBuffer.wrap(associatedData.getBytes("ISO-8859-1"));
            server.send(associatedDataBuff, remoteAdd);

            while(true)
            {
                // Receive ciphertext from the Client
                ciphertextBuff = ByteBuffer.allocate(2048);
                remoteAdd = server.receive(ciphertextBuff);

                // Extract the ciphertext
                ciphertextBuff.flip();
                cBLimits = ciphertextBuff.limit();
                ciphertext = new byte[cBLimits];
                ciphertextBuff.get(ciphertext, 0, cBLimits);

                // Decrypt and display the message from the Client
                decryptedCiphertext = new String(aead.decrypt(ciphertext, associatedData.getBytes("ISO-8859-1")));
                System.out.println("Client: " + decryptedCiphertext);

                // Send a new encrypted message back to the Client
                plaintext = "\nServer: Hey look it worked! I got your message! Just to confirm, you sent over the message \"" + decryptedCiphertext + "\", right? Try sending over another message!\n";
                ciphertext = aead.encrypt(plaintext.getBytes("ISO-8859-1"), associatedData.getBytes("ISO-8859-1"));

                ciphertextBuff = ByteBuffer.wrap(ciphertext);
                server.send(ciphertextBuff, remoteAdd);
            }
        }

        else
        {
            System.out.println("***ERROR OCCURRED DURING HANDSHAKE! NO ENCRYPTION METHOD PROVIDED! ABORTING HANDSHAKE!***");
        }
    }
}


package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;

public class EditorClient extends JFrame {

    private static JTextArea textArea;
    private JFileChooser fileChooser;

    /*
     * 1.) externalUpdateFlag tells us whether a document update was done by us or some other user
     *     It helps the document listener ignore updates by other users
     * 2 and 3) Number of times an operation was sent or received. This gets synced across members
     */
    private static boolean externalUpdateFlag = false;
    private static int lastInsertOpNum = 1;
    private static int lastDeleteOpNum = 1;

    // generate a private key for yourself for this session. Starting at 37 (Just a random prime)
    private static final long PRIVATE_KEY = new Random().nextInt(Integer.MAX_VALUE) + 37;

    // This key is dynamic and depends on the number of people in the session
    private static long sessionKey = PRIVATE_KEY;

    private static long receivedKey = 1;

    // Stored session keys helps us ignore them when they are sent to us
    private static HashSet<Long> storedSessionKeys = new HashSet<>();

    // User ID from user service class
    private final static long USER_ID = UserService.getInstance().USER_ID;

    public EditorClient() {
        setTitle("Shared Text Editor");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Open a file on your computer
        openItem.addActionListener(e -> openFile());

        // Save the file to your computer
        saveItem.addActionListener(e -> saveFile());

        // Exit the application
        exitItem.addActionListener(e -> System.exit(0));

        // Make the text area
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event){

                // Check to see if the event matches the text area, if it does,
                // that means it was added through a keyboard
                if (externalUpdateFlag == false) {
                    //System.out.println("Internal insert");
                    String insertedText;
                    try {
                        int offset = event.getOffset();
                        int length = event.getLength();

                        insertedText = event.getDocument().getText(offset, length);
                        lastInsertOpNum += 1;

                        // After doing the operation locally, get the packet to broadcast it out
                        byte[] insertPacket = Packets.createInsertPacket(offset, lastInsertOpNum, length, insertedText);
                        byte[] encryptedPacket = SimpleSecurity.encrypt(insertPacket, sessionKey);

                        UserService.getInstance().broadcast(encryptedPacket);

                    } catch (Exception e) {
                        System.out.println("Error:" + e.getMessage());
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent event){

                if (externalUpdateFlag == false) {

                    int offset = event.getOffset();
                    int length = event.getLength();

                    lastDeleteOpNum += 1;

                    // Create the packet
                    byte[] deletePacket = Packets.createDeletePacket(offset, lastDeleteOpNum, length);
                    byte[] encryptedPacket = SimpleSecurity.encrypt(deletePacket, sessionKey);

                    // Broadcast the packet to the other users in the channel
                    UserService.getInstance().broadcast(encryptedPacket);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent event){}
        });

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        fileChooser = new JFileChooser();

        // This hook waits for runtime end and closes the producer and consumers associated with that user
        // Runtime.getRuntime().addShutdownHook(new Thread(this::closeResources));
    }

    private static void keyExchange() {
        byte[] exchangeKey = Packets.createKeyExchangePacket(PRIVATE_KEY);
        UserService.getInstance().broadcast(exchangeKey);
    }

    private static void diffieHellman(long privateKey, long receivedKey) {
        // get the public key and modValue
        BigInteger sharedKey = UserService.getInstance().publicKey;
        BigInteger moduloValue = BigInteger.valueOf(UserService.getInstance().modValue);

        // for performance, we will constrain this value
        long exponent = ((privateKey % 15) + 3) * ((receivedKey % 15) + 3);

        // the diffie-hellman key exchange formula
        BigInteger diffieHellman = sharedKey.modPow(BigInteger.valueOf(exponent), moduloValue);

        // our new session key would be the resulting value of the diffie-hellman equation
        sessionKey = diffieHellman.longValue();
        storedSessionKeys.add(sessionKey);
        System.out.println("The current session key is: " + sessionKey);
    }

    private static void requestCurrentTextArea() {
        byte[] requestPacket = Packets.createTextAreaRequestPacket(USER_ID);
        UserService.getInstance().broadcast(requestPacket);
    }

    // Call the UserService method to close the resources
    private void closeResources() {
        UserService.getInstance().close();
    }

    private void openFile() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                textArea.read(reader, null);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile() {
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                textArea.write(writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void receivePacket(byte[] packet){

        // If we are receiving a packet, we are about to get an external update, so
        // we use compare the operation number we just received to what we currently have.
        // To run the updates on the Event Dispatch Thread, we use invokelater
        SwingUtilities.invokeLater(() -> {
            long parsedKey;
            byte[] decryptedPacket = null;

            switch (Packets.parseOperation(packet)) {
                case INSERT:
                    decryptedPacket = SimpleSecurity.decrypt(packet, sessionKey);
                    int insertOpNum = Packets.parseOperationNum(decryptedPacket);
                    if (insertOpNum > lastInsertOpNum) {
                        lastInsertOpNum = insertOpNum;
                        insertIntoEditor(decryptedPacket);
                    }
                    break;
                case DELETE:
                    decryptedPacket = SimpleSecurity.decrypt(packet, sessionKey);
                    int deleteOpNum = Packets.parseOperationNum(decryptedPacket);
                    if (deleteOpNum > lastDeleteOpNum) {
                        lastDeleteOpNum = deleteOpNum;
                        deleteFromEditor(decryptedPacket);
                    }
                    break;
                case REQUEST:
                    long requesterID = Packets.parseID(packet);
                    // only send update if you are not the requester
                    if (requesterID != USER_ID) {
                        sendTextArea(requesterID);
                    }
                    break;
                case UPDATE:
                    decryptedPacket = SimpleSecurity.decrypt(packet, sessionKey);
                    // only take the update if you are the requester
                    if (Packets.parseID(decryptedPacket) == USER_ID) {
                        updateTextArea(decryptedPacket);
                    }
                    break;
                case KEY_EXCHANGE:
                    parsedKey = Packets.parseKey(packet);
                    if (!storedSessionKeys.contains(parsedKey)) {
                        receivedKey = parsedKey;
                        byte[] sessionKeyPacket = Packets.createKeyPacket(sessionKey);
                        UserService.getInstance().broadcast(sessionKeyPacket);
                        diffieHellman(sessionKey, receivedKey);
                    }
                    break;
                case KEY:
                    parsedKey = Packets.parseKey(packet);
                    if (!storedSessionKeys.contains(parsedKey)) {
                        receivedKey = parsedKey;
                        storedSessionKeys.add(parsedKey);
                        diffieHellman(PRIVATE_KEY, receivedKey);
                    }
                    break;
            }
        });
    }

    private static void sendTextArea(long requesterID) {

        String textAreaText = "";
        try {
            textAreaText = textArea.getText();
        } catch (NullPointerException e) {
            return;
        }

        byte[] updatePacket = Packets.createUpdatePacket(requesterID, textAreaText);
        byte[] encryptedPacket = SimpleSecurity.encrypt(updatePacket,sessionKey);

        UserService.getInstance().broadcast(encryptedPacket);
    }

    private static void updateTextArea(byte[] packet) {
        String currentTextAreaText = textArea.getText();
        String receivedTextAreaText = Packets.parseTextArea(packet);

        // we do not want text areas repeatedly sent to us
        if (currentTextAreaText.equals(receivedTextAreaText)){
            return;
        }

        textArea.setText(receivedTextAreaText);
    }

    // This method will be for inserting characters into the text editor, it is passed in the offset and length of the inserted text
    public static void insertIntoEditor(byte[] packet) {
        int offset = Packets.parseOffset(packet);
        String characters = Packets.parseString(packet);

        try {
            externalUpdateFlag = true;
            textArea.getDocument().insertString(offset, characters, null);
            externalUpdateFlag = false;
        } catch (BadLocationException e) {
            System.out.println("Bad location insert at offset " + offset + " for " + "\""+characters+"\"");
            e.printStackTrace();
        }
    }

    //This method will be for deleting characters from the text editor, it is passed in the offset and length of the deleted text
    public static void deleteFromEditor(byte[] packet) {

        int offset = Packets.parseOffset(packet);
        int length = Packets.parseLength(packet);
        try {
            externalUpdateFlag = true;
            textArea.getDocument().remove(offset,length);
            externalUpdateFlag = false;
        } catch (BadLocationException e) {
            System.out.println("Couldn't delete at offset " + offset + " for length " + length);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // When the application is run,

        // It creates an instance of the user service that you'll use all through this session
        UserService.getInstance();

        // It then creates your text area
        new EditorClient().setVisible(true);

        // after which you request a key exchange
        storedSessionKeys.add(sessionKey);
        keyExchange();

        System.out.println("id: " + USER_ID);

        // and finally, request the current text area
        requestCurrentTextArea();
    }
}
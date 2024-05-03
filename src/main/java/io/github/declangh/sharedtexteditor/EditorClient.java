package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.lang.Runtime;

public class EditorClient extends JFrame {

    private static JTextArea textArea;
    private JFileChooser fileChooser;


    /*
     * 1.) externalUpdateFlag tells us whether a document update was done by us or some other user
     *     It helps the document listener ignore updates by other users
     * 2.)
     */
    private static boolean externalUpdateFlag = false;
    private static int operationNumber = 0;

    // User ID from user service class
    private final static String USER_ID = UserService.getInstance().USER_ID;

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

        // request the current text area when you join
        requestCurrentTextArea();

        // Make the text area
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event){

                if (!externalUpdateFlag) {
                    String insertedText;
                    try {
                        int offset = event.getOffset();
                        int length = event.getLength();

                        insertedText = event.getDocument().getText(offset, length);

                        // After doing the operation locally, get the packet to broadcast it out
                        byte[] insertPacket = Packets.createInsertPacket(offset, ++operationNumber, length, insertedText);
                        System.out.println(Arrays.toString(insertPacket));

                        UserService.getInstance().broadcast(insertPacket);

                    } catch (Exception e) {
                        System.out.println("Error:" + e.getMessage());
                    }
                }

                // If it was set to true by receivePacket function, change back to false
                externalUpdateFlag = false;
            }

            @Override
            public void removeUpdate(DocumentEvent event){

                if (!externalUpdateFlag) {

                    int offset = event.getOffset();
                    int length = event.getLength();

                    // Create the packet
                    byte[] deletePacket = Packets.createDeletePacket(offset, ++operationNumber, length);

                    // Broadcast the packet to the other users in the channel
                    UserService.getInstance().broadcast(deletePacket);


                    System.out.println("Offset " + offset + " Length " + length);
                }

                // If it was set to true by receivePacket function, change back to false
                externalUpdateFlag = false;
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
        //Runtime.getRuntime().addShutdownHook(new Thread(this::closeResources));
    }

    private void requestCurrentTextArea() {
        byte[] requestPacket = Packets.createTextAreaRequestPacket(USER_ID);
        UserService.getInstance().broadcast(requestPacket);
    }

    // Call the UserService method to close the resources
    private void closeResources() throws GeneralSecurityException, IOException {
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

        // If we are receiving a packet, we are about to get an external update
        externalUpdateFlag = true;

        int receivedOperationNumber = Packets.parseOperationNum(packet);

        if (receivedOperationNumber == operationNumber) {
            return;
        } else operationNumber = receivedOperationNumber;

        // to run the updates on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (Packets.parseOperation(packet) == Packets.Operation.INSERT) {
                insertIntoEditor(packet);
            } else if (Packets.parseOperation(packet) == Packets.Operation.DELETE) {
                deleteFromEditor(packet);
            } else if (Packets.parseOperation(packet) == Packets.Operation.REQUEST) {
                String requesterID = Packets.parseID(packet);
                // only send update if you are not the requester
                if (!requesterID.equals(USER_ID)) sendTextArea(requesterID);
            } else { // update packet
                // only take the update if you are the requester
                if (Packets.parseID(packet).equals(USER_ID)) updateTextArea(packet);
            }
        });

    }


    private static void sendTextArea(String requesterID) {

        String textAreaText = "";
        try {
            textAreaText = textArea.getText();
        } catch (NullPointerException e) {
            return;
        }

        byte[] updatePacket = Packets.createUpdatePacket(requesterID, textAreaText);
        UserService.getInstance().broadcast(updatePacket);
    }

    private static void updateTextArea(byte[] packet) {
        String currrentTextAreaText = textArea.getText();
        String receivedTextAreaText = Packets.parseTextArea(packet);

        // we do not want text areas repeatedly sent to us
        if (textArea.getDocument() != null || currrentTextAreaText.equals(receivedTextAreaText))
            return;

        textArea.setText(receivedTextAreaText);
    }

    // This method will be for inserting characters into the text editor, it is passed in the offset and length of the inserted text
    public static void insertIntoEditor(byte[] packet) {
        int offset = Packets.parseOffset(packet);
        String characters = Packets.parseString(packet);

        try {
            textArea.getDocument().insertString(offset, characters, null);
        } catch (BadLocationException e) {
            System.out.println(offset);
            System.out.println(characters);
            e.printStackTrace();
        }
    }

    //This method will be for deleting characters from the text editor, it is passed in the offset and length of the deleted text
    public static void deleteFromEditor(byte[] packet) {

        int offset = Packets.parseOffset(packet);
        int length = Packets.parseLength(packet);

        try {
            textArea.getDocument().remove(offset,length);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        UserService.getInstance();

        System.out.println("HERE");
        new EditorClient().setVisible(true);
    }
}
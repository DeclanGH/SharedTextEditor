package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Runtime;

public class EditorClient extends JFrame {

    private static JTextArea textArea;
    private JFileChooser fileChooser;
    private static HashMap<Integer, Character> textMap = new HashMap<>();


    /*
     * This flag tells us whether a document update was done by us or some external entity
     * It helps the document listener ignore updates
     */
    private static boolean externalUpdateFlag = false;

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

                if (!externalUpdateFlag) {
                    String insertedText;
                    try {
                        insertedText = event.getDocument().getText(event.getOffset(), event.getLength());

                        // After doing the operation locally, get the packet to broadcast it out
                        byte[] insertPacket = Packets.createInsertPacket(event.getOffset(), event.getLength(), insertedText);
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
                    // String removedText;
                    int offset = event.getOffset();
                    int length = event.getLength();

                    // After doing the operation locally, get the packet to broadcast it out
                    byte[] deletePacket = Packets.createDeletePacket(event.getOffset(), event.getLength());

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
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeResources));
    }

    // Call the UserService method to close the resources
    private void closeResources(){
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

        // to run the updates on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (Packets.parseOperation(packet) == Packets.Operation.INSERT) {
                insertIntoEditor(packet);
            } else {
                deleteFromEditor(packet);
            }
        });


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

    public static void main(String[] args) {
        UserService.getInstance();

        new EditorClient().setVisible(true);
    }
}
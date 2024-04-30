package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.util.HashMap;

public class EditorClient extends JFrame {

    private static JTextArea textArea;
    private JFileChooser fileChooser;
    private static HashMap<Integer, Character> textMap = new HashMap<>();

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
                String insertedText;

                try {
                    insertedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    // update map
                    addOperation(event.getOffset(), event.getLength(), insertedText);

                    // After doing the operation locally, get the packet to broadcast it out
                    byte[] insertPacket = Packets.createInsertPacket(event.getOffset(), event.getLength(), insertedText);

                    // Broadcast the packet to the other users in the channel
                    UserService.getInstance().broadcast(insertPacket);
                    
                } catch (Exception e) {
                    insertedText = "";
                }
                System.out.println(textMap.entrySet());
                System.out.println(insertedText);
            }

            @Override
            public void removeUpdate(DocumentEvent event){
                // String removedText;
                int offset = event.getOffset();
                int length = event.getLength();

                // update map
                deleteOperation(offset, length);

                // After doing the operation locally, get the packet to broadcast it out
                byte[] deletePacket = Packets.createDeletePacket(event.getOffset(), event.getLength());

                // Broadcast the packet to the other users in the channel
                UserService.getInstance().broadcast(deletePacket);

                System.out.println("Offset " + offset + " Length " + length);
            }

            @Override
            public void changedUpdate(DocumentEvent event){
                String updatedText;

                try {
                    updatedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    //CALL UPDATE MAP HERE

                    //After doing the operation locally, get the packet to broadcast it out

                    //TODO: BROADCAST THE PACKET TO THE OTHER USERS IN THE CHANNEL
                    
                } catch (Exception e) {
                    updatedText = "";
                }
                System.out.println(updatedText);                
            }
        });

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        fileChooser = new JFileChooser();
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

    // This method is for updating the hashmap for which characters map to which position.
    // This should be called after every update in the text editor.
    private static void addOperation(int offset, int length, String newCharacter){
        char value;
        int key;
        // First check to see if there is a key for that offset
        if(textMap.containsKey(offset)){
            // Make a new map
            HashMap<Integer, Character> tempMap = new HashMap<>();
            
            // Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
                
                // If it is less than the offset (the new chars position) then you can just put in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }

                // if i = the offset, put the new character at that position and the old character at the position + length
                else if(i == offset){
                    // Check if the length is greater than 1
                    if(length > 1){
                        // If it is, we need a FOR loop to add each character in the string to the map and move the old characters to the new place
                        for(int j = 0; j < length; j++){
                            tempMap.put(i + j, newCharacter.charAt(j));
                            value = textMap.get(i + j);
                            tempMap.put(i + length, value);
                        }
                    }
                    // Else just replace the one character
                    else{
                        tempMap.put(i, newCharacter.charAt(0));
                        value = textMap.get(i);
                        tempMap.put(i+1, value);
                    }
                }
                // Else, map to the new position (oldPosition + length)
                else{
                    key = i + length;
                    value = textMap.get(i);
                    tempMap.put(key, value);
                }
            }
            // Clear the old map and copy the new map
            textMap.clear();
            textMap.putAll(tempMap);
            System.out.println(tempMap.entrySet() + " temp map");
        } else{
            // Check to see the length of the new text
            if (length > 1){
                for(int i = 0; i < length; i++){
                    textMap.put(offset + i, newCharacter.charAt(i));
                }
            } else{
                // If there isnt, put the new character at that position
                textMap.put(offset, newCharacter.charAt(0));
            }
        }
    }

    // This is called when a delete operation occurs.
    private static void deleteOperation(int offset, int length){
        char value;
        int key; 

        // If the offset = size of the map
        if(offset == textMap.size()){
            // This means the offset is the last character in the map, so no need to update others
            textMap.remove(offset);
        }
        // Else there are characters that need to be moved
        else{
            // Make a new map
            HashMap<Integer, Character> tempMap = new HashMap<Integer, Character>();

            // Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
    
                // If it is less than the offset then you can just put it in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }
                // if i = the offset, then continue. We dont want that value
                else if(i == offset || i < offset + length){
                    continue;
                }
                // Else, map to the new position (oldPosition - length)
                else{
                    key = i - length;
                    value = textMap.get(i);
                    tempMap.put(key, value);
                }
            }
            //Clear the old map and copy the new map
            textMap.clear();
            textMap.putAll(tempMap);
        }
    }

    public static void receivePacket(byte[] packet){

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
        int length = Packets.parseLength(packet);

        try {
            textArea.getDocument().insertString(offset, characters, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    //This method will be for deleting characters from the text editor, it is passed in the offset and length of the deleted text
    public static void deleteFromEditor(byte[] packet) {
        // paramsToInsert[0] = offset
        int offset = paramsToDelete[0];

        // paramsToInsert[1] = length
        int length = paramsToDelete[1];

        // Loop through Map from offset to length, deleting each character in the text area
        for(int i = offset; i < length; i++){
            textArea.remove(i);
        }
    }

    public static void main(String[] args) {
        UserService.getInstance();

        EditorClient client = new EditorClient();
        client.setVisible(true);
    }
}

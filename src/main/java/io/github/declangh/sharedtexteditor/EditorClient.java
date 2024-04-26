package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.io.*;
import java.util.HashMap;

public class EditorClient extends JFrame {

    private JTextArea textArea;
    private JFileChooser fileChooser;
    private HashMap<Integer, String> textMap = new HashMap<>();

    Packets packets;

    int numOps = 0;

    public EditorClient(/*String serverURI*/) {
        setTitle("Simple Text Editor");
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

        //Make the text area
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event){
                String insertedText;

                try {
                    insertedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    //CALL UPDATE MAP HERE
                    addOperation(event.getOffset(), event.getLength(), insertedText);

                    //After doing the operation locally, get the packet to broadcast it out
                    byte[] insertPacket = packets.insertPacket(numOps, (short)event.getOffset(), (short)event.getLength(), insertedText);

                    //TODO: BROADCAST THE PACKET TO THE OTHER USERS IN THE CHANNEL
                    
                } catch (Exception e) {
                    // TODO: handle exception
                    insertedText = "";
                }
                System.out.println(textMap.entrySet());
                System.out.println(insertedText);
            }

            @Override
            public void removeUpdate(DocumentEvent event){
                //String removedText;
                int offset = event.getOffset();
                int length = event.getLength();

                //CALL UPDATE MAP HERE
                deleteOperation(offset, length);

                //After doing the operation locally, get the packet to broadcast it out
                if (packets!= null){
                    byte[] deletePacket = packets.deletePacket((short)numOps, (short)event.getOffset(), (short)event.getLength());
                }

                //TODO: BROADCAST THE PACKET TO THE OTHER USERS IN THE CHANNEL

                System.out.println("Offset " + offset + " Length " + length);
            }

            @Override
            public void changedUpdate(DocumentEvent event){
                String updatedText;

                try {
                    updatedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    //CALL UPDATE MAP HERE

                    //After doing the operation locally, get the packet to broadcast it out
                    //TODO - ADD FUNCTION FOR UPDATING TEXT IN THE MAP

                    //TODO: BROADCAST THE PACKET TO THE OTHER USERS IN THE CHANNEL
                    
                } catch (Exception e) {
                    // TODO: handle exception
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
    private void addOperation(int offset, int length, String newCharacter){
        char value;
        int key;
        //First check to see if there is a key for that offset
        if(textMap.containsKey(offset)){
            //Make a new map 
            HashMap<Integer, Character> tempMap = new HashMap<>();
            
            //Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
                
                //If i is less than the offset (the new chars position) then you can just put in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }
                //if i = the offset, put the new character at that position and the old character at the position + length
                else if(i == offset){
                    //Check if the length is greater than 1 
                    if(length > 1){
                        //If it is, we need a FOR loop to add each character in the string to the map and move the old characters to the new place 
                        for(int j = 0; j < length; j++){
                            tempMap.put(i + j, newCharacter.charAt(j));
                            value = textMap.get(i + j);
                            tempMap.put(i + length, value);
                        }
                    }
                    //Else just replace the one character
                    else{
                        tempMap.put(i, newCharacter.charAt(0));
                        value = textMap.get(i);
                        tempMap.put(i+1, value);
                    }
                }
                //Else, map to the new position (oldPosition + length)
                else{
                    key = i + length;
                    value = textMap.get(i);
                    tempMap.put(key, value);
                }
            }
            //Clear the old map and copy the new map
            textMap.clear();
            textMap.putAll(tempMap);
            System.out.println(tempMap.entrySet() + " temp map");
        }
        else{
            //Check to see the length of the new text
            if(length > 1){
                for(int i = 0; i < length; i++){
                    textMap.put(offset + i, newCharacter.charAt(i));
                }
            }
            else{
                //If there isnt, put the new character at that position
                textMap.put(offset, newCharacter.charAt(0));
            }

        }
    }

    //This is called when a delete occurs. 
    private void deleteOperation(int offset, int length){
        char value;
        int key; 

        //If the offset = size of the map
        if(offset == textMap.size()){
            //This means the the offset is the last character in the map, so no need to update others
            textMap.remove(offset);
        }
        //Else there are characters that need to be moved
        else{
            //Make a new map 
            HashMap<Integer, Character> tempMap = new HashMap<Integer, Character>();

            //Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
    
                //If i is less than the offset then you can just put it in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }
                //if i = the offset, then continue. We dont want that value
                else if(i == offset || i < offset + length){
                    continue;
                }
                //Else, map to the new position (oldPosition - length)
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

    public static void main(String[] args) throws Exception {
        // we'll keep this local for now. We can replace with a GitHub server or cs server later
        new EditorClient().setVisible(true);
        //new EditorClient("localhost:8080");
    }
}

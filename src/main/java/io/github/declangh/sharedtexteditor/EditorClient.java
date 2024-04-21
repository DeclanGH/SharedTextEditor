//package io.github.declangh.sharedtexteditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;

public class github extends JFrame {
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private HashMap<Integer, String> textMap = new HashMap<Integer, String>();
    // TODO: Add Nathan's Text Editor to the constructor below
    //public EditorClient(String serverURI){
    public EditorClient() {
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

        //Open a file on your computer  
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        //Save the file to your computer
        saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        //Exit the application
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //Make the text area
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event){
                String insertedText;

                try {
                    insertedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    //CALL UPDATE MAP HERE
                    addOperation(event.getOffset(), event.getLength(), insertedText);
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

                System.out.println("Offset " + offset + " Length " + length);
            }

            @Override
            public void changedUpdate(DocumentEvent event){
                String updatedText;

                try {
                    updatedText = event.getDocument().getText(event.getOffset(), event.getLength());
                    //CALL UPDATE MAP HERE
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

    public static void main(String[] args) throws Exception {
        // we'll keep this local for now. We can replace with a GitHub server or cs server later
        new EditorClient().setVisible(true);
        //new EditorClient("localhost:8080");
    }

   //This method is for updating the hashmap for which characters map to which position. 
    //This should be called after every update in the text editor.
    private void addOperation(int offset, int length, String newCharacter){
        String value;
        int key;
        //First check to see if there is a key for that offset
        if(textMap.containsKey(offset)){
            //Make a new map 
            HashMap<Integer, String> tempMap = new HashMap<Integer, String>();
            
            //Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
                
                //If i is less than the offset (the new chars position) then you can just put in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }
                //if i = the offset, put the new character at that position 
                else if(i == offset){
                    tempMap.put(i, newCharacter);
                }
                //Else, map to the new position (oldPosition + length)
                else{
                    key = i + length;
                    value = tempMap.get(i);
                    tempMap.put(key, value);
                }
            }
            //Clear the old map and copy the new map
            textMap.clear();
            textMap.putAll(tempMap);
            System.out.println(tempMap.entrySet() + " temp map");
        }
        else{
            //If there isnt, put the new character at that position
            textMap.put(offset, newCharacter);
        }
    }

    //This is called when a delete occurs. 
    private void deleteOperation(int offset, int length){
        String value;
        int key; 

        //If the offset = size of the map
        if(offset == textMap.size()){
            //This means the the offset is the last character in the map, so no need to update others
            textMap.remove(offset);
        }
        //Else there are characters that need to be moved
        else{
            //Make a new map 
            HashMap<Integer, String> tempMap = new HashMap<Integer, String>();

            //Make a new map and map each value up to the offset to the same position
            for(int i = 0; i < textMap.size(); i++){
    
                //If i is less than the offset then you can just put it in the new map
                if(i < offset){
                    value = textMap.get(i);
                    tempMap.put(i, value);
                }
                //if i = the offset, then continue. We dont want that value
                else if(i == offset){
                    continue;
                }
                //Else, map to the new position (oldPosition - length)
                else{
                    key = i - length;
                    value = tempMap.get(i);
                    tempMap.put(key, value);
                }
            }
            //Clear the old map and copy the new map
            textMap.clear();
            textMap.putAll(tempMap);
        }
    }
}

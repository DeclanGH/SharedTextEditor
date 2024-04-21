package io.github.declangh.sharedtexteditor;

import javax.swing.*;

public class EditorClient extends JFrame {

    // TODO: Add Nathan's Text Editor to the constructor below
    public EditorClient(String serverUri) {
        //
    }

    public static void main(String[] args) throws Exception {
        // we'll keep this local for now. We can replace with a GitHub server or cs server later
        new EditorClient("ws://localhost:8080/shared-text-editor");
    }
}
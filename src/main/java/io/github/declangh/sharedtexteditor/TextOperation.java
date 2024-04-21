package io.github.declangh.sharedtexteditor;

public class TextOperation {

    public enum Operation {
        INSERT,
        DELETE
    }

    private Operation operation;
    private char character;
    private int position;
    private long timestamp;

    public TextOperation(Operation operation, char character, int position, long timestamp) {
        this.operation = operation;
        this.character = character;
        this.position = position;
        this.timestamp = timestamp;
    }
}

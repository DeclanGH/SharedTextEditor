package io.github.declangh.sharedtexteditor;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketTextHandler {

    @MessageMapping("/edit")
    @SendTo("/SharedTextEditor/changes")
    public TextOperation broadcastChanges(TextOperation editOperation) {

        // Using TextOperation class for now. Might update
        return editOperation;
    }
}
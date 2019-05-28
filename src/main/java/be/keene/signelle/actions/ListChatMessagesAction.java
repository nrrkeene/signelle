package be.keene.signelle.actions;

import be.keene.signelle.net.HttpResponse;
import be.keene.signelle.SignelleException;
import be.keene.signelle.chat.ChatMessage;
import be.keene.signelle.chat.SignelleModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ListChatMessagesAction extends AbstractAction {
    private static Logger logger = Logger.getLogger("Signelle.Actions");
    
    @JsonProperty("id")
    String id;
    @JsonProperty("sourceUserId")
    long sourceUserId;
    @JsonProperty("destinationUserId")
    long destinationUserId;
    @JsonProperty("timestamp")
    long timestamp;
    @JsonProperty("message")
    String message;
    
    /**
     * Returns a ListChatMessagesAction for the given JSON string. If there is any error, logs it and returns null.
     *
     * @param json
     * @return
     */
    public static ListChatMessagesAction from(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            ListChatMessagesAction result = objectMapper.readValue(json, ListChatMessagesAction.class);
            return result;
        } catch (IOException e) {
            logger.error("Could not create a Create Chat Action from " + json, e);
            return null;
        }
    }
    
    @Override
    public String act(SignelleModel model) throws SignelleException {
        ChatMessage[] chatMessages = model.getMessages(sourceUserId, destinationUserId);
        String result = buildJsonForMessages(chatMessages);
        return result;
    }
    
    private String buildJsonForMessages(ChatMessage[] chatMessages) {
        if(chatMessages == null) {
            return "\"No chats for messages\"";
        }
        try {
            String result = new ObjectMapper().writeValueAsString(chatMessages);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert messages to JSON", e);
            return "\"Error 20\"";
        }
    }
}

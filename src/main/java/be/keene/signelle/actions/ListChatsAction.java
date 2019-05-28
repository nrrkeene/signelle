package be.keene.signelle.actions;

import be.keene.signelle.*;
import be.keene.signelle.chat.Chat;
import be.keene.signelle.chat.SignelleModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ListChatsAction extends AbstractAction {
    private static Logger logger = Logger.getLogger("Signelle.Actions");
    
    @JsonProperty("id")
    String id;
    @JsonProperty("participantIds")
    Long[] participantIds;
    
    
    public static ListChatsAction from(String payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            ListChatsAction result = objectMapper.readValue(payload, ListChatsAction.class);
            return result;
        } catch (IOException e) {
            logger.error("Could not create a Create Chat Action from " + payload, e);
            return null;
        }
    }
    
    @Override
    public String act(SignelleModel model) throws SignelleException {
        Chat[] chats = model.getUserChats(participantIds);
        String result = buildJsonForChats(chats);
        return result;
    }
    
    private String buildJsonForChats(Chat[] chats) {
        if(chats == null) {
            return "\"No chats\"";
        }
        try {
            String result = new ObjectMapper().writeValueAsString(chats);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert messages to JSON", e);
            return "\"Error 87\"";
        }
    }
}

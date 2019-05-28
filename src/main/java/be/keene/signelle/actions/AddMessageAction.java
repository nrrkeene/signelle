package be.keene.signelle.actions;

import be.keene.signelle.net.HttpRequest;
import be.keene.signelle.SignelleException;
import be.keene.signelle.chat.SignelleModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;

public class AddMessageAction extends AbstractAction {
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
    
    String chatId;
    
    
    public static AddMessageAction from(HttpRequest httpRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            AddMessageAction result = objectMapper.readValue(httpRequest.getPayload(), AddMessageAction.class);
            result.chatId = httpRequest.getResource().substring(7, httpRequest.getResource().length() - 9);
            return result;
        } catch (IOException e) {
            logger.error("Could not create a Create Chat Action from " + httpRequest.getPayload(), e);
            return null;
        }
    }
    
    @Override
    public String act(SignelleModel model) throws SignelleException {
        model.addMessage(chatId, sourceUserId, destinationUserId, timestamp, message);
        String result = buildJsonForMessage("Success");
        return result;
    }
    
    private String buildJsonForMessage(String message) {
        if(message == null) {
            return "\"Empty message\"";
        }
        try {
            String result = new ObjectMapper().writeValueAsString(message);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert messages to JSON", e);
            return "\"Error 20\"";
        }
    }
}

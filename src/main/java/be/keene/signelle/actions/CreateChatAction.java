package be.keene.signelle.actions;


import be.keene.signelle.net.HttpRequest;
import be.keene.signelle.net.HttpResponse;
import be.keene.signelle.SignelleException;
import be.keene.signelle.chat.SignelleModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;

public class CreateChatAction extends AbstractAction {
    private static Logger logger = Logger.getLogger("Signelle.Actions");
    
    @JsonProperty("id")
    String id;
    @JsonProperty("participantIds")
    Long[] participantIds;
    
    /**
     * Returns a CreateChatAction for the given JSON string. If there is any error, logs it and returns null.
     *
     * @param httpRequest
     * @return
     */
    public static CreateChatAction from(HttpRequest httpRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            CreateChatAction result = objectMapper.readValue(httpRequest.getPayload(), CreateChatAction.class);
            return result;
        } catch (IOException e) {
            logger.error("Could not create a Create Chat Action from " + httpRequest.getPayload(), e);
            return null;
        }
    }
    
    @Override
    public String act(SignelleModel model) throws SignelleException {
        String response = model.createChat(id, participantIds);
        String result = buildJsonForChatId(response);
        return result;
    }
    
    private String buildJsonForChatId(String message) {
        if(message == null) {
            return "\"Empty message\"";
        }
        try {
            String result = new ObjectMapper().writeValueAsString(message);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert messages to JSON", e);
            return "\"Error 15\"";
        }
    }
}

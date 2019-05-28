package be.keene.signelle.actions;

import be.keene.signelle.net.HttpRequest;
import be.keene.signelle.SignelleException;
import be.keene.signelle.chat.SignelleModel;
import org.apache.log4j.Logger;

/**
 * This class encapsulates the semantics of messages coming off of the wire including factory methods for creating AbstractAction objects
 * and a visitor method to implement the business logic of the appropriate type of message.
 */
public abstract class AbstractAction {
    private static Logger logger = Logger.getLogger("Signelle.Actions");
    
    
    
    public AbstractAction() {
    }
    
    /**
     * Returns an appropriate Action given the HTTP request. If there is any error this method logs it and returns null.
     *
     * @param httpRequest
     * @return
     */
    public static AbstractAction from(HttpRequest httpRequest) {
        if(httpRequest.getType() == HttpRequest.TYPE.POST) {
            String resource = httpRequest.getResource();
            if(resource.equalsIgnoreCase("/chats")) {
                return CreateChatAction.from(httpRequest);
            }
        
            else if(resource.startsWith("/chats/") && resource.endsWith("/messages")) {
                return AddMessageAction.from(httpRequest);
            }
        
            else {
                if (logger.isTraceEnabled()) logger.trace("No action for GET resource " + httpRequest.getResource());
                return null;
            }
        }
        
        else if(httpRequest.getType() == HttpRequest.TYPE.GET) {
            String resource = httpRequest.getResource();
            if(resource.equalsIgnoreCase("/chats")) {
                return ListChatsAction.from(httpRequest);
            }
    
            else if(resource.startsWith("/chats/") && resource.endsWith("/messages")) {
                return ListChatMessagesAction.from(httpRequest);
            }
    
            else {
                if (logger.isTraceEnabled()) logger.trace("No action for GET resource " + httpRequest.getResource());
                return null;
            }
        }
        
        else {
            if (logger.isTraceEnabled()) logger.trace("No action for request type " + httpRequest.getType());
            return null;
        }
    }
    
    public abstract String act(SignelleModel model) throws SignelleException;
}


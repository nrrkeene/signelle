package be.keene.signelle.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    final long sourceUserId;
    final long destinationUserId;
    final long timestamp;
    final String message;
    
    public ChatMessage(long sourceUserId, long destinationUserId, long timestamp, String message) {
        this.sourceUserId = sourceUserId;
        this.destinationUserId = destinationUserId;
        this.timestamp = timestamp;
        this.message = message;
    }
    
    public long getSourceUserId() {
        return sourceUserId;
    }
    
    public long getDestinationUserId() {
        return destinationUserId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Long[] getParticipants() {
        return new Long[] { sourceUserId, destinationUserId };
    }
}

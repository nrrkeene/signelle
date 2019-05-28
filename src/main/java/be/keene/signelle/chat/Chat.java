package be.keene.signelle.chat;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Signelle logical chat.
 */
public class Chat {
    long id;
    List<Long> participantIds;
    
    ConcurrentLinkedQueue<ChatMessage> messages = new ConcurrentLinkedQueue<>();
    
    public ChatMessage[] getMessages() {
        ChatMessage[] result = new ChatMessage[] {};
        messages.toArray(result);
        return result;
    }
    
    public Chat(long id, Long[] participantIds) {
        this.id = id;
        this.participantIds = Arrays.asList(participantIds);
    }
    
    public synchronized void add(ChatMessage chatMessage) {
        // Messages may arrive out of order
        // In general we expect messages to arrive approximately close to order, so the correct insertion point
        // should be close to the end. Pop them off one list into a temp stack, insert, then pop them back.
        Stack<ChatMessage> afterMessages = new Stack<>();
        while(this.messages.peek() != null && this.messages.peek().timestamp > chatMessage.timestamp) {
            afterMessages.push(this.messages.poll());
        }
        this.messages.add(chatMessage);
        while(!afterMessages.isEmpty()) {
            this.messages.add(afterMessages.pop());
        }
    }
}

package be.keene.signelle.chat;

import be.keene.signelle.SignelleException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class SignelleModel {
    private static Logger logger = Logger.getLogger("Signelle.Model");
    
    private final UserContacts contacts;
    private final UserChats chats;
    private final SecurityManager security;
    
    public SignelleModel() {
        this.contacts = new UserContacts();
        this.chats = new UserChats();
        this.security = new SecurityManager();
    }
    
    /**
     * Returns an ordered list of chat messages between the two users. If the users have never exchanged messages, returns an empty array.
     *
     * @param sourceUserId
     * @param destinationUserId
     * @return
     */
    public ChatMessage[] getMessages(long sourceUserId, long destinationUserId) {
        Chat[] chatsForTheseParticipants = chats.get(sourceUserId, destinationUserId);
    
        if(chatsForTheseParticipants == null) {
            if(logger.isTraceEnabled()) logger.trace("No chat found for " + sourceUserId + " and " + destinationUserId);
            return null;
        }
        if(chatsForTheseParticipants.length == 0) {
            if(logger.isTraceEnabled()) logger.trace("Empty list of chats found for " + sourceUserId + " and " + destinationUserId);
            return null;
        }
        if(chatsForTheseParticipants.length > 1) {
            logger.warn("Today Signelle only supports one chat between users, so picking the first one, but in the future a chat message will include an explicit chat reference to do away with this ambiguity");
        }
        Chat chat = chatsForTheseParticipants[0];
        ChatMessage[] result = chat.getMessages();
        return result;
    }
    
    /**
     * Returns an array of Chats active for the user. If the user has no active chats, returns an empty array.
     *
     * @param sourceUserId
     * @return
     */
    public Chat[] getUserChats(Long[] sourceUserId) {
        return this.chats.get(sourceUserId);
    }
    
    /**
     *
     * @param participantIds
     * @return
     */
    public String createChat(String id, Long[] participantIds) {
        String result = chats.add(id, participantIds);
        return result;
    }
    
    public String createChat(String id, long sourceUserId, long destinationUserId) {
        return createChat(id, new Long[] {sourceUserId, destinationUserId });
    }
    
//    /**
//     *
//     * @param chatMessage
//     */
//    public void addMessage(ChatMessage chatMessage) {
//        Chat[] chatsForTheseParticipants = this.chats.get(chatMessage.getParticipants());
//        if(chatsForTheseParticipants == null) {
//            if(logger.isTraceEnabled()) logger.trace("No chat found for " + chatMessage);
//            return;
//        }
//        if(chatsForTheseParticipants.length == 0) {
//            logger.warn("Unexpected empty list of chats");
//        }
//        if(chatsForTheseParticipants.length > 1) {
//            logger.warn("Today Signelle only supports one chat between users, so picking the first one, but in the future a chat message will include an explicit chat reference to do away with this ambiguity");
//        }
//        Chat chat = chatsForTheseParticipants[0];
//        chat.add(chatMessage);
//    }
    
    public String addMessage(String chatId, long sourceUserId, long destinationUserId, long timestamp, String message) {
        Chat chat = this.chats.getChat(chatId);
        if(chat == null) {
            if(logger.isTraceEnabled()) logger.trace("No chat found for " + message);
            return "No chat found for " + chatId;
        }
        ChatMessage chatMessage = new ChatMessage(sourceUserId, destinationUserId, timestamp, message);
        chat.add(chatMessage);
        return "Message added to chat " + chatId;
    }
    
    /**
     * This class encapsulates the logical contact data for the whole system.
     */
    class UserContacts {
        private final Map<String, Set<Integer>> contacts;
    
        public UserContacts() {
            Map<String, Set<Integer>> contactsValue;
            try {
                // convert the entire contacts input file to a map
                InputStream inputStream = getClass().getResourceAsStream("/contacts.json");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String entireContactsFileJson = bufferedReader.lines().collect(Collectors.joining("\n"));
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                try {
                    contactsValue = objectMapper.readValue(entireContactsFileJson, new TypeReference<Map<String, Set<Integer>>>(){});
                } catch (IOException e) {
                    logger.error("Could not render contacts into JSON ", e);
                    contactsValue = new HashMap<>();
                }
            } catch (Exception e) {
                logger.error("Cannot read contacts file", e);
                contactsValue = new HashMap<>();
            }
            this.contacts = contactsValue;
        }
    
        public boolean usersKnowEachOther(long participantId, long otherParticipant) {
            return
                    this.contacts.containsKey(participantId)
                    && this.contacts.containsKey(otherParticipant)
                    && this.contacts.get(participantId).contains(otherParticipant)
                    && this.contacts.get(otherParticipant).contains(participantId);
        }
    }
    
    /**
     * This class encapsulates and hides the logic of a data structure used to store chats related to two users.
     *
     * Chats are stored in a thread-safe map which uses a String key. Multiple keys are used to refer to the same
     * chat in order to make lookups fast in multiple contexts. Keys can be arbitrary such as the chat ID which is
     * requested by the client. Keys can be a hash of userIDs which identifies the people in the chat. Keys can
     * be individual user IDs so as to find all of a person's chats.
     *
     * Each key refers to a set of chats, not just one, because one user can have multiple chats, right? And two
     * users could have different chats open with one another, right? Using keys this way allows the use of a single
     * container for the chats but the tradeoff is the weird situation that a single arbitrary ChatID could refer
     * to more than one chat, which is illogical. Prevent that using code.
     */
    private class UserChats {
        ConcurrentHashMap<String, ConcurrentLinkedDeque<Chat>> allChats = new ConcurrentHashMap<>();
        
        public String add(String id, Long[] chatParticipantIds) {
            if(!security.allowCreateApp(chatParticipantIds )) {
                if (logger.isTraceEnabled()) logger.trace("Cannot create chat because users don't know each other: " + chatParticipantIds);
                return "Cannot create chat because users don't know each other: " + chatParticipantIds;
            }
            Chat chat = new Chat(0, chatParticipantIds);
            
            // for any chat between N participants, there are N+2 keys for looking it up. This allows us to look up the
            // chat by any user, or by all users, or by ID.
            
            // by any user
            for(Long chatParticipantId: chatParticipantIds) {
                ConcurrentLinkedDeque<Chat> userChats = allChats.get(chatParticipantId);
                if(userChats == null) {
                    userChats = new ConcurrentLinkedDeque<>();
                    allChats.put(renderKey(chatParticipantId), userChats);
                }
                userChats.add(chat);
            }
            
            // by all users
            ConcurrentLinkedDeque<Chat> userChats = allChats.get(renderKey(chatParticipantIds));
            if(userChats == null) {
                userChats = new ConcurrentLinkedDeque<>();
                allChats.put(renderKey(chatParticipantIds), userChats);
            }
            userChats.add(chat);
            
            // by ID
            userChats = allChats.get(id);
            if(userChats == null) {
                userChats = new ConcurrentLinkedDeque<>();
                allChats.put(renderKey(chatParticipantIds), userChats);
            }
            userChats.add(chat);
            
            return "Created chat " + id;
        }
        
        public Chat[] get(Long userId1, Long userId2) {
            return get(new Long[] { userId1, userId2 });
        }
        
        public Chat[] get(Long[] participantIds) {
            ConcurrentLinkedDeque<Chat> participantChats = allChats.get(renderKey(participantIds));
            if(participantChats == null) return null;
            Chat[] result = new Chat[participantChats.size()];
            result = participantChats.toArray(result);
            return result;
        }
    
        /**
         * This method would be a way to look up multiple chats for one user but isn't used.
         *
         * @param userId
         * @return
         */
        public Chat[] get(Long userId) {
            return get(new Long[] { userId });
        }
    
        public Chat getChat(String chatId) {
            ConcurrentLinkedDeque<Chat> shouldBeOneChat = allChats.get(chatId);
            return shouldBeOneChat == null ? null : shouldBeOneChat.getFirst();
        }
    
        private String renderKey(Long userId) {
            return String.valueOf(userId);
        }
        
        private String renderKey(Long[] participantIds) {
            Arrays.sort(participantIds);
            StringBuilder stringBuilder = new StringBuilder();
            for(Long participantId: participantIds) {
                stringBuilder.append(participantId);
            }
            String result = stringBuilder.toString();
            return result;
        }
    }
    
    /**
     * This class encapsulates any authentication and authorization for the model and related business logic.
     */
    private class SecurityManager {
        boolean allowCreateApp(Long[] participantIds) {
            // check that users are allowed to create this chat
            for(long participantId: participantIds) {
                for(long otherParticipant: participantIds) {
                    if(participantId != otherParticipant && contacts.usersKnowEachOther(participantId, otherParticipant)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}

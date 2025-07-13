package co.teamsphere.api.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.teamsphere.api.exception.ChatException;
import co.teamsphere.api.exception.MessageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.Chat;
import co.teamsphere.api.models.Messages;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.MessageRepository;
import co.teamsphere.api.request.SendMessageRequest;
import co.teamsphere.api.services.ChatService;
import co.teamsphere.api.services.MessageService;
import co.teamsphere.api.services.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepo;

    private final UserService userService;

    private final ChatService chatService;

    public MessageServiceImpl(MessageRepository messageRepo, UserService userService, ChatService chatService) {
        this.messageRepo = messageRepo;
        this.userService = userService;
        this.chatService = chatService;
    }

    @Override
    @Transactional
    public Messages sendMessage(SendMessageRequest req) throws UserException, ChatException {
        log.info("Attempting to send a message");

        try {
            User user = userService.findUserById(req.getUserId());
            log.info("Found user for sending message: {}", user);

            Chat chat = chatService.findChatById(req.getChatId());
            log.info("Found chat for sending message: {}", chat);

            if (chat.getUsers().stream().noneMatch(u -> u.getId().equals(user.getId()))) {
                log.error("User {} is not part of chat {}", user.getId(), chat.getId());
                throw new UserException("User is not part of the chat");
            }

            Messages messages = Messages.builder()
                .chat(chat)
                .username(user)
                .content(req.getContent())
                .timeStamp(LocalDateTime.now())
                .isRead(false)
                .build();

            log.info("Creating and saving the message: {}", messages);

            return messageRepo.save(messages);
        } catch (UserException | ChatException e) {
            log.error("Error sending message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending message", e);
            throw new ChatException("Error sending message" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId, UUID reqUserId) throws MessageException {
        log.info("Attempting to delete message with ID: {}", messageId);

        try {
            Messages messages = messageRepo.findById(messageId).orElseThrow(() -> new MessageException("Message not found with ID: " + messageId));
            log.info("Found message for deletion: {}", messages);

            if (!messages.getUsername().getId().equals(reqUserId) || (messages.getChat().getIsGroup() && messages.getChat().getAdmins().stream().noneMatch(u -> u.getId().equals(reqUserId)))) {
                log.error("User {} is not part of the chat", reqUserId);
                throw new MessageException("User is not part of the chat");
            }

            messageRepo.deleteById(messages.getId());

            log.info("Message deleted successfully");
        } catch (MessageException e) {
            log.error("Error deleting message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting message", e);
            throw new MessageException("Error deleting message: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Messages> getChatsMessages(UUID chatId, UUID reqUserId) throws ChatException {
        log.info("Attempting to retrieve messages for chat with ID: {}", chatId);
        try {
            Chat chat = chatService.findChatById(chatId);
            log.info("Found chat for retrieving messages: {}", chat);

            if (chat.getUsers().stream().noneMatch(u -> u.getId().equals(reqUserId))) {
                log.error("User {} is not part of chat {}", reqUserId, chatId);
                throw new ChatException("User is not part of the chat");
            }

            List<Messages> messages = messageRepo.findMessageByChatId(chatId);
            log.info("Retrieved {} messages for chat with ID: {}", messages.size(), chatId);

            return messages;
        } catch (ChatException e) {
            log.error("Error retrieving messages: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving messages", e);
            throw new ChatException("Error retrieving messages: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Messages findMessageById(UUID messageId) throws MessageException {
        log.info("Attempting to find message by ID: {}", messageId);

        try {
            Optional<Messages> optionalMessage = messageRepo.findById(messageId);

            if (optionalMessage.isPresent()) {
                Messages messages = optionalMessage.get();
                log.info("Found message for ID {}: {}", messageId, messages);
                return messages;
            }

            log.error("Message with ID {} not found. Unable to retrieve.", messageId);
            throw new MessageException("Message not found with ID: " + messageId);
        } catch (MessageException e) {
            log.error("Error finding message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error finding message", e);
            throw new MessageException("Error finding message: " + e.getMessage());
        }
    }

}

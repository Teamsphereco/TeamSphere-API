package co.teamsphere.api.services;

import co.teamsphere.api.exception.ChatException;
import co.teamsphere.api.exception.MessageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.Messages;
import co.teamsphere.api.request.SendMessageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface MessageService {

    Messages sendMessage(SendMessageRequest req) throws UserException, ChatException;

    List<Messages> getChatsMessages(UUID chatId, UUID reqUserId) throws ChatException;

    Messages findMessageById(UUID messageId) throws MessageException;

    void deleteMessage(UUID messageId, UUID reqUserId) throws MessageException;
}

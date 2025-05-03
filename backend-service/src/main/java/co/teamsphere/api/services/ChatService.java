package co.teamsphere.api.services;

import co.teamsphere.api.DTO.ChatSummaryDTO;
import co.teamsphere.api.exception.ChatException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.Chat;
import co.teamsphere.api.models.User;
import co.teamsphere.api.request.GroupChatRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ChatService{

    Chat createChat(UUID reqUserId, UUID userId2, boolean isGroup) throws UserException;

    Chat findChatById(UUID chatId) throws ChatException;

    Chat createGroup(GroupChatRequest req, UUID reqUerId) throws UserException;

    Chat addUserToGroup(UUID userId, UUID chatId, User reqUserId) throws UserException, ChatException;

    Chat renameGroup(UUID chatId, String groupName, UUID reqUserId) throws ChatException, UserException;

    Chat removeFromGroup(UUID chatId, UUID userId, UUID reqUser) throws UserException,ChatException;

    Chat deleteChat(UUID chatId, UUID userId) throws ChatException, UserException;

    List<ChatSummaryDTO> getChatSummaries(UUID userId, int page, int size) throws ChatException;

}

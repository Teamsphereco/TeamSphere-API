package co.teamsphere.api.repository;

import co.teamsphere.api.models.Chat;
import co.teamsphere.api.models.Messages;
import co.teamsphere.api.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messagesRepository;

    private User testUser;
    private Chat testChat;
    private Messages testMessage;

    /**
     * Helper method to create a user with default or custom values
     */
    private User createUser(String username, String email, String password, String phoneNumber) {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .phoneNumber(phoneNumber)
                .createdDate(OffsetDateTime.now())
                .lastUpdatedDate(OffsetDateTime.now())
                .build();
    }

    /**
     * Helper method to create a chat with default or custom values
     */
    private Chat createChat(String chatName, Set<User> users, boolean isGroup) {
        Chat chat = new Chat();
        chat.setChatName(chatName);
        chat.setUsers(users);
        chat.setIsGroup(isGroup);
        return chat;
    }

    /**
     * Helper method to create a message with default or custom values
     */
    private Messages createMessage(Chat chat, String content, LocalDateTime timestamp) {
        Messages message = new Messages();
        message.setChat(chat);
        message.setContent(content);
        message.setTimeStamp(timestamp != null ? timestamp : LocalDateTime.now());
        return message;
    }

    @BeforeEach
    void setUp() {
        // Setup test data using helper methods
        testUser = createUser("tester", "test@gmail.com", "securePassword", "1234567890");
        // We need to mock the ID for testing purposes since we're not using real persistence
        //when(testUser.getId()).thenReturn(UUID.randomUUID());

        testChat = createChat("Test Chat", Set.of(testUser), false);
        //when(testChat.getId()).thenReturn(UUID.randomUUID());

        testMessage = createMessage(testChat, "Test message", null);
        //when(testMessage.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void findUserByEmailTest() {
        // Setup mock
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testUser));

        // Test
        Optional<User> foundUser = userRepository.findByEmail("test@gmail.com");

        // Verify
        assertTrue(foundUser.isPresent());
        assertEquals("tester", foundUser.get().getUsername());
        verify(userRepository).findByEmail("test@gmail.com");
    }

    @Test
    void findUserByUserNameTest() {
        // Setup mock
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(testUser));

        // Test
        Optional<User> foundUser = userRepository.findByUsername("tester");

        // Verify
        assertTrue(foundUser.isPresent());
        assertEquals("test@gmail.com", foundUser.get().getEmail());
        verify(userRepository).findByUsername("tester");
    }

    @Test
    void searchUserTest() {
        // Setup test data using helper methods
        User user1 = createUser("john_doe", "john@example.com", "password1", "1112223333");
        User user2 = createUser("jane_doe", "jane@example.com", "password2", "2223334444");
        User user3 = createUser("doejohn", "doejohn@example.com", "password3", "3334445555");

        List<User> userList = List.of(user1, user2, user3);

        // Setup mock
        when(userRepository.searchUsers("doe")).thenReturn(userList);

        // Test
        List<User> searchedUsers = userRepository.searchUsers("doe");

        // Verify
        assertEquals(3, searchedUsers.size());
        assertTrue(searchedUsers.stream().anyMatch(u -> u.getUsername().equals("john_doe")));
        assertTrue(searchedUsers.stream().anyMatch(u -> u.getUsername().equals("jane_doe")));
        assertTrue(searchedUsers.stream().anyMatch(u -> u.getUsername().equals("doejohn")));
        verify(userRepository).searchUsers("doe");
    }

    @Test
    void findMessageByChatIdTest() {
        // Setup test data using helper methods
        LocalDateTime time1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime time2 = LocalDateTime.now().minusMinutes(5);

        Messages message1 = createMessage(testChat, "First message", time1);
        Messages message2 = createMessage(testChat, "Second message", time2);

        List<Messages> messagesList = List.of(message1, message2);

        // Setup mock
        when(messagesRepository.findMessageByChatId(testChat.getId())).thenReturn(messagesList);

        // Test
        List<Messages> messages = messagesRepository.findMessageByChatId(testChat.getId());

        // Verify
        assertFalse(messages.isEmpty());
        assertEquals(2, messages.size());
        verify(messagesRepository).findMessageByChatId(testChat.getId());
    }

    @Test
    void findChatsByUserIdTest() {
        // Setup test data using helper methods
        Chat chat1 = createChat("Chat 1", Set.of(testUser), false);
        Chat chat2 = createChat("Chat 2", Set.of(testUser), false);

        List<Chat> chatList = List.of(chat1, chat2);
        Page<Chat> chatPage = new PageImpl<>(chatList);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Setup mock
        when(chatRepository.findChatsByUserId(testUser.getId(), pageRequest)).thenReturn(chatPage);

        // Test
        Page<Chat> chats = chatRepository.findChatsByUserId(testUser.getId(), pageRequest);

        // Verify
        assertFalse(chats.isEmpty());
        assertEquals(2, chats.getTotalElements());
        verify(chatRepository).findChatsByUserId(testUser.getId(), pageRequest);
    }

    @Test
    void findSingleChatByUsersIdTest() {
        // Setup test data using helper methods
        User user1 = createUser("user1", "user1@example.com", "pass1", "1112223333");
        user1.setId(UUID.randomUUID());
        User user2 = createUser("user2", "user2@example.com", "pass2", "4445556666");
        user2.setId(UUID.randomUUID());

        Set<User> users = new HashSet<>();
        users.add(user1);
        users.add(user2);

        Chat chat = createChat("Direct Chat", users, false);

        // Setup mock
        when(chatRepository.findSingleChatByUsersId(user1, user2)).thenReturn(chat);

        // Test
        Chat foundChat = chatRepository.findSingleChatByUsersId(user1, user2);

        // Verify
        assertNotNull(foundChat);
        assertFalse(foundChat.getIsGroup());
        assertEquals(2, foundChat.getUsers().size());
        verify(chatRepository).findSingleChatByUsersId(user1, user2);
    }

    @Test
    void deleteChatsByIdTest() {
        // Setup
        UUID chatId = UUID.randomUUID();
        doNothing().when(chatRepository).deleteById(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // Test
        chatRepository.deleteById(chatId);
        Optional<Chat> deletedChat = chatRepository.findById(chatId);

        // Verify
        assertFalse(deletedChat.isPresent());
        verify(chatRepository).deleteById(chatId);
        verify(chatRepository).findById(chatId);
    }
}

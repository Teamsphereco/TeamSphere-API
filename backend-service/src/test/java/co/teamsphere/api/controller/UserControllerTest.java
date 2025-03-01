package co.teamsphere.api.controller;

import co.teamsphere.api.DTO.UserDTO;
import co.teamsphere.api.DTOmapper.UserDTOMapper;
import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.helpers.TestDataBuilder;
import co.teamsphere.api.models.User;
import co.teamsphere.api.request.UpdateUserRequest;
import co.teamsphere.api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserDTOMapper userDTOMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserDTO testUserDTO;
    private UUID userId;

    @BeforeEach
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
        userId = UUID.randomUUID();
        testUser = TestDataBuilder.buildUser("testUser", "https://example.com/profiles/abc123/public", "password123", "test@example.com");
        testUser.setId(userId);
        
        testUserDTO = new UserDTO();
        testUserDTO.setId(userId);
        testUserDTO.setUsername("testUser");
        testUserDTO.setProfilePicture("https://example.com/profiles/abc123/public");
        testUserDTO.setEmail("test@example.com");
    }

    @Test
    void updateUserHandler_Success() throws UserException, ProfileImageException {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(testUser);
        when(userDTOMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.updateUserHandler(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testUserDTO);
        
        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        verify(userDTOMapper).toUserDTO(testUser);
    }

    @Test
    void updateUserHandler_ThrowsException() throws ProfileImageException, UserException {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> userController.updateUserHandler(request, userId))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Error during update user process");
        
        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        verify(userDTOMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    void getUserProfileHandler_Success() {
        // Arrange
        String jwt = "Bearer valid.jwt.token";
        
        when(userService.findUserProfile(jwt)).thenReturn(testUser);
        when(userDTOMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.getUserProfileHandler(jwt);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(testUserDTO);
        
        verify(userService).findUserProfile(jwt);
        verify(userDTOMapper).toUserDTO(testUser);
    }

    @Test
    void getUserProfileHandler_ThrowsException() {
        // Arrange
        String jwt = "Bearer invalid.jwt.token";
        
        when(userService.findUserProfile(jwt)).thenThrow(new RuntimeException("Invalid token"));

        // Act & Assert
        assertThatThrownBy(() -> userController.getUserProfileHandler(jwt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error during get user profile process");
        
        verify(userService).findUserProfile(jwt);
        verify(userDTOMapper, never()).toUserDTO(any(User.class));
    }

    @Test
    void searchUsersByName_Success() {
        // Arrange
        String name = "test";
        List<User> users = List.of(testUser);
        HashSet<UserDTO> userDTOs = new HashSet<>();
        userDTOs.add(testUserDTO);
        
        when(userService.searchUser(name)).thenReturn(users);
        when(userDTOMapper.toUserDtos(any())).thenReturn(userDTOs);

        // Act
        ResponseEntity<HashSet<UserDTO>> response = userController.searchUsersByName(name);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(userDTOs);
        
        verify(userService).searchUser(name);
        verify(userDTOMapper).toUserDtos(any());
    }

    @Test
    void searchUsersByName_ThrowsException() {
        // Arrange
        String name = "test";
        
        when(userService.searchUser(name)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> userController.searchUsersByName(name))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error during search users process");
        
        verify(userService).searchUser(name);
        verify(userDTOMapper, never()).toUserDtos(any());
    }
}
package co.teamsphere.api.services.impl;

import co.teamsphere.api.config.JWTTokenProvider;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.helpers.TestDataBuilder;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.UserRepository;
import co.teamsphere.api.request.UpdateUserRequest;
import co.teamsphere.api.response.CloudflareApiResponse;
import co.teamsphere.api.services.CloudflareApiService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTTokenProvider jwtTokenProvider;

    @Mock
    private CloudflareApiService cloudflareApiService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = TestDataBuilder.buildUser("testUser", "https://example.com/profiles/abc123/public", "password123", "test@example.com");
        testUser.setId(userId);
    }

    @Test
    void findUserById_WhenUserExists_ReturnsUser() throws UserException {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.findUserById(userId);

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(userId);
        assertThat(foundUser.getUsername()).isEqualTo("testUser");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void findUserById_WhenUserDoesNotExist_ThrowsUserException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("user doesnt exist with the id: " + userId);
        
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void findUserProfile_WhenUserExists_ReturnsUser() {
        // Arrange
        String jwt = "valid.jwt.token";
        String email = "test@example.com";
        
        when(jwtTokenProvider.getEmailFromToken(jwt)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.findUserProfile(jwt);

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(email);
        verify(jwtTokenProvider, times(1)).getEmailFromToken(jwt);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void findUserProfile_WhenUserDoesNotExist_ThrowsBadCredentialsException() {
        // Arrange
        String jwt = "valid.jwt.token";
        String email = "test@example.com";
        
        when(jwtTokenProvider.getEmailFromToken(jwt)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findUserProfile(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Received invalid token!");
        
        verify(jwtTokenProvider, times(1)).getEmailFromToken(jwt);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void searchUser_WhenUsersFound_ReturnsUserList() {
        // Arrange
        String query = "test";
        List<User> expectedUsers = List.of(testUser);
        
        when(userRepository.searchUsers(query)).thenReturn(expectedUsers);

        // Act
        List<User> foundUsers = userService.searchUser(query);

        // Assert
        assertThat(foundUsers).isNotNull();
        assertThat(foundUsers.size()).isEqualTo(1);
        assertThat(foundUsers.get(0).getUsername()).isEqualTo("testUser");
        verify(userRepository, times(1)).searchUsers(query);
    }

    @Test
    void searchUser_WhenNoUsersFound_ReturnsEmptyList() {
        // Arrange
        String query = "nonexistent";
        
        when(userRepository.searchUsers(query)).thenReturn(Collections.emptyList());

        // Act
        List<User> foundUsers = userService.searchUser(query);

        // Assert
        assertThat(foundUsers).isNotNull();
        assertThat(foundUsers).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
        verify(userRepository, times(1)).searchUsers(query);
    }

    @Test
    void updateUser_WithUsernameOnly_UpdatesUsername() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User updatedUser = userService.updateUser(userId, request);

        // Assert
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getUsername()).isEqualTo("newUsername");
        assertThat(updatedUser.getProfilePicture()).isEqualTo(testUser.getProfilePicture());
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newUsername");
        assertThat(savedUser.getLastUpdatedDate()).isNotNull();
    }

    @Test
    void updateUser_WithProfilePicture_UpdatesProfilePicture() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testUser");
        MockMultipartFile profilePicture = new MockMultipartFile(
                "profile_picture", 
                "test.jpg",
                "image/jpeg", 
                "test image content".getBytes()
        );
        request.setProfile_picture(profilePicture);
        
        CloudflareApiResponse deleteResponse = new CloudflareApiResponse();
        deleteResponse.setSuccess(true);
        
        CloudflareApiResponse.Result uploadResult = new CloudflareApiResponse.Result();
        uploadResult.setVariants(List.of("https://example.com/profiles/xyz789/variant"));
        
        CloudflareApiResponse uploadResponse = new CloudflareApiResponse();
        uploadResponse.setSuccess(true);
        uploadResponse.setResult(uploadResult);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cloudflareApiService.deleteImage("abc123")).thenReturn(deleteResponse);
        when(cloudflareApiService.uploadImage(profilePicture)).thenReturn(uploadResponse);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User updatedUser = userService.updateUser(userId, request);

        // Assert
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getProfilePicture()).isEqualTo("https://example.com/profiles/xyz789/public");
        
        verify(cloudflareApiService).deleteImage("abc123");
        verify(cloudflareApiService).uploadImage(profilePicture);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_UserNotFound_ThrowsUserException() throws UserException {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Error updating user: user doesnt exist with the id");
        
        verify(userRepository, never()).save(any(User.class));
    }

    private MultipartFile mockMultipartFile() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getContentType()).thenReturn("image/png");

        return file;
    }

    @Test
    void updateUserThrowsExceptionWhenProfileUploadFails() throws IOException {
        // Setup user repository mock
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Create mock file with actual content
        MultipartFile mockFile = mockMultipartFile();

        // Setup request
        UpdateUserRequest request = new UpdateUserRequest();
        request.setProfile_picture(mockFile);
        request.setUsername("random");

        // Setup Cloudflare API mock responses
        CloudflareApiResponse mockDeleteResponse = new CloudflareApiResponse();
        mockDeleteResponse.setSuccess(true);

        CloudflareApiResponse mockUploadResponse = new CloudflareApiResponse();
        mockUploadResponse.setSuccess(false);
        mockUploadResponse.setErrors(List.of("Upload failed"));

        // Mock cloudflare service behavior
        when(cloudflareApiService.deleteImage(anyString())).thenReturn(mockDeleteResponse);
        when(cloudflareApiService.uploadImage(any(MultipartFile.class))).thenReturn(mockUploadResponse);

        // Assert that UserException is thrown
        assertThrows(UserException.class, () -> userService.updateUser(testUser.getId(), request));

        // Verify interactions (optional but recommended)
        verify(cloudflareApiService).uploadImage(any(MultipartFile.class));
        verify(userRepository).findById(testUser.getId());
    }
}
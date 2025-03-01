package co.teamsphere.api.services.impl;

import co.teamsphere.api.config.JWTTokenProvider;
import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.UserRepository;
import co.teamsphere.api.request.SignupRequest;
import co.teamsphere.api.response.AuthResponse;
import co.teamsphere.api.response.CloudflareApiResponse;
import co.teamsphere.api.services.CloudflareApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private CloudflareApiService cloudflareApiService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private SignupRequest validSignupRequest;
    private MockMultipartFile validProfilePicture;
    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Set up valid profile picture
        validProfilePicture = new MockMultipartFile(
                "profile_picture",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Set up valid signup request
        validSignupRequest = new SignupRequest();
        validSignupRequest.setEmail("test@example.com");
        validSignupRequest.setUsername("testuser");
        validSignupRequest.setPassword("Password123");
        validSignupRequest.setFile(validProfilePicture);

        // Set up test user
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword")
                .profilePicture("https://example.com/profiles/abc123/public")
                .build();
        
        // Set up UserDetails
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void isEmailInvalid_WithValidEmail_ReturnsFalse() {
        // Test with various valid email formats
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user@example.com")).isFalse();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("first.last@example.co.uk")).isFalse();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user-name@domain.com")).isFalse();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user_name@domain.com")).isFalse();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user123@domain.com")).isFalse();
    }

    @Test
    void isEmailInvalid_WithInvalidEmail_ReturnsTrue() {
        // Test with various invalid email formats
        assertThat(AuthenticationServiceImpl.isEmailInvalid("")).isTrue();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user@")).isTrue();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user@domain")).isTrue();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("user.domain.com")).isTrue();
        assertThat(AuthenticationServiceImpl.isEmailInvalid("@domain.com")).isTrue();
    }

    @Test
    void signupUser_WithValidRequest_ReturnsAuthResponse() throws Exception {
        // Arrange
        CloudflareApiResponse.Result result = new CloudflareApiResponse.Result();
        result.setVariants(List.of("https://example.com/profiles/abc123/variant"));
        
        CloudflareApiResponse cloudflareResponse = new CloudflareApiResponse();
        cloudflareResponse.setSuccess(true);
        cloudflareResponse.setResult(result);
        
        when(userRepository.findByEmail(validSignupRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(validSignupRequest.getUsername())).thenReturn(Optional.empty());
        when(cloudflareApiService.uploadImage(validSignupRequest.getFile())).thenReturn(cloudflareResponse);
        when(passwordEncoder.encode(validSignupRequest.getPassword())).thenReturn("encodedPassword");
        when(jwtTokenProvider.generateJwtToken(any(Authentication.class))).thenReturn("jwt.token.here");
        
        // Act
        AuthResponse response = authenticationService.signupUser(validSignupRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJwt()).isEqualTo("jwt.token.here");
        assertThat(response.isStatus()).isTrue();
        
        // Verify user was saved with correct data
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertThat(savedUser.getEmail()).isEqualTo(validSignupRequest.getEmail());
        assertThat(savedUser.getUsername()).isEqualTo(validSignupRequest.getUsername());
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getProfilePicture()).isEqualTo("https://example.com/profiles/abc123/public");
        assertThat(savedUser.getCreatedDate()).isNotNull();
        assertThat(savedUser.getLastUpdatedDate()).isNotNull();
    }

    @Test
    void signupUser_WithExistingEmail_ThrowsUserException() {
        // Arrange
        when(userRepository.findByEmail(validSignupRequest.getEmail())).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.signupUser(validSignupRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Email is already used with another account");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupUser_WithExistingUsername_ThrowsUserException() {
        // Arrange
        when(userRepository.findByEmail(validSignupRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(validSignupRequest.getUsername())).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.signupUser(validSignupRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Username is already used with another account");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupUser_WithInvalidFileType_ThrowsProfileImageException() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
                "profile_picture",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
        
        SignupRequest requestWithInvalidFile = new SignupRequest();
        requestWithInvalidFile.setEmail("test@example.com");
        requestWithInvalidFile.setUsername("testuser");
        requestWithInvalidFile.setPassword("Password123");
        requestWithInvalidFile.setFile(invalidFile);
        
        when(userRepository.findByEmail(requestWithInvalidFile.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(requestWithInvalidFile.getUsername())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.signupUser(requestWithInvalidFile))
                .isInstanceOf(ProfileImageException.class)
                .hasMessageContaining("Profile Picture type is not allowed");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_WithValidCredentials_ReturnsAuthResponse() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "Password123";
        
        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(password, userDetails.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateJwtToken(any(Authentication.class))).thenReturn("jwt.token.here");
        
        // Act
        AuthResponse response = authenticationService.loginUser(email, password);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJwt()).isEqualTo("jwt.token.here");
        assertThat(response.isStatus()).isTrue();
    }

    @Test
    void loginUser_WithInvalidEmail_ThrowsUserException() throws UserException{
        // Arrange
        String invalidEmail = "invalid";
        String password = "Password123";
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.loginUser(invalidEmail, password))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Unexpected error during login process");
    }

    @Test
    void loginUser_WithNonExistentUser_ThrowsUserException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "Password123";
        
        when(customUserDetailsService.loadUserByUsername(email)).thenThrow(new BadCredentialsException("User not found"));
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.loginUser(email, password))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginUser_WithIncorrectPassword_ThrowsUserException() {
        // Arrange
        String email = "test@example.com";
        String wrongPassword = "WrongPassword";
        
        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(wrongPassword, userDetails.getPassword())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.loginUser(email, wrongPassword))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
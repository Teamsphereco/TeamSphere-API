package co.teamsphere.api.controller;

import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.RefreshToken;
import co.teamsphere.api.request.LoginRequest;
import co.teamsphere.api.request.SignupRequest;
import co.teamsphere.api.response.AuthResponse;
import co.teamsphere.api.services.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private AuthResponse successAuthResponse;

    @BeforeEach
    void setUp() {
        // Setup SecurityContext mock
        SecurityContextHolder.setContext(securityContext);

        // Setup SignupRequest
        signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setUsername("testuser");
        signupRequest.setPassword("Password123");
        signupRequest.setFile(new MockMultipartFile(
                "profile_picture",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        ));

        // Setup LoginRequest
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123");

        // Setup success AuthResponse
        successAuthResponse = new AuthResponse("jwt.token.here", "refresh.token.here", true);

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setRefreshToken("refresh.token.here");
    }

    @Test
    void verifyJwtToken_AuthenticatedUser_ReturnsOk() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);

        // Act
        ResponseEntity<String> response = authController.verifyJwtToken();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyJwtToken_NotAuthenticated_ReturnsUnauthorized() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        ResponseEntity<String> response = authController.verifyJwtToken();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Token is invalid or not provided.");
    }

    @Test
    void userSignupMethod_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        when(authenticationService.signupUser(any(SignupRequest.class))).thenReturn(successAuthResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.userSignupMethod(signupRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(successAuthResponse);
        verify(authenticationService).signupUser(signupRequest);
    }

    @Test
    void userSignupMethod_UserException_ThrowsUserException() throws Exception {
        // Arrange
        when(authenticationService.signupUser(any(SignupRequest.class)))
                .thenThrow(new UserException("Email already exists"));

        // Act & Assert
        assertThatThrownBy(() -> authController.userSignupMethod(signupRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Email already exists");

        verify(authenticationService).signupUser(signupRequest);
    }

    @Test
    void userSignupMethod_ProfileImageException_ThrowsProfileImageException() throws Exception {
        // Arrange
        when(authenticationService.signupUser(any(SignupRequest.class)))
                .thenThrow(new ProfileImageException("Profile Picture type is not allowed!"));

        // Act & Assert
        assertThatThrownBy(() -> authController.userSignupMethod(signupRequest))
                .isInstanceOf(ProfileImageException.class)
                .hasMessageContaining("Profile Picture type is not allowed!");

        verify(authenticationService).signupUser(signupRequest);
    }

    @Test
    void userLoginMethod_ValidCredentials_ReturnsOk() throws Exception {
        // Arrange
        when(authenticationService.loginUser(loginRequest.getEmail(), loginRequest.getPassword()))
                .thenReturn(successAuthResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.userLoginMethod(loginRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(successAuthResponse);
        verify(authenticationService).loginUser(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @Test
    void userLoginMethod_InvalidCredentials_ThrowsUserException() throws Exception {
        // Arrange
        when(authenticationService.loginUser(loginRequest.getEmail(), loginRequest.getPassword()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authController.userLoginMethod(loginRequest))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationService).loginUser(loginRequest.getEmail(), loginRequest.getPassword());
    }
}

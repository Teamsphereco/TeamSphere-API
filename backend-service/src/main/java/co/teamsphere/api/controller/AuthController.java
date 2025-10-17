package co.teamsphere.api.controller;

import co.teamsphere.api.config.JWTTokenProvider;
import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.RefreshTokenException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.request.LoginRequest;
import co.teamsphere.api.request.RefreshTokenRequest;
import co.teamsphere.api.request.SignupRequest;
import co.teamsphere.api.response.AuthResponse;
import co.teamsphere.api.services.AuthenticationService;
import co.teamsphere.api.services.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private final JWTTokenProvider jwtTokenProvider;

    private final AuthenticationService authenticationService;

    private final RefreshTokenService refreshTokenService;

    public AuthController(JWTTokenProvider jwtTokenProvider,
                          AuthenticationService authenticationService,
                          RefreshTokenService refreshTokenService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify JWT Token", description = "Check if a provided JWT token is valid.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200"
            ),
        @ApiResponse(
                responseCode = "401",
                description = "Token is invalid or not provided"
            )
    })
    public ResponseEntity<String> verifyJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // This block is unlikely to be hit as the filter would reject invalid tokens
            return new ResponseEntity<>("Token is invalid or not provided.", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/refreshTest")
    @Operation(summary = "Refresh JWT Token", description = "Refresh JWT token with a refresh token.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Token refreshed successfully",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = AuthResponse.class)
                )
            ),
        @ApiResponse(
                responseCode = "400",
                description = "Token is invalid or not provided"
            ),
        @ApiResponse(
                responseCode = "500",
                description = "Something went wrong"
            )
    })
    public ResponseEntity<?> refreshTest(@RequestBody RefreshTokenRequest request) throws RefreshTokenException {
        try {
            // there is a way to do this with just optionals and maps, but you are a funny guy if you think im writing that out (it's not readable)
            log.info("Processing refresh token request");
            var refreshToken = refreshTokenService.findRefreshToken(request.getRefreshToken());

            log.info("checking for refresh token: {}", refreshToken);
            if (refreshToken.isEmpty()) {
                log.error("Refresh Token was not found: {}", request.getRefreshToken());
                throw new RefreshTokenException("Refresh Token is not valid!");
            }

            log.info("Verifying expiration of refresh token");
            var rToken = refreshTokenService.verifyExpiration(refreshToken.get());
            if (rToken == null) {
                log.error("Refresh Token is expired: {}", request.getRefreshToken());
                var msg = new AuthResponse("Unauthorized", null, false);
                return new ResponseEntity<>(msg, HttpStatus.UNAUTHORIZED);
            }

            log.info("Generating new JWT token");
            var user = rToken.getUser();
            String jwtToken = jwtTokenProvider.generateJwtTokenFromEmail(user.getEmail(), user.getId());
            String newRefreshToken = refreshTokenService.replaceRefreshToken(user.getEmail());
            if (newRefreshToken == null) {
                log.error("Error during refresh token replacement");
                throw new Exception("Error during refresh token replacement");
            }
            var auth = new AuthResponse(jwtToken, newRefreshToken, true);

            return new ResponseEntity<>(auth, HttpStatus.OK);
        } catch (RefreshTokenException e) {
            log.error("Error during JWT token verification: ", e);
            throw new RefreshTokenException("Token is invalid or not provided.");
        } catch (Exception e) {
            log.error("Error during JWT token verification: ", e);
            throw new RefreshTokenException("Something went Wrong.");
        }
    }

    @PostMapping(value="/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Sign up a new user", description = "Register new user and returns a JWT token.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "User successfully registered",
                content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class)
                )
            ),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
    })
    public ResponseEntity<AuthResponse> userSignupMethod (
            @Schema(description = "User details", implementation = SignupRequest.class)
            @Valid @ModelAttribute SignupRequest request) throws UserException, ProfileImageException {
        try {
            log.info("Processing signup request for user with email: {}, username:{}", request.getEmail(), request.getUsername());

            AuthResponse authResponse = authenticationService.signupUser(request);

            log.info("Signup process completed successfully for user with email: {}", request.getEmail());

            return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
        } catch (UserException e) {
            log.error("Error during signup process", e);
            throw e; // Rethrow specific exception to be handled by global exception handler
        } catch (ProfileImageException e){
            log.warn("File type not accepted, {}", request.getFile().getContentType());
            throw new ProfileImageException("Profile Picture type is not allowed!");
        } catch (Exception e) {
            log.error("Unexpected error during signup process", e);
            throw new UserException("Unexpected error during signup process");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login a user", description = "Login with email and password.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = AuthResponse.class)
                )
            ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> userLoginMethod(
            @Schema(description = "Login request body", implementation = LoginRequest.class)
            @Valid @RequestBody LoginRequest loginRequest) throws UserException {
        try {
            log.info("Processing login request for user with username: {}", loginRequest.getEmail());

            AuthResponse authResponse = authenticationService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

            log.info("Login successful for user with username: {}", loginRequest.getEmail());

            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user with username: {}", loginRequest.getEmail());
            throw new UserException("Invalid username or password.");
        } catch (UserException e) {
            log.error("Unexpected error during login process", e);
            throw new UserException("Unexpected error during login process.");
        }
    }
}

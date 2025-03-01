package co.teamsphere.api.services.impl;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import co.teamsphere.api.config.JWTTokenProvider;
import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.RefreshToken;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.UserRepository;
import co.teamsphere.api.request.SignupRequest;
import co.teamsphere.api.response.AuthResponse;
import co.teamsphere.api.response.CloudflareApiResponse;
import co.teamsphere.api.services.AuthenticationService;
import co.teamsphere.api.services.CloudflareApiService;
import co.teamsphere.api.services.RefreshTokenService;
import co.teamsphere.api.utils.GoogleAuthRequest;
import co.teamsphere.api.utils.GoogleUserInfo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Service
@Validated
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetails;
    private final CloudflareApiService cloudflareApiService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JWTTokenProvider jwtTokenProvider,
            CustomUserDetailsService customUserDetails,
            CloudflareApiService cloudflareApiService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetails = customUserDetails;
        this.cloudflareApiService = cloudflareApiService;
        this.refreshTokenService = refreshTokenService;
    }


    @Override
    @Transactional
    public AuthResponse signupUser(@Valid SignupRequest request) throws UserException, ProfileImageException {
        try {
            if (isEmailInvalid(request.getEmail())) {
                log.warn("Bad Email={} was passed in", request.getEmail());
                throw new UserException("Valid email was not passed in");
            }

            // Check if user with the given email or username already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Email={} is already used with another account", request.getEmail());
                throw new UserException("Email is already used with another account");
            }

            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                log.warn("Username={} is already used with another account", request.getUsername());
                throw new UserException("Username is already used with another account");
            }

            if (request.getFile().isEmpty() || (!request.getFile().getContentType().equals("image/jpeg") && !request.getFile().getContentType().equals("image/png"))) {
                log.warn("File type not accepted, {}", request.getFile().getContentType());
                throw new ProfileImageException("Profile Picture type is not allowed!");
            }

            // Upload profile picture to Cloudflare
            CloudflareApiResponse responseEntity = cloudflareApiService.uploadImage(request.getFile());
            String baseUrl = Objects.requireNonNull(responseEntity.getResult().getVariants().get(0));
            String profileUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + "public";

            var currentDateTime = LocalDateTime.now().atOffset(ZoneOffset.UTC);

            // Creating a new user
            var newUser = User.builder()
                    .email(request.getEmail())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .profilePicture(profileUrl)
                    .createdDate(currentDateTime)
                    .lastUpdatedDate(currentDateTime)
                    .build();

            userRepository.save(newUser);

            // auto-login after signup
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = authentication(request.getEmail(), request.getPassword());
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            if (!authentication.isAuthenticated()) {
                log.warn("Authentication failed for user with username: {}", request.getEmail());
                throw new BadCredentialsException("Invalid username or password.");
            }

            String token = jwtTokenProvider.generateJwtToken(authentication);

            log.info("Generating refresh token for user with ID: {}", newUser.getId());
            var refreshToken = refreshTokenService.createRefreshToken(newUser.getEmail());
            return new AuthResponse(token, refreshToken.getRefreshToken(), true);
        }
        catch (UserException e) {
            log.error("Error during signup process", e);
            throw new UserException("Error Signing up");
        } catch (BadCredentialsException e) {
            log.error("Authentication failed for user with username: {}", request.getEmail());
            throw new BadCredentialsException("Invalid username or password.", e);
        } catch (ProfileImageException e){
            log.error("ERROR: {}", e.getMessage());
            throw new ProfileImageException(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during signup process", e);
            throw new UserException("Unexpected error during signup process");
        }
    }

    @Override
    @Transactional
    public AuthResponse loginUser(String email, String password) throws UserException {
        try {
            if(isEmailInvalid(email)){
                log.warn("Email={} is already used with another account", email);
                throw new UserException("Email is already used with another account");
            }

            Optional<User> optionalUser = userRepository.findByEmail(email);
            if (!optionalUser.isPresent()) {
                log.warn("User with email={} not found", email);
                throw new BadCredentialsException("Invalid username or password.");
            }

            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = authentication(email, password);
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            if (!authentication.isAuthenticated()) {
                log.warn("Authentication failed for user with username: {}", email);
                throw new BadCredentialsException("Invalid username or password.");
            }

            String token = jwtTokenProvider.generateJwtToken(authentication);

            log.info("Generating refresh token for user with ID: {}", optionalUser.get().getId());
            RefreshToken refreshToken = createRefreshToken(optionalUser.get().getId().toString(), email);
            return new AuthResponse(token, refreshToken.getRefreshToken(), true);
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user with username: {}", email);
            throw new UserException("Invalid username or password.");
        } catch (Exception e) {
            log.error("Unexpected error during login process", e);
            throw new UserException("Unexpected error during login process.");
        }
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) throws UserException {
        try {
            GoogleUserInfo googleUserInfo = request.getGoogleUserInfo();

            String email = googleUserInfo.getEmail();
            String username = googleUserInfo.getName();
            String pictureUrl = googleUserInfo.getPicture();

            // Check if user exists
            User googleUser = null;
            Optional<User> optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                log.info("Existing user found with userId: {}", optionalUser.get().getId());
                googleUser = optionalUser.get();
            } else {
                // Register a new user if not exists
                var currentDateTime = LocalDateTime.now().atOffset(ZoneOffset.UTC);
                var user = User.builder()
                        .email(email)
                        .username(username)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // consider adding this cause the password field should NEVER be null
                        .profilePicture(pictureUrl)
                        .createdDate(currentDateTime)
                        .lastUpdatedDate(currentDateTime)
                        .build();

                googleUser = userRepository.saveAndFlush(user);
                log.info("New user created with email: {}", email);
            }

            // Load UserDetails and set authentication context
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = new UsernamePasswordAuthenticationToken(email, null);
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            String token = jwtTokenProvider.generateJwtTokenFromEmail(email);
            RefreshToken refreshToken = createRefreshToken(googleUser.getId().toString(), email);
            return new AuthResponse(token, refreshToken.getRefreshToken(), true);
        } catch (BadCredentialsException e) {
            log.error("Error during Google authentication: ", e);
            throw new BadCredentialsException("Error during Google authentication");
        } catch (Exception e) {
            log.error("Error during Google authentication: ", e);
            throw new UserException("Error during Google authentication");
        }
    }

    public static boolean isEmailInvalid(String email) {
        if (email.isEmpty())
            return true;

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        return !Pattern.compile(emailRegex).matcher(email).matches();
    }

    private Authentication authentication(String email, String password) throws BadCredentialsException {
        log.info("Authenticating user with email: {}", email);

        UserDetails userDetails = customUserDetails.loadUserByUsername(email);

        if (userDetails == null) {
            log.warn("User with email {} not found", email);
            throw new BadCredentialsException("Email or Password is invalid: " + email);
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.warn("Invalid password for user with email: {}", email);
            throw new BadCredentialsException("Email or Password is invalid: " + email);
        }

        log.info("Authentication successful for user with email: {}", email);

        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    private RefreshToken createRefreshToken(String userID, String email) throws UserException {
        RefreshToken refreshToken = refreshTokenService.findByUserId(userID);
        if (refreshToken == null || refreshToken.getExpiredAt().compareTo(Instant.now()) < 0) {
            refreshToken = refreshTokenService.createRefreshToken(email);
        }

        return refreshToken;
    }
}

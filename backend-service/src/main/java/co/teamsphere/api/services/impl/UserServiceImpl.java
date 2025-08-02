package co.teamsphere.api.services.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.teamsphere.api.config.JWTTokenProvider;
import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.UserRepository;
import co.teamsphere.api.request.UpdateUserRequest;
import co.teamsphere.api.response.CloudflareApiResponse;
import co.teamsphere.api.services.CloudflareApiService;
import co.teamsphere.api.services.UserService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final JWTTokenProvider jwtTokenProvider;
    private final CloudflareApiService cloudflareApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    public UserServiceImpl( UserRepository userRepo, JWTTokenProvider jwtTokenProvider, CloudflareApiService cloudflareApiService, RedisTemplate<String, Object> redisTemplate) {
        this.userRepo = userRepo;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cloudflareApiService = cloudflareApiService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest req, UUID reqUserId) throws UserException, ProfileImageException {
        try {
            log.info("Attempting to update user with ID: {}", userId);
            User user = findUserById(userId);

            if (user == null) {
                log.error("User with ID: {}, not found. Unable to update.", userId);
                throw new UserException("User not found");
            }

            // check if email is the same as the one in the JWT token
            if (!user.getId().equals(reqUserId)) {
                log.error("SECURITY ERROR: User with ID: {} has tried to edit someone else's account!", userId);
                throw new UserException("User not found");
            }

            log.info("Found user for update: {}", user.getId());

            User existingName = userRepo.findByUsername(req.getUsername()).orElse(null);

            if (req.getUsername() != null && !req.getUsername().isEmpty() && !req.getUsername().isBlank() && (existingName == null || !user.getUsername().equals(req.getUsername()))) {
                log.info("Updating username to: {}", req.getUsername());
                user.setUsername(req.getUsername());
            }

            if (req.getProfile_picture() != null) {
                log.info("Checking if profile picture is valid");
                if (req.getProfile_picture().isEmpty() || (!Objects.equals(req.getProfile_picture().getContentType(), "image/jpeg") && !req.getProfile_picture().getContentType().equals("image/png"))) {
                    log.warn("File type not accepted, {}", req.getProfile_picture().getContentType());
                    throw new ProfileImageException("Profile Picture type is not allowed!");
                }

                log.info("Updating profile picture: {}", user.getProfilePicture());
                var profileImg = user.getProfilePicture().split("/");

                CloudflareApiResponse responseEntity = cloudflareApiService.deleteImage(profileImg[profileImg.length - 2]);
                if (!responseEntity.isSuccess()) {
                    log.warn("Error deleting profile picture from Cloudflare, ID: {}", profileImg[profileImg.length - 2]);
                    throw new ProfileImageException("Error deleting profile picture from Cloudflare");
                }

                responseEntity = cloudflareApiService.uploadImage(req.getProfile_picture());
                if (!responseEntity.isSuccess() || responseEntity.getResult() == null || responseEntity.getResult().getVariants() == null || responseEntity.getResult().getVariants().isEmpty()) {
                    log.warn("Error uploading new profile picture to Cloudflare");
                    throw new ProfileImageException("Error uploading new profile picture to Cloudflare");
                }

                String baseUrl = Objects.requireNonNull(responseEntity.getResult().getVariants().getFirst());
                String profileUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + "public";
                user.setProfilePicture(profileUrl);
            }

            user.setLastUpdatedDate(LocalDateTime.now().atOffset(ZoneOffset.UTC));

            // Save the updated user
            User updatedUser = userRepo.save(user);
            log.info("User updated successfully. Updated user details: {}", updatedUser);

            return updatedUser;
        } catch (ProfileImageException e) {
            log.error("Error updating user profile image: {}", e.getMessage());
            throw new UserException("Error updating user: " + e.getMessage());
        } catch (UserException e) {
            log.error("Error updating user: {}", e.getMessage());
            throw new UserException("Error updating user: " + e.getMessage());
        } catch (IOException e) {
            log.error("Unexpected error during user update: {}", e.getMessage());
            throw new UserException("Unexpected error during user update!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(UUID userId) throws UserException {
        log.info("Attempting to find user by ID: {}", userId);

        var cacheKey = "user:" + userId.toString();
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);

        if (cachedUser != null) {
            log.info("User found in cache with ID: {}", userId);
            return cachedUser;
        }

        Optional<User> opt = userRepo.findById(userId);

        if (opt.isEmpty()) {
            log.error("ERROR: User not found with ID: {}", userId);
            throw new UserException("User doesn't exist with the id: " + userId);
        }

        User user = opt.get();
        log.info("Found user with ID {}: {}", userId, user);

        redisTemplate.opsForValue().set(cacheKey, user, 60, TimeUnit.HOURS);
        log.info("User cached with key: {}", cacheKey);

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserProfile(String jwt) {
        log.info("Attempting to find user profile using JWT");

        UUID userId = jwtTokenProvider.getIdFromToken(jwt);

        Optional<User> opt = userRepo.findById(userId);

        if (opt.isEmpty()) {
            log.error("User profile not found for userId: {}", userId);
            throw new BadCredentialsException("Received invalid token!");
        }

        log.info("Found user profile for userId: {}", opt.get().getId());
        return opt.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUser(String query) {
        log.info("Searching users with query: {}", query);

        List<User> searchResults = userRepo.searchUsers(query);

        if (searchResults.isEmpty()) {
            log.info("No users found matching the query: {}", query);
            return Collections.emptyList();
        }

        log.info("Found {} user(s) matching the query.", searchResults.size());
        return searchResults;
    }
}

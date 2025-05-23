package co.teamsphere.api.services.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.teamsphere.api.exception.RefreshTokenException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.RefreshToken;
import co.teamsphere.api.models.User;
import co.teamsphere.api.repository.RefreshTokenRepository;
import co.teamsphere.api.repository.UserRepository;
import co.teamsphere.api.services.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String email) throws UserException {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.error("User not found with ID: {}", email);
            throw new UserException("User not found with ID: " + email);
        }

        return refreshTokenRepository.save(RefreshToken.builder()
                .user(user.get())
                .refreshToken(UUID.randomUUID().toString())
                .expiredAt(Instant.now().plusSeconds(86400000))
                .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) throws RefreshTokenException {
        if(token.getExpiredAt().compareTo(Instant.now())<0){
            refreshTokenRepository.delete(token);
            return null;
        }

        return token;
    }

    @Override
    @Transactional
    public void deleteRefreshTokenByUserId(String userId) {
        Optional<RefreshToken> user = refreshTokenRepository.findByUserId(UUID.fromString(userId));

        if(user.isPresent()){
            refreshTokenRepository.delete(user.get());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByUserId(String userId) {
        Optional<RefreshToken> potentialToken = refreshTokenRepository.findByUserId(UUID.fromString(userId));

        if(potentialToken.isEmpty()){
            return null;
        }

        return potentialToken.get();
    }

    @Override
    @Transactional
    public String replaceRefreshToken(String oldRefreshToken) {
        try {
            Optional<RefreshToken> token = refreshTokenRepository.findByRefreshToken(oldRefreshToken);
            if(token.isEmpty() && !token.get().getRefreshToken().equals(oldRefreshToken)){
                log.error("Refresh token not found with ID: {}", oldRefreshToken);
                throw new UserException("Refresh token not found with ID: " + oldRefreshToken);
            }
            token.get().setRefreshToken(UUID.randomUUID().toString());
            return refreshTokenRepository.save(token.get()).getRefreshToken();
        } catch (UserException e) {
            log.error("Error occurred while replacing refresh token for user with refreshToken: {}", oldRefreshToken);
            return null;
        }
    }
}

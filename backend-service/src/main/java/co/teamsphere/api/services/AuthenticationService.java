package co.teamsphere.api.services;

import org.springframework.stereotype.Service;

import co.teamsphere.api.exception.ProfileImageException;
import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.request.SignupRequest;
import co.teamsphere.api.response.AuthResponse;
import co.teamsphere.api.utils.GoogleAuthRequest;
import jakarta.validation.Valid;

@Service
public interface AuthenticationService {
    AuthResponse signupUser(@Valid SignupRequest request) throws UserException, ProfileImageException;

    AuthResponse loginUser(String username, String password) throws UserException;

    AuthResponse loginWithGoogle(GoogleAuthRequest request) throws UserException;
}

package co.teamsphere.api.services;

import co.teamsphere.api.exception.UserException;
import co.teamsphere.api.models.User;
import co.teamsphere.api.request.UpdateUserRequest;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import co.teamsphere.api.exception.ProfileImageException;

@Service
public interface UserService{

    User findUserProfile(String jwt);

    User updateUser(UUID userId, UpdateUserRequest req, UUID reqUser) throws UserException, ProfileImageException;

    User findUserById(UUID userId) throws UserException;

    List<User> searchUser(String query);
}

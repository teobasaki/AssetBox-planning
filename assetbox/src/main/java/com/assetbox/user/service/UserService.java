package com.assetbox.user.service;

import com.assetbox.user.domain.User;
import com.assetbox.user.dto.UserResponse;
import java.util.Optional;

public interface UserService {

    User requireExists(Long userId);

    Optional<User> findByEmail(String email);

    User getMe(Long currentUserId);

    UserResponse toResponse(User user);

    Long getSystemUserId();
}

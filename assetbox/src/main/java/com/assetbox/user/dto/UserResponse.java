package com.assetbox.user.dto;

import com.assetbox.user.domain.Role;

public record UserResponse(
        Long id,
        String email,
        String realName,
        String nickname,
        String major,
        Role role,
        String bio,
        String avatarUrl
) {
}

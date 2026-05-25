package com.assetbox.common.security;

import com.assetbox.user.domain.Role;
import java.util.Set;

public record AuthUser(Long id, String email, Set<Role> roles) {
}

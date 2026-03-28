package com.oramadan.ratto.auth;

import java.util.Set;

public class AuthorizationService {

    private final Set<Long> superadminUserIds;

    public AuthorizationService(Set<Long> superadminUserIds) {
        this.superadminUserIds = superadminUserIds;
    }

    public boolean isSuperadmin(long userId) {
        return superadminUserIds.contains(userId);
    }
}

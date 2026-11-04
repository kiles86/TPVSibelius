package com.tpv26.domain.model;

public record User(Long id, String username, String passwordHash, UserRole role) {
    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}

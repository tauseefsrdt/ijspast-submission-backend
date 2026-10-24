package com.ijspast.journal.entity;

public enum UserRole {
    ROLE_AUTHOR("author"),
    ROLE_EDITOR("editor"),
    ROLE_REVIEWER("reviewer");

    private final String simpleName;

    UserRole(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public static UserRole fromSimpleName(String simpleName) {
        if (simpleName == null) {
            return ROLE_AUTHOR;
        }
        for (UserRole role : values()) {
            if (role.simpleName.equalsIgnoreCase(simpleName) || role.name().equalsIgnoreCase(simpleName)) {
                return role;
            }
        }
        return ROLE_AUTHOR;
    }
}

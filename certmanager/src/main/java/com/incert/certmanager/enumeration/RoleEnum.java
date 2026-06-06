package com.incert.certmanager.enumeration;

public enum RoleEnum {
    ROLE_ADMIN,
    ROLE_USER;

    public static RoleEnum fromString(String role) {
        if (role == null) return null;
        String cleaned = role.toUpperCase();
        if (!cleaned.startsWith("ROLE_")) {
            cleaned = "ROLE_" + cleaned;
        }
        try {
            return RoleEnum.valueOf(cleaned);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

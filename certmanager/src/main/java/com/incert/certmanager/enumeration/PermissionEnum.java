package com.incert.certmanager.enumeration;

public enum PermissionEnum {
    READ,
    WRITE,
    UPDATE;

    public static PermissionEnum fromString(String permission) {
        if (permission == null) return null;
        try {
            return PermissionEnum.valueOf(permission.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

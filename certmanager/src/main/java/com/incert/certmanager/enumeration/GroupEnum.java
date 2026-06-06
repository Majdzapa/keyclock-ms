package com.incert.certmanager.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
@AllArgsConstructor
@Getter
public enum GroupEnum {
    ADMINS("/admins"),
    DEVELOPERS("/developers"),
    VIEWERS("/viewers");

    private final String path;

    public static GroupEnum fromPath(String value) {
        return Arrays.stream(values())
                .filter(group ->
                        group.path.equalsIgnoreCase(value)
                                || group.name().equalsIgnoreCase(value)
                )
                .findFirst()
                .orElse(null);
    }

}

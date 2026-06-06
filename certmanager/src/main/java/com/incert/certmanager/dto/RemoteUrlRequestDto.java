package com.incert.certmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RemoteUrlRequestDto(

    @NotBlank(message = "URL must not be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    String url,

    @Pattern(regexp = "^([0-9]{1,5})?$", message = "Port must be a valid number")
    String port
) {

    public int resolvedPort() {
        if (port == null || port.isBlank()) {
            return 443;
        }
        int p = Integer.parseInt(port);
        if (p < 1 || p > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return p;
    }
}

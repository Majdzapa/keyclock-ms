package com.incert.certmanager.dto;

import com.incert.certmanager.enumeration.SourceTypeEnum;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CertificateResponseDto(

    Long id,
    String filename,
    String mimeType,

    // Parsed certificate fields
    String issuer,
    String subject,
    String owner,
    String serialNumber,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime expirationDate,
    String algorithm,
    String fingerprint,
    String sans,

    // Origin
    SourceTypeEnum sourceType,
    String remoteUrl,
    String uploadedBy,
    String uploadedByGroup,

    // Auditing
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Computed helper for UI
    boolean isExpiringSoon,
    boolean isExpired
) {}

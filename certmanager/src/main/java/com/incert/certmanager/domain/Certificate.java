package com.incert.certmanager.domain;

import com.incert.certmanager.enumeration.SourceTypeEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a managed SSL/TLS certificate.
 * <p>
 * Stores both the raw certificate bytes (BLOB) and all parsed metadata fields.
 * Uses {@link SourceTypeEnum} to distinguish manual uploads from remote URL fetches.
 */
@Entity
@Table(
    name = "certificates",
    indexes = {
        @Index(name = "idx_uploaded_by_group", columnList = "uploaded_by_group"),
        @Index(name = "idx_expiration_date",   columnList = "expiration_date"),
        @Index(name = "idx_source_type",       columnList = "source_type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "certificateContent")
@EqualsAndHashCode(of = "id")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────── File Metadata ────────────────────────────

    @NotBlank
    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // ─────────────────────────── Parsed Fields ────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String issuer;

    @Column(columnDefinition = "TEXT")
    private String subject;

    /** Common Name or Organisation extracted from subject. */
    @Column(length = 512)
    private String owner;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(length = 128)
    private String algorithm;

    @Column(length = 512)
    private String fingerprint;

    /** Subject Alternative Names, stored as comma-separated string. */
    @Column(columnDefinition = "TEXT")
    private String sans;

    /**
     * Alias of {@link #validTo} — maintained separately for explicit
     * expiry queries and scheduler use.
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    // ─────────────────────────── Origin Metadata ──────────────────────────

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceTypeEnum sourceType;

    /** Only populated when sourceType == REMOTE_URL. */
    @Column(name = "remote_url", length = 2048)
    private String remoteUrl;

    @NotBlank
    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    /** The Keycloak group the uploader belonged to at upload time. */
    @Column(name = "uploaded_by_group", length = 255)
    private String uploadedByGroup;

    // ─────────────────────────── Raw Certificate ──────────────────────────

    /**
     * Raw certificate file bytes.
     * Stored as LONGBLOB to support binary formats (p12, pfx) up to ~16 MB.
     */
    @Lob
    @Column(name = "certificate_content", columnDefinition = "LONGBLOB")
    private byte[] certificateContent;

    // ─────────────────────────── Auditing ─────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Keep expirationDate in sync with validTo on initial save
        if (this.expirationDate == null) {
            this.expirationDate = this.validTo;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.expirationDate = this.validTo;
    }
}

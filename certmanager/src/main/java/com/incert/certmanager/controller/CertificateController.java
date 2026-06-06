package com.incert.certmanager.controller;

import com.incert.certmanager.dto.CertificateResponseDto;
import com.incert.certmanager.dto.RemoteUrlRequestDto;
import com.incert.certmanager.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for certificate management operations.
 *
 * <p>Base path: {@code /api/v1/certificates}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    // ─────────────────────────── Upload ───────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') and (hasAuthority('WRITE') or hasAuthority('UPDATE'))")
    public ResponseEntity<CertificateResponseDto> uploadCertificate(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        log.info("POST /api/v1/certificates/upload — file: {}", file.getOriginalFilename());
        CertificateResponseDto response = certificateService.uploadCertificate(file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────── Fetch from URL ───────────────────────────

    @PostMapping("/fetch-url")
    @PreAuthorize("hasRole('ADMIN') and (hasAuthority('WRITE') or hasAuthority('UPDATE'))")
    public ResponseEntity<CertificateResponseDto> fetchCertificateFromUrl(
            @Valid @RequestBody RemoteUrlRequestDto request,
            Authentication authentication) {

        log.info("POST /api/v1/certificates/fetch-url — url: {}", request.url());
        CertificateResponseDto response = certificateService.fetchCertificateFromUrl(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────── List ─────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasAuthority('READ'))")
    public ResponseEntity<List<CertificateResponseDto>> getCertificates(Authentication authentication) {
        log.debug("GET /api/v1/certificates");
        return ResponseEntity.ok(certificateService.getCertificates(authentication));
    }

    // ─────────────────────────── Get By ID ────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasAuthority('READ'))")
    public ResponseEntity<CertificateResponseDto> getCertificateById(
            @PathVariable Long id,
            Authentication authentication) {

        log.debug("GET /api/v1/certificates/{}", id);
        return ResponseEntity.ok(certificateService.getCertificateById(id, authentication));
    }

    // ─────────────────────────── Update ───────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and (hasAuthority('WRITE') or hasAuthority('UPDATE'))")
    public ResponseEntity<CertificateResponseDto> updateCertificate(
            @PathVariable Long id,
            @RequestParam String owner,
            Authentication authentication) {

        log.info("PUT /api/v1/certificates/{} — owner: {}", id, owner);
        return ResponseEntity.ok(certificateService.updateCertificate(id, owner, authentication));
    }

    // ─────────────────────────── Delete ───────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCertificate(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("DELETE /api/v1/certificates/{}", id);
        certificateService.deleteCertificate(id);
    }

    // ─────────────────────────── Download ─────────────────────────────────

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasAuthority('READ'))")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long id,
            Authentication authentication) {
        byte[] content = certificateService.downloadCertificate(id, authentication);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + id + ".pem\"")
            .body(content);
    }
}

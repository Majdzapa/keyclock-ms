package com.incert.certmanager.service;

import com.incert.certmanager.config.JwtClaimsExtractor;
import com.incert.certmanager.domain.Certificate;
import com.incert.certmanager.enumeration.GroupEnum;
import com.incert.certmanager.enumeration.RoleEnum;
import com.incert.certmanager.enumeration.SourceTypeEnum;
import com.incert.certmanager.dto.CertificateResponseDto;
import com.incert.certmanager.dto.RemoteUrlRequestDto;
import com.incert.certmanager.exception.CertificateNotFoundException;
import com.incert.certmanager.exception.CertificateParsingException;
import com.incert.certmanager.exception.InvalidCertificateFormatException;
import com.incert.certmanager.exception.UnauthorizedGroupAccessException;
import com.incert.certmanager.mapper.CertificateMapper;
import com.incert.certmanager.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final RemoteCertificateFetchService remoteCertificateFetchService;
    private final CertificateMapper certificateMapper;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    @Value("${app.certificate.allowed-extensions:pem,crt,cer,p12,pfx}")
    private List<String> allowedExtensions;


    @Transactional
    public CertificateResponseDto uploadCertificate(MultipartFile file, Authentication authentication) {

        validateFile(file);

        try {
            byte[] fileBytes = file.getBytes();
            String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));

            // Parse the certificate to extract all metadata fields
            X509Certificate x509 = parseCertificateBytes(fileBytes, extension);

            Certificate certificate = buildFromX509(x509, fileBytes, file.getOriginalFilename(),
                                                    file.getContentType());
            certificate.setSourceType(SourceTypeEnum.MANUAL_UPLOAD);

            Certificate saved = certificateRepository.save(certificate);
            log.info("Certificate saved with ID: {}", saved.getId());

            return certificateMapper.toResponseDto(saved);

        } catch (IOException e) {
            throw new CertificateParsingException("Failed to read uploaded file", e);
        }
    }

    // ─────────────────────────── Remote Fetch ─────────────────────────────

    @Transactional
    public CertificateResponseDto fetchCertificateFromUrl(RemoteUrlRequestDto request, Authentication authentication) {
        log.info("Fetching certificate from URL: {} by user: {}",
                 request.url(), jwtClaimsExtractor.getCurrentUsername());

        Certificate certificate = remoteCertificateFetchService.fetchAndParse(request);

        // Enrich with uploader info
        String username = jwtClaimsExtractor.getCurrentUsername();
        GroupEnum group    = jwtClaimsExtractor.getCurrentUserPrimaryGroup();
        certificate.setUploadedBy(username);
        certificate.setUploadedByGroup(group != null ? group.getPath() : null);

        Certificate saved = certificateRepository.save(certificate);
        log.info("Remote certificate from {} saved with ID: {}", request.url(), saved.getId());

        return certificateMapper.toResponseDto(saved);
    }

    // ─────────────────────────── Read (List) ──────────────────────────────

    public List<CertificateResponseDto> getCertificates(Authentication authentication) {
        List<Certificate> certificates;

        if (isAdmin(authentication)) {
            certificates = certificateRepository.findAll();
        } else {
            GroupEnum group = jwtClaimsExtractor.getCurrentUserPrimaryGroup();
            log.info("this is the group {}",group);
            certificates = certificateRepository.findByUploadedByGroup(group.getPath());
        }

        return certificateMapper.toResponseDtoList(certificates);
    }

    // ─────────────────────────── Read (By ID) ─────────────────────────────

    public CertificateResponseDto getCertificateById(Long id, Authentication authentication) {
        Certificate certificate;

        if (isAdmin(authentication)) {
            certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new CertificateNotFoundException(id));
        } else {
            GroupEnum group = jwtClaimsExtractor.getCurrentUserPrimaryGroup();

            certificate = certificateRepository.findByIdAndUploadedByGroup(id, group.getPath())
                .orElseThrow(() -> new UnauthorizedGroupAccessException(
                    "Certificate " + id + " does not belong to your group or does not exist"));
        }

        return certificateMapper.toResponseDto(certificate);
    }

    // ─────────────────────────── Update ───────────────────────────────────

    @Transactional
    public CertificateResponseDto updateCertificate(Long id, String owner, Authentication authentication) {
        Certificate certificate = certificateRepository.findById(id)
            .orElseThrow(() -> new CertificateNotFoundException(id));

        log.info("Updating certificate ID: {} owner to: {} by user: {}",
                 id, owner, jwtClaimsExtractor.getCurrentUsername());

        certificate.setOwner(owner);
        Certificate saved = certificateRepository.save(certificate);
        return certificateMapper.toResponseDto(saved);
    }

    // ─────────────────────────── Delete ───────────────────────────────────

    @Transactional
    public void deleteCertificate(Long id) {
        if (!certificateRepository.existsById(id)) {
            throw new CertificateNotFoundException(id);
        }
        log.info("Deleting certificate ID: {} by user: {}",
                 id, jwtClaimsExtractor.getCurrentUsername());
        certificateRepository.deleteById(id);
    }

    // ─────────────────────────── Download ─────────────────────────────────

    public byte[] downloadCertificate(Long id, Authentication authentication) {
        Certificate certificate;

        if (isAdmin(authentication)) {
            certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new CertificateNotFoundException(id));
        } else {
            GroupEnum group = jwtClaimsExtractor.getCurrentUserPrimaryGroup();
            if (group == null) {
                throw new UnauthorizedGroupAccessException(
                    "You must belong to a group to access certificates");
            }
            certificate = certificateRepository.findByIdAndUploadedByGroup(id, group.getPath())
                .orElseThrow(() -> new UnauthorizedGroupAccessException(
                    "Certificate " + id + " does not belong to your group"));
        }

        if (certificate.getCertificateContent() == null) {
            throw new CertificateNotFoundException("No file content stored for certificate " + id);
        }
        return certificate.getCertificateContent();
    }

    // ─────────────────────────── Private Helpers ──────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCertificateFormatException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidCertificateFormatException("Filename is missing");
        }

        String ext = getFileExtension(filename).toLowerCase();
        List<String> allowed = allowedExtensions;
        if (allowed == null || !allowed.contains(ext)) {
            throw new InvalidCertificateFormatException(
                "Unsupported file type: ." + ext + ". Allowed: " + allowed);
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) {
            throw new InvalidCertificateFormatException("File has no extension: " + filename);
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private X509Certificate parseCertificateBytes(byte[] bytes, String extension) {
        try {
            if (extension.equals("p12") || extension.equals("pfx")) {
                return parseP12(bytes);
            }
            try (PEMParser parser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
                Object obj = parser.readObject();
                if (obj instanceof X509CertificateHolder holder) {
                    return new JcaX509CertificateConverter()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate(holder);
                }
            }
            return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(bytes));

        } catch (Exception e) {
            throw new CertificateParsingException("Cannot parse certificate file: " + e.getMessage(), e);
        }
    }

    private X509Certificate parseP12(byte[] bytes) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12",
                                        BouncyCastleProvider.PROVIDER_NAME);
            ks.load(new ByteArrayInputStream(bytes), null);
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isCertificateEntry(alias) || ks.isKeyEntry(alias)) {
                    return (X509Certificate) ks.getCertificate(alias);
                }
            }
            throw new CertificateParsingException("No certificate found in PKCS#12 file");
        } catch (CertificateParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateParsingException("Failed to parse PKCS#12 file: " + e.getMessage(), e);
        }
    }

    private Certificate buildFromX509(X509Certificate x509, byte[] rawBytes,
                                      String filename, String mimeType) {
        String issuer      = x509.getIssuerX500Principal().getName();
        String subject     = x509.getSubjectX500Principal().getName();
        String owner       = extractCN(subject);
        String serialNum   = x509.getSerialNumber().toString(16).toUpperCase();
        String algorithm   = x509.getSigAlgName();
        String fingerprint = computeFingerprint(x509);
        LocalDateTime validFrom = toLocalDateTime(x509.getNotBefore());
        LocalDateTime validTo   = toLocalDateTime(x509.getNotAfter());

        String username = jwtClaimsExtractor.getCurrentUsername();
        GroupEnum group    = jwtClaimsExtractor.getCurrentUserPrimaryGroup();

        return Certificate.builder()
            .filename(filename)
            .mimeType(mimeType != null ? mimeType : "application/octet-stream")
            .issuer(issuer)
            .subject(subject)
            .owner(owner)
            .serialNumber(serialNum)
            .validFrom(validFrom)
            .validTo(validTo)
            .expirationDate(validTo)
            .algorithm(algorithm)
            .fingerprint(fingerprint)
            .uploadedBy(username)
            .uploadedByGroup(group != null ? group.getPath() : null)
            .certificateContent(rawBytes)
            .build();
    }

    private String extractCN(String dn) {
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) return trimmed.substring(3);
        }
        return dn;
    }

    private String computeFingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                if (!sb.isEmpty()) sb.append(':');
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(RoleEnum.ROLE_ADMIN.name()::equals);
    }
}

package com.incert.certmanager.service;

import com.incert.certmanager.domain.Certificate;
import com.incert.certmanager.dto.CertificateResponseDto;
import com.incert.certmanager.exception.CertificateNotFoundException;
import com.incert.certmanager.exception.InvalidCertificateFormatException;
import com.incert.certmanager.exception.UnauthorizedGroupAccessException;
import com.incert.certmanager.mapper.CertificateMapper;
import com.incert.certmanager.repository.CertificateRepository;
import com.incert.certmanager.config.JwtClaimsExtractor;
import com.incert.certmanager.enumeration.SourceTypeEnum;
import com.incert.certmanager.enumeration.GroupEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CertificateService}.
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private RemoteCertificateFetchService remoteCertificateFetchService;

    @Mock
    private CertificateMapper certificateMapper;

    @Mock
    private JwtClaimsExtractor jwtClaimsExtractor;

    @InjectMocks
    private CertificateService certificateService;

    @Mock
    private Authentication adminAuth;

    @Mock
    private Authentication userAuth;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
            certificateService, "allowedExtensions", List.of("pem", "crt", "cer", "p12", "pfx"));

        // Admin authentication
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("WRITE")))
            .when(adminAuth).getAuthorities();

        // User authentication
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("READ")))
            .when(userAuth).getAuthorities();
    }

    // ─────────────────────────── List Tests ───────────────────────────────

    @Test
    void getCertificates_asAdmin_shouldReturnAll() {
        Certificate cert = buildCertificate();
        CertificateResponseDto dto = buildResponseDto();

        when(certificateRepository.findAll()).thenReturn(List.of(cert));
        when(certificateMapper.toResponseDtoList(anyList())).thenReturn(List.of(dto));

        List<CertificateResponseDto> result = certificateService.getCertificates(adminAuth);

        assertThat(result).hasSize(1);
        verify(certificateRepository).findAll();
        verify(certificateRepository, never()).findByUploadedByGroup(any());
    }

    @Test
    void getCertificates_asUser_shouldFilterByGroup() {
        Certificate cert = buildCertificate();
        CertificateResponseDto dto = buildResponseDto();

        when(jwtClaimsExtractor.getCurrentUserPrimaryGroup()).thenReturn(GroupEnum.VIEWERS);
        when(certificateRepository.findByUploadedByGroup("/viewers")).thenReturn(List.of(cert));
        when(certificateMapper.toResponseDtoList(anyList())).thenReturn(List.of(dto));

        List<CertificateResponseDto> result = certificateService.getCertificates(userAuth);

        assertThat(result).hasSize(1);
        verify(certificateRepository).findByUploadedByGroup("/viewers");
        verify(certificateRepository, never()).findAll();
    }

    // ─────────────────────────── Get By ID Tests ──────────────────────────

    @Test
    void getCertificateById_asAdmin_shouldReturnAny() {
        Certificate cert = buildCertificate();
        CertificateResponseDto dto = buildResponseDto();

        when(certificateRepository.findById(1L)).thenReturn(Optional.of(cert));
        when(certificateMapper.toResponseDto(cert)).thenReturn(dto);

        CertificateResponseDto result = certificateService.getCertificateById(1L, adminAuth);

        assertThat(result).isNotNull();
    }

    @Test
    void getCertificateById_asAdmin_notFound_shouldThrow() {
        when(certificateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.getCertificateById(99L, adminAuth))
            .isInstanceOf(CertificateNotFoundException.class);
    }

    @Test
    void getCertificateById_asUser_wrongGroup_shouldThrow() {
        when(jwtClaimsExtractor.getCurrentUserPrimaryGroup()).thenReturn(GroupEnum.VIEWERS);
        when(certificateRepository.findByIdAndUploadedByGroup(1L, "/viewers"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.getCertificateById(1L, userAuth))
            .isInstanceOf(UnauthorizedGroupAccessException.class);
    }

    // ─────────────────────────── Delete Tests ─────────────────────────────

    @Test
    void deleteCertificate_existingId_shouldDelete() {
        when(certificateRepository.existsById(1L)).thenReturn(true);
        when(jwtClaimsExtractor.getCurrentUsername()).thenReturn("admin");

        certificateService.deleteCertificate(1L);

        verify(certificateRepository).deleteById(1L);
    }

    @Test
    void deleteCertificate_nonExistingId_shouldThrow() {
        when(certificateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> certificateService.deleteCertificate(99L))
            .isInstanceOf(CertificateNotFoundException.class);
    }

    // ─────────────────────────── Upload Validation Tests ──────────────────

    @Test
    void uploadCertificate_invalidExtension_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> certificateService.uploadCertificate(file, adminAuth))
            .isInstanceOf(InvalidCertificateFormatException.class)
            .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadCertificate_emptyFile_shouldThrow() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "cert.pem", "application/x-pem-file", new byte[0]);

        assertThatThrownBy(() -> certificateService.uploadCertificate(emptyFile, adminAuth))
            .isInstanceOf(InvalidCertificateFormatException.class);
    }

    // ─────────────────────────── Builders ─────────────────────────────────

    private Certificate buildCertificate() {
        return Certificate.builder()
            .id(1L)
            .filename("test.pem")
            .issuer("CN=Test CA")
            .subject("CN=test.example.com")
            .owner("test.example.com")
            .serialNumber("ABC123")
            .validFrom(LocalDateTime.now().minusDays(30))
            .validTo(LocalDateTime.now().plusDays(30))
            .expirationDate(LocalDateTime.now().plusDays(30))
            .algorithm("SHA256withRSA")
            .fingerprint("AA:BB:CC:DD")
            .sourceType(SourceTypeEnum.MANUAL_UPLOAD)
            .uploadedBy("admin")
            .uploadedByGroup("/admins")
            .build();
    }

    private CertificateResponseDto buildResponseDto() {
        return CertificateResponseDto.builder()
            .id(1L)
            .filename("test.pem")
            .issuer("CN=Test CA")
            .subject("CN=test.example.com")
            .owner("test.example.com")
            .sourceType(SourceTypeEnum.MANUAL_UPLOAD)
            .uploadedBy("admin")
            .uploadedByGroup("/admins")
            .isExpired(false)
            .isExpiringSoon(false)
            .build();
    }
}

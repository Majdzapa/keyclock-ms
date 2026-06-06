package com.incert.certmanager.mapper;

import com.incert.certmanager.domain.Certificate;
import com.incert.certmanager.dto.CertificateResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;
import java.util.List;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CertificateMapper {

    @Mapping(target = "isExpired",       expression = "java(isExpired(certificate))")
    @Mapping(target = "isExpiringSoon",  expression = "java(isExpiringSoon(certificate))")
    CertificateResponseDto toResponseDto(Certificate certificate);


    List<CertificateResponseDto> toResponseDtoList(List<Certificate> certificates);

    default boolean isExpired(Certificate certificate) {
        return certificate.getExpirationDate() != null
               && certificate.getExpirationDate().isBefore(LocalDateTime.now());
    }

    default boolean isExpiringSoon(Certificate certificate) {
        if (certificate.getExpirationDate() == null) return false;
        LocalDateTime threshold = LocalDateTime.now().plusHours(72);
        LocalDateTime now = LocalDateTime.now();
        return certificate.getExpirationDate().isAfter(now)
               && certificate.getExpirationDate().isBefore(threshold);
    }
}

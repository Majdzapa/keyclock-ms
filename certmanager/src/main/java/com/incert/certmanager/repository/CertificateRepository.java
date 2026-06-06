package com.incert.certmanager.repository;

import com.incert.certmanager.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByUploadedByGroup(String group);

    Optional<Certificate> findByIdAndUploadedByGroup(Long id, String group);

    @Query("SELECT c FROM Certificate c WHERE c.expirationDate IS NOT NULL AND c.expirationDate <= :threshold AND c.expirationDate >= CURRENT_TIMESTAMP")
    List<Certificate> findAllExpiringBefore(@Param("threshold") LocalDateTime threshold);

}

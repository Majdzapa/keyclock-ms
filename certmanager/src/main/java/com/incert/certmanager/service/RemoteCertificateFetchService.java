package com.incert.certmanager.service;

import com.incert.certmanager.domain.Certificate;
import com.incert.certmanager.enumeration.SourceTypeEnum;
import com.incert.certmanager.dto.RemoteUrlRequestDto;
import com.incert.certmanager.exception.CertificateParsingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteCertificateFetchService {

    @Value("${app.certificate.remote-fetch-timeout-ms:10000}")
    private int remoteFetchTimeoutMs;


    public Certificate fetchAndParse(RemoteUrlRequestDto request) {
        String host = sanitizeHost(request.url());
        int port    = request.resolvedPort();

        log.info("Fetching SSL certificate from {}:{}", host, port);

        X509Certificate leafCert = fetchLeafCertificate(host, port);
        return parseCertificate(leafCert, host, port);
    }

    // ─────────────────────────── TLS Connection ───────────────────────────

    private X509Certificate fetchLeafCertificate(String host, int port) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // Use a trust-all manager so we can inspect self-signed / expired certs
            sslContext.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new java.security.SecureRandom());

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            try (Socket plainSocket = new Socket()) {
                plainSocket.connect(new InetSocketAddress(host, port), remoteFetchTimeoutMs);

                try (SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(
                        plainSocket, host, port, true)) {

                    sslSocket.setSoTimeout(remoteFetchTimeoutMs);

                    // Set SNI extension
                    SSLParameters params = sslSocket.getSSLParameters();
                    params.setServerNames(List.of(new SNIHostName(host)));
                    sslSocket.setSSLParameters(params);

                    sslSocket.startHandshake();

                    java.security.cert.Certificate[] chain = sslSocket.getSession().getPeerCertificates();
                    if (chain == null || chain.length == 0) {
                        throw new CertificateParsingException("No certificates returned from " + host);
                    }

                    return (X509Certificate) chain[0];
                }
            }

        } catch (CertificateParsingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to {}:{} — {}", host, port, e.getMessage(), e);
            throw new CertificateParsingException(
                "Failed to retrieve certificate from " + host + ":" + port + " — " + e.getMessage(), e);
        }
    }

    // ─────────────────────────── Certificate Parsing ──────────────────────

    private Certificate parseCertificate(X509Certificate x509, String host, int port) {
        try {
            X509CertificateHolder holder = new X509CertificateHolder(x509.getEncoded());

            String issuer      = x509.getIssuerX500Principal().getName();
            String subject     = x509.getSubjectX500Principal().getName();
            String owner       = extractCN(subject);
            String serialNum   = x509.getSerialNumber().toString(16).toUpperCase();
            String algorithm   = x509.getSigAlgName();
            String fingerprint = computeFingerprint(x509);
            String sans        = extractSANs(holder);
            LocalDateTime validFrom = toLocalDateTime(x509.getNotBefore());
            LocalDateTime validTo   = toLocalDateTime(x509.getNotAfter());

            // Encode certificate back to PEM
            byte[] pemBytes = encodeToPem(x509);

            String remoteUrl = host + ":" + port;

            return Certificate.builder()
                .filename(host + ".pem")
                .mimeType("application/x-pem-file")
                .issuer(issuer)
                .subject(subject)
                .owner(owner)
                .serialNumber(serialNum)
                .validFrom(validFrom)
                .validTo(validTo)
                .expirationDate(validTo)
                .algorithm(algorithm)
                .fingerprint(fingerprint)
                .sans(sans)
                .sourceType(SourceTypeEnum.REMOTE_URL)
                .remoteUrl(remoteUrl)
                .certificateContent(pemBytes)
                .build();

        } catch (CertificateEncodingException | IOException e) {
            throw new CertificateParsingException("Failed to parse certificate from " + host, e);
        }
    }

    // ─────────────────────────── Helpers ──────────────────────────────────

    private String sanitizeHost(String url) {
        return url.replaceFirst("^https?://", "")
                  .replaceFirst("/.*$", "")
                  .trim();
    }

    private String extractCN(String dn) {
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return dn;
    }

    private String computeFingerprint(X509Certificate cert) throws CertificateEncodingException {
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
            log.warn("Could not compute fingerprint: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private String extractSANs(X509CertificateHolder holder) {
        try {
            Extension sanExt = holder.getExtension(Extension.subjectAlternativeName);
            if (sanExt == null) return "";

            GeneralNames generalNames = GeneralNames.getInstance(sanExt.getParsedValue());
            return Arrays.stream(generalNames.getNames())
                          .filter(gn -> gn.getTagNo() == GeneralName.dNSName
                                     || gn.getTagNo() == GeneralName.iPAddress)
                          .map(gn -> gn.getName().toString())
                          .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.warn("Could not extract SANs: {}", e.getMessage());
            return "";
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private byte[] encodeToPem(X509Certificate cert) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(cert);
        }
        return sw.toString().getBytes();
    }

    // ─────────────────────────── Trust-All Manager ────────────────────────

    private static class TrustAllX509TrustManager implements javax.net.ssl.X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
}

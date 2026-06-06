package com.incert.certmanager.config.security;

import com.incert.certmanager.config.KeycloakJwtConverter;
import com.incert.certmanager.enumeration.RoleEnum;
import com.incert.certmanager.enumeration.PermissionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakJwtConverter}.
 */
class KeycloakJwtConverterTest {

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    void convert_shouldMapRolesAndPermissionsCorrectly() {
        // Given
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("preferred_username", "john_doe")
            .claim("realm_access", Map.of("roles", List.of("admin", "user")))
            .claim("permissions", List.of("READ", "WRITE"))
            .subject("user-id-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        // When
        AbstractAuthenticationToken token = converter.convert(jwt);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("john_doe");

        List<String> authorities = token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        assertThat(authorities).containsExactlyInAnyOrder(
            RoleEnum.ROLE_ADMIN.name(),
            RoleEnum.ROLE_USER.name(),
            PermissionEnum.READ.name(),
            PermissionEnum.WRITE.name()
        );
    }

    @Test
    void convert_withNoRolesOrPermissions_shouldReturnEmptyAuthorities() {
        // Given
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("preferred_username", "anonymous")
            .subject("anonymous-id")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        // When
        AbstractAuthenticationToken token = converter.convert(jwt);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("anonymous");
        assertThat(token.getAuthorities()).isEmpty();
    }
}

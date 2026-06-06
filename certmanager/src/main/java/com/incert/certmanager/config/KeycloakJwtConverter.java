package com.incert.certmanager.config;

import com.incert.certmanager.enumeration.RoleEnum;
import com.incert.certmanager.enumeration.PermissionEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Custom JWT converter to map Keycloak realm roles and permission claims into Spring Security authorities.
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES        = "roles";
    private static final String PERMISSIONS  = "permissions";
    private static final String USERNAME_CLAIM = "preferred_username";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Extract Realm Roles
        if (jwt.hasClaim(REALM_ACCESS)) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
            if (realmAccess != null && realmAccess.get(ROLES) instanceof List<?> roles) {
                roles.stream()
                     .filter(Objects::nonNull)
                     .map(Object::toString)
                     .map(RoleEnum::fromString)
                     .filter(Objects::nonNull)
                     .map(RoleEnum::name)
                     .map(SimpleGrantedAuthority::new)
                     .forEach(authorities::add);
            }
        }

        // 2. Extract Permissions
        if (jwt.hasClaim(PERMISSIONS)) {
            List<?> permissions = jwt.getClaim(PERMISSIONS);
            if (permissions != null) {
                permissions.stream()
                           .filter(Objects::nonNull)
                           .map(Object::toString)
                           .map(PermissionEnum::fromString)
                           .filter(Objects::nonNull)
                           .map(PermissionEnum::name)
                           .map(SimpleGrantedAuthority::new)
                           .forEach(authorities::add);
            }
        }

        String principalClaimName = jwt.hasClaim(USERNAME_CLAIM) ? USERNAME_CLAIM : "sub";
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString(principalClaimName));
    }
}

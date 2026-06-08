package com.incert.certmanager.config;

import com.incert.certmanager.enumeration.GroupEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
public class JwtClaimsExtractor {

    private static final String GROUPS_CLAIM      = "groups";
    private static final String PREFERRED_USERNAME = "preferred_username";


    public GroupEnum getCurrentUserPrimaryGroup() {
        List<String> groups = getCurrentUserGroups();
        if (groups.isEmpty()) return null;

        for (String g : groups) {
            GroupEnum group = GroupEnum.fromPath(g);
            if (group != null && group != GroupEnum.ADMINS) {
                return group;
            }
        }
        return GroupEnum.fromPath(groups.getFirst());
    }

    public List<String> getCurrentUserGroups() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) return Collections.emptyList();
        return jwt.getClaimAsStringList(GROUPS_CLAIM);
    }

    public String getCurrentUsername() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "anonymous";
        }
        String username = jwt.getClaimAsString(PREFERRED_USERNAME);
        return username != null ? username : jwt.getSubject();
    }

    private Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}

package com.library.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Reads roles from Keycloak's "realm_access.roles" claim. */
public class KeycloakRolesStrategy implements JwtRolesStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        Object roles = realmAccess.get("roles");
        return roles instanceof Collection<?> c ? (Collection<String>) c : List.of();
    }
}

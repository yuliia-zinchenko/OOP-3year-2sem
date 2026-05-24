package com.library.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/** Reads roles from a custom Auth0 namespaced claim, e.g. "https://library.app/roles". */
public class Auth0RolesStrategy implements JwtRolesStrategy {

    private final String claim;

    public Auth0RolesStrategy(String claim) {
        this.claim = claim;
    }

    @Override
    public Collection<String> extractRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(claim);
        return roles == null ? List.of() : roles;
    }
}

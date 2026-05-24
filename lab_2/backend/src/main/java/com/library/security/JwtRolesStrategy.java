package com.library.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/** Strategy: extract granted role names from a provider-specific JWT layout. */
public interface JwtRolesStrategy {
    Collection<String> extractRoles(Jwt jwt);
}

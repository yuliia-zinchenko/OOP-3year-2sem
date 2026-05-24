package com.library.security;

import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * GoF: Strategy. Pluggable JWT verifier (Keycloak / Auth0 / mock).
 */
public interface AuthStrategy {
    DecodedJWT verify(String token);
}

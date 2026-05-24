package com.library.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

/**
 * Verifies JWTs (RS256) issued by Keycloak or Auth0 using a remote JWKS endpoint.
 */
public class JwksAuthStrategy implements AuthStrategy {

    private final JwkProvider jwkProvider;
    private final String issuer;

    public JwksAuthStrategy(String jwksUrl, String issuer) {
        try {
            this.jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build();
            this.issuer = issuer;
        } catch (Exception e) {
            throw new IllegalStateException("Bad JWKS url: " + jwksUrl, e);
        }
    }

    @Override
    public DecodedJWT verify(String token) {
        try {
            DecodedJWT unverified = JWT.decode(token);
            Jwk jwk = jwkProvider.get(unverified.getKeyId());
            Algorithm alg = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier verifier = JWT.require(alg).withIssuer(issuer).build();
            return verifier.verify(token);
        } catch (Exception e) {
            throw new SecurityException("Invalid JWT: " + e.getMessage(), e);
        }
    }
}

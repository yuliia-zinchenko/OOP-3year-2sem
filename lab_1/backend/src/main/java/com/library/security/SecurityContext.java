package com.library.security;

import java.io.InputStream;
import java.util.Properties;

public final class SecurityContext {
    private static volatile AuthStrategy strategy;

    public static AuthStrategy strategy() {
        AuthStrategy s = strategy;
        if (s == null) {
            synchronized (SecurityContext.class) {
                if (strategy == null) {
                    Properties p = new Properties();
                    try (InputStream in = SecurityContext.class.getClassLoader()
                            .getResourceAsStream("db.properties")) {
                        if (in != null) p.load(in);
                    } catch (Exception ignore) {}
                    String jwks = env("JWT_JWKS_URL", p.getProperty("jwt.jwksUrl"));
                    String issuer = env("JWT_ISSUER", p.getProperty("jwt.issuer"));
                    strategy = new JwksAuthStrategy(jwks, issuer);
                }
                s = strategy;
            }
        }
        return s;
    }

    private static String env(String k, String fb) {
        String v = System.getenv(k);
        return v != null && !v.isBlank() ? v : fb;
    }
}
